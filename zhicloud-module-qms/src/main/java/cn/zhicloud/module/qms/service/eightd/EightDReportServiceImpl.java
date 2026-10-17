package cn.zhicloud.module.qms.service.eightd;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.eightd.vo.EightDReportPageReqVO;
import cn.zhicloud.module.qms.controller.admin.eightd.vo.EightDReportSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.eightd.EightDReportDO;
import cn.zhicloud.module.qms.dal.mysql.eightd.EightDReportMapper;
import cn.zhicloud.module.qms.enums.qms.EightDStatusEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.EIGHT_D_REPORT_NOT_CLOSEABLE;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.EIGHT_D_REPORT_NOT_EXISTS;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.EIGHT_D_REPORT_STAGE_INVALID;

/**
 * QMS 8D 报告 Service 实现类
 *
 * @author zhicloud
 */
@Service
@Validated
public class EightDReportServiceImpl implements EightDReportService {

    @Resource
    private EightDReportMapper eightDReportMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEightDReport(EightDReportSaveReqVO createReqVO) {
        // 插入
        EightDReportDO eightDReport = BeanUtils.toBean(createReqVO, EightDReportDO.class);
        // 默认状态为草稿
        // 状态无条件置为初始值，忽略调用方传入的任何 status。
        // §12.235.5：此前「仅在 null 时补默认」，调用方即可在**建单**这一步直接写入终态，
        // 跳过整个流转（该类可能连流转方法都没有），产生「未审即批/未做即完成」的记录。
        // 状态只能由本模块的流转方法产生。
        eightDReport.setStatus(EightDStatusEnum.DRAFT.getStatus());
        eightDReportMapper.insert(eightDReport);
        // 返回
        return eightDReport.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEightDReport(EightDReportSaveReqVO updateReqVO) {
        // 校验存在
        validateEightDReportExists(updateReqVO.getId());
        // 更新
        EightDReportDO updateObj = BeanUtils.toBean(updateReqVO, EightDReportDO.class);
        // 禁止通过通用更新修改状态，状态变更必须走 advanceStage/closeEightDReport 等状态流转方法
        updateObj.setStatus(null);
        eightDReportMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEightDReport(Long id) {
        // 校验存在
        validateEightDReportExists(id);
        // 删除
        eightDReportMapper.deleteById(id);
    }

    private void validateEightDReportExists(Long id) {
        if (eightDReportMapper.selectById(id) == null) {
            throw exception(EIGHT_D_REPORT_NOT_EXISTS);
        }
    }

    @Override
    public EightDReportDO getEightDReport(Long id) {
        return eightDReportMapper.selectById(id);
    }

    @Override
    public PageResult<EightDReportDO> getEightDReportPage(EightDReportPageReqVO pageReqVO) {
        return eightDReportMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void advanceStage(Long id) {
        // 1. 校验存在
        EightDReportDO report = eightDReportMapper.selectById(id);
        if (report == null) {
            throw exception(EIGHT_D_REPORT_NOT_EXISTS);
        }
        // 2. 校验状态：未关闭才允许推进
        Integer currentStatus = report.getStatus();
        if (EightDStatusEnum.D8_CLOSED.getStatus().equals(currentStatus)) {
            throw exception(EIGHT_D_REPORT_STAGE_INVALID);
        }
        // 3. 流转到下一阶段
        Integer nextStatus = nextStage(currentStatus);
        EightDReportDO updateObj = new EightDReportDO();
        updateObj.setId(id);
        updateObj.setStatus(nextStatus);
        eightDReportMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeEightDReport(Long id) {
        // 1. 校验存在
        EightDReportDO report = eightDReportMapper.selectById(id);
        if (report == null) {
            throw exception(EIGHT_D_REPORT_NOT_EXISTS);
        }
        // 2. §12.253 前置态校验：只有 D7（预防）阶段可关闭。
        // 更早阶段关闭 = 跳过未完成阶段的流转（D1~D7 内容不全即终结）；
        // 已关闭重复关 = closeTime 被重写（关闭证据链被变更）。
        // 「只有 D7 可关」对 D8 同样成立：提前关与重复关共用同一拒绝理由（循 CAPA 单码先例）。
        if (!EightDStatusEnum.D7_PREVENT.getStatus().equals(report.getStatus())) {
            throw exception(EIGHT_D_REPORT_NOT_CLOSEABLE);
        }
        // 3. §12.253 CAS 条件更新：读-判-写之间状态可能被并发流转，
        // 只在仍处于读到的 D7 时落 D8 + 关闭时间；0 行即竞态失败，拒绝而非覆盖。
        if (eightDReportMapper.update(null, new LambdaUpdateWrapper<EightDReportDO>()
                .set(EightDReportDO::getStatus, EightDStatusEnum.D8_CLOSED.getStatus())
                .set(EightDReportDO::getCloseTime, LocalDateTime.now())
                .eq(EightDReportDO::getId, id)
                .eq(EightDReportDO::getStatus, EightDStatusEnum.D7_PREVENT.getStatus())) == 0) {
            throw exception(EIGHT_D_REPORT_NOT_CLOSEABLE);
        }
    }

    /**
     * 获取下一阶段状态码
     *
     * @param currentStatus 当前状态
     * @return 下一阶段状态
     */
    private Integer nextStage(Integer currentStatus) {
        if (currentStatus == null) {
            return EightDStatusEnum.D1_TEAM.getStatus();
        }
        // 按状态码递增 10 推进至 D7，D7 之后由 closeEightDReport 流转到 D8
        int next = currentStatus + 10;
        if (next > EightDStatusEnum.D7_PREVENT.getStatus()) {
            return EightDStatusEnum.D7_PREVENT.getStatus();
        }
        return next;
    }

}