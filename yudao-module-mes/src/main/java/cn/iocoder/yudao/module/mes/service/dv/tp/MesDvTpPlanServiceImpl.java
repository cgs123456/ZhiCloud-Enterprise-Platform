package cn.iocoder.yudao.module.mes.service.dv.tp;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.dv.tp.vo.MesDvTpExecuteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.tp.vo.MesDvTpPlanPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.tp.vo.MesDvTpPlanSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.tp.MesDvTpPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.tp.MesDvTpPlanItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.tp.MesDvTpRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.tp.MesDvTpPlanItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.tp.MesDvTpPlanMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.tp.MesDvTpRecordMapper;
import cn.iocoder.yudao.module.mes.enums.dv.tp.MesDvTpCycleTypeEnum;
import cn.iocoder.yudao.module.mes.enums.dv.tp.MesDvTpPlanStatusEnum;
import cn.iocoder.yudao.module.mes.enums.dv.tp.MesDvTpRecordResultEnum;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.DV_TP_PLAN_NO_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.DV_TP_PLAN_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.DV_TP_PLAN_STATUS_INVALID;

/**
 * MES TPM 计划 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class MesDvTpPlanServiceImpl implements MesDvTpPlanService {

    @Resource
    private MesDvTpPlanMapper tpPlanMapper;
    @Resource
    private MesDvTpPlanItemMapper tpPlanItemMapper;
    @Resource
    private MesDvTpRecordMapper tpRecordMapper;
    @Resource
    private MesDvMachineryService machineryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTpPlan(MesDvTpPlanSaveReqVO createReqVO) {
        // 校验编号唯一
        validatePlanNoUnique(null, createReqVO.getPlanNo());
        // 校验设备存在
        machineryService.validateMachineryExists(createReqVO.getEquipmentId());
        // 插入
        MesDvTpPlanDO plan = BeanUtils.toBean(createReqVO, MesDvTpPlanDO.class);
        plan.setStatus(MesDvTpPlanStatusEnum.ENABLED.getStatus());
        tpPlanMapper.insert(plan);
        return plan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTpPlan(MesDvTpPlanSaveReqVO updateReqVO) {
        MesDvTpPlanDO existPlan = validateTpPlan(updateReqVO.getId());
        // 只有禁用状态才能修改
        if (!ObjUtil.equal(existPlan.getStatus(), MesDvTpPlanStatusEnum.DISABLED.getStatus())) {
            throw exception(DV_TP_PLAN_STATUS_INVALID);
        }
        validatePlanNoUnique(updateReqVO.getId(), updateReqVO.getPlanNo());
        machineryService.validateMachineryExists(updateReqVO.getEquipmentId());
        MesDvTpPlanDO updateObj = BeanUtils.toBean(updateReqVO, MesDvTpPlanDO.class);
        tpPlanMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTpPlan(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            MesDvTpPlanDO plan = validateTpPlan(id);
            // 只有禁用状态才能删除
            if (!ObjUtil.equal(plan.getStatus(), MesDvTpPlanStatusEnum.DISABLED.getStatus())) {
                throw exception(DV_TP_PLAN_STATUS_INVALID);
            }
            // 删除计划项目
            tpPlanItemMapper.deleteByPlanId(id);
            tpPlanMapper.deleteById(id);
        }
    }

    @Override
    public MesDvTpPlanDO getTpPlan(Long id) {
        return tpPlanMapper.selectById(id);
    }

    @Override
    public PageResult<MesDvTpPlanDO> getTpPlanPage(MesDvTpPlanPageReqVO pageReqVO) {
        return tpPlanMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enablePlan(Long id) {
        MesDvTpPlanDO plan = validateTpPlan(id);
        if (!ObjUtil.equal(plan.getStatus(), MesDvTpPlanStatusEnum.DISABLED.getStatus())) {
            throw exception(DV_TP_PLAN_STATUS_INVALID);
        }
        tpPlanMapper.updateById(new MesDvTpPlanDO().setId(id)
                .setStatus(MesDvTpPlanStatusEnum.ENABLED.getStatus()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disablePlan(Long id) {
        MesDvTpPlanDO plan = validateTpPlan(id);
        if (!ObjUtil.equal(plan.getStatus(), MesDvTpPlanStatusEnum.ENABLED.getStatus())) {
            throw exception(DV_TP_PLAN_STATUS_INVALID);
        }
        tpPlanMapper.updateById(new MesDvTpPlanDO().setId(id)
                .setStatus(MesDvTpPlanStatusEnum.DISABLED.getStatus()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long executePlan(MesDvTpExecuteReqVO reqVO, Long executorId) {
        // 1. 校验计划存在 + 启用状态
        MesDvTpPlanDO plan = validateTpPlan(reqVO.getPlanId());
        if (!ObjUtil.equal(plan.getStatus(), MesDvTpPlanStatusEnum.ENABLED.getStatus())) {
            throw exception(DV_TP_PLAN_STATUS_INVALID);
        }
        // 2. 创建执行记录
        MesDvTpRecordDO record = MesDvTpRecordDO.builder()
                .planId(reqVO.getPlanId())
                .equipmentId(plan.getEquipmentId())
                .executeDate(reqVO.getExecuteDate())
                .executorId(executorId)
                .result(reqVO.getResult() != null ? reqVO.getResult() : MesDvTpRecordResultEnum.QUALIFIED.getResult())
                .issuesFound(reqVO.getIssuesFound())
                .actionTaken(reqVO.getActionTaken())
                .remark(reqVO.getRemark())
                .build();
        tpRecordMapper.insert(record);
        // 3. 更新下次执行日期（基于周期类型 + 周期值）
        LocalDate nextDate = calculateNextExecuteDate(reqVO.getExecuteDate(), plan.getCycleType(), plan.getCycleValue());
        tpPlanMapper.updateById(new MesDvTpPlanDO().setId(reqVO.getPlanId()).setNextExecuteDate(nextDate));
        log.info("[executePlan][planId({}) executorId({}) executeDate({}) nextDate({})]",
                reqVO.getPlanId(), executorId, reqVO.getExecuteDate(), nextDate);
        return record.getId();
    }

    @Override
    public List<MesDvTpPlanDO> getOverduePlans() {
        return tpPlanMapper.selectOverdueList(LocalDate.now());
    }

    @Override
    public MesDvTpPlanDO validateTpPlan(Long id) {
        MesDvTpPlanDO plan = tpPlanMapper.selectById(id);
        if (plan == null) {
            throw exception(DV_TP_PLAN_NOT_EXISTS);
        }
        return plan;
    }

    @Override
    public List<MesDvTpPlanItemDO> getTpPlanItemListByPlanId(Long planId) {
        return tpPlanItemMapper.selectListByPlanId(planId);
    }

    @Override
    public List<MesDvTpRecordDO> getTpRecordListByPlanId(Long planId) {
        return tpRecordMapper.selectListByPlanId(planId);
    }

    // ==================== 校验方法 ====================

    private void validatePlanNoUnique(Long id, String planNo) {
        if (planNo == null) {
            return;
        }
        MesDvTpPlanDO plan = tpPlanMapper.selectByPlanNo(planNo);
        if (plan == null) {
            return;
        }
        if (id == null || !ObjUtil.equal(plan.getId(), id)) {
            throw exception(DV_TP_PLAN_NO_DUPLICATE, planNo);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 根据周期类型和周期值计算下次执行日期
     *
     * @param baseDate 基准日期
     * @param cycleType 周期类型
     * @param cycleValue 周期值
     * @return 下次执行日期
     */
    private LocalDate calculateNextExecuteDate(LocalDate baseDate, Integer cycleType, Integer cycleValue) {
        if (baseDate == null) {
            baseDate = LocalDate.now();
        }
        int value = (cycleValue != null && cycleValue > 0) ? cycleValue : 1;
        if (cycleType == null) {
            return baseDate.plusMonths(value);
        }
        if (ObjUtil.equal(cycleType, MesDvTpCycleTypeEnum.DAY.getCycleType())) {
            return baseDate.plusDays(value);
        }
        if (ObjUtil.equal(cycleType, MesDvTpCycleTypeEnum.WEEK.getCycleType())) {
            return baseDate.plusWeeks(value);
        }
        if (ObjUtil.equal(cycleType, MesDvTpCycleTypeEnum.MONTH.getCycleType())) {
            return baseDate.plusMonths(value);
        }
        if (ObjUtil.equal(cycleType, MesDvTpCycleTypeEnum.QUARTER.getCycleType())) {
            return baseDate.plusMonths(value * 3L);
        }
        if (ObjUtil.equal(cycleType, MesDvTpCycleTypeEnum.YEAR.getCycleType())) {
            return baseDate.plusYears(value);
        }
        return baseDate.plusMonths(value);
    }

}