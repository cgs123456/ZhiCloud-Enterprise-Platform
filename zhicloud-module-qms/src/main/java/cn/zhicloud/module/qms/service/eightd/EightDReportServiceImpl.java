package cn.zhicloud.module.qms.service.eightd;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.eightd.vo.EightDReportPageReqVO;
import cn.zhicloud.module.qms.controller.admin.eightd.vo.EightDReportSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.eightd.EightDReportDO;
import cn.zhicloud.module.qms.dal.mysql.eightd.EightDReportMapper;
import cn.zhicloud.module.qms.enums.qms.EightDStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
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
        if (eightDReport.getStatus() == null) {
            eightDReport.setStatus(EightDStatusEnum.DRAFT.getStatus());
        }
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
        // 2. 流转到 D8 关闭，并记录关闭时间
        EightDReportDO updateObj = new EightDReportDO();
        updateObj.setId(id);
        updateObj.setStatus(EightDStatusEnum.D8_CLOSED.getStatus());
        updateObj.setCloseTime(LocalDateTime.now());
        eightDReportMapper.updateById(updateObj);
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