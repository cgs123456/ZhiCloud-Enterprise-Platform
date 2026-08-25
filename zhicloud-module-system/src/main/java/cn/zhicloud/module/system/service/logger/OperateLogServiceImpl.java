package cn.zhicloud.module.system.service.logger;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.common.biz.system.logger.dto.OperateLogCreateReqDTO;
import cn.zhicloud.module.system.api.logger.dto.OperateLogPageReqDTO;
import cn.zhicloud.module.system.controller.admin.logger.vo.operatelog.OperateLogHashVerifyRespVO;
import cn.zhicloud.module.system.controller.admin.logger.vo.operatelog.OperateLogPageReqVO;
import cn.zhicloud.module.system.dal.dataobject.logger.OperateLogDO;
import cn.zhicloud.module.system.dal.mysql.logger.OperateLogMapper;
import cn.zhicloud.module.system.framework.auditlog.core.service.AuditLogHashChainService;
import cn.zhicloud.module.system.framework.auditlog.core.service.AuditLogPersistService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
@Slf4j
public class OperateLogServiceImpl implements OperateLogService {

    /**
     * 默认 Hash 链验证的最大日志条数（避免一次性加载过多）
     */
    private static final int DEFAULT_VERIFY_LIMIT = 1000;

    @Resource
    private OperateLogMapper operateLogMapper;

    @Resource
    private AuditLogHashChainService auditLogHashChainService;

    @Resource
    private AuditLogPersistService auditLogPersistService;

    @Override
    public void createOperateLog(OperateLogCreateReqDTO createReqDTO) {
        OperateLogDO operateLog = BeanUtils.toBean(createReqDTO, OperateLogDO.class);

        // 1. Hash 链式审计：插入前获取上一条日志的 current_hash 作为 prev_hash，并计算当前日志的 current_hash
        if (auditLogHashChainService.isEnabled()) {
            try {
                String prevHash = operateLogMapper.selectLatestCurrentHash();
                auditLogHashChainService.fillHash(operateLog, prevHash == null ? "" : prevHash);
            } catch (Throwable ex) {
                // 失败降级：仅打印日志，不阻断主流程
                log.error("[createOperateLog][Hash 链计算失败，跳过 hash 填充]", ex);
            }
        }

        // 2. 插入 MySQL
        operateLogMapper.insert(operateLog);

        // 3. 独立文件存储：失败不影响主流程
        try {
            if (auditLogPersistService.isEnabled()) {
                auditLogPersistService.persist(operateLog);
                // 写入文件级 hash 链记录（基于当天日期）
                // 注意：此处的 file_hash 是基于当天日志文件全量内容计算的，重复调用会产生多条 file_hash 记录。
                // 为简化最小改动，每次写日志后都追加一条 file_hash 记录，验证时只需对比最后一条即可。
                auditLogPersistService.appendFileHash(LocalDate.now());
            }
        } catch (Throwable ex) {
            log.error("[createOperateLog][独立文件存储失败 logId={}]", operateLog.getId(), ex);
        }
    }

    @Override
    public OperateLogDO getOperateLog(Long id) {
        return operateLogMapper.selectById(id);
    }

    @Override
    public PageResult<OperateLogDO> getOperateLogPage(OperateLogPageReqVO pageReqVO) {
        return operateLogMapper.selectPage(pageReqVO);
    }

    @Override
    public PageResult<OperateLogDO> getOperateLogPage(OperateLogPageReqDTO pageReqDTO) {
        return operateLogMapper.selectPage(pageReqDTO);
    }

    @Override
    public OperateLogHashVerifyRespVO verifyHashChain(Long startId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = DEFAULT_VERIFY_LIMIT;
        }
        if (startId == null) {
            startId = 0L;
        }

        OperateLogHashVerifyRespVO resp = new OperateLogHashVerifyRespVO();
        resp.setStartId(startId);
        List<Long> tamperedIds = new ArrayList<>();

        // Hash 链未启用，直接返回有效
        if (!auditLogHashChainService.isEnabled()) {
            resp.setValid(true);
            resp.setTotalCount(0);
            resp.setTamperedIds(tamperedIds);
            resp.setMessage("Hash 链式审计未启用，跳过验证");
            return resp;
        }

        List<OperateLogDO> logs = operateLogMapper.selectListForHashVerify(startId, limit);
        resp.setTotalCount(logs == null ? 0 : logs.size());

        if (logs == null || logs.isEmpty()) {
            resp.setValid(true);
            resp.setTamperedIds(tamperedIds);
            resp.setMessage("未找到符合条件的日志记录，跳过验证");
            return resp;
        }

        // 验证每一条日志：
        // 1. 单条日志的 current_hash 与重新计算的 hash 一致
        // 2. 相邻日志的 prev_hash 等于前一条的 current_hash（链式连续性）
        String expectedPrevHash = null;
        boolean chainBroken = false;
        for (OperateLogDO logDO : logs) {
            boolean currentValid = true;
            // (1) 单条日志 hash 一致性
            if (!auditLogHashChainService.verifyHash(logDO)) {
                currentValid = false;
            }
            // (2) 链式连续性：当前日志的 prev_hash 应等于前一条日志的 current_hash
            // 第一条日志的 prev_hash 应为 ""，否则视为链断
            if (expectedPrevHash == null) {
                // 链上的第一条日志，prev_hash 应为空字符串
                String firstPrev = logDO.getPrevHash();
                if (firstPrev != null && !firstPrev.isEmpty()) {
                    currentValid = false;
                }
            } else {
                String actualPrev = logDO.getPrevHash() == null ? "" : logDO.getPrevHash();
                if (!expectedPrevHash.equals(actualPrev)) {
                    currentValid = false;
                }
            }
            if (!currentValid) {
                tamperedIds.add(logDO.getId());
                chainBroken = true;
            }
            // 期望的下一条日志的 prev_hash = 当前日志的 current_hash
            expectedPrevHash = logDO.getCurrentHash() == null ? "" : logDO.getCurrentHash();
        }

        resp.setValid(!chainBroken);
        resp.setTamperedIds(tamperedIds);
        if (chainBroken) {
            resp.setMessage(String.format("检测到 %d 条日志被篡改或链断裂", tamperedIds.size()));
        } else {
            resp.setMessage(String.format("Hash 链完整性验证通过，共验证 %d 条日志", resp.getTotalCount()));
        }
        return resp;
    }

}
