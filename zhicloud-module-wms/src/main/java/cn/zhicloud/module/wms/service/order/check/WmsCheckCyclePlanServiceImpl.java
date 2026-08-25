package cn.zhicloud.module.wms.service.order.check;

import cn.hutool.core.util.ObjectUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.wms.controller.admin.order.check.vo.cycle.WmsCheckCyclePlanPageReqVO;
import cn.zhicloud.module.wms.controller.admin.order.check.vo.cycle.WmsCheckCyclePlanSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.order.check.WmsCheckCyclePlanDO;
import cn.zhicloud.module.wms.dal.mysql.order.check.WmsCheckCyclePlanMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.CHECK_CYCLE_PLAN_DUPLICATE;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.CHECK_CYCLE_PLAN_ABC_INVALID;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.CHECK_CYCLE_PLAN_NOT_EXISTS;

/**
 * WMS 循环盘点计划 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class WmsCheckCyclePlanServiceImpl implements WmsCheckCyclePlanService {

    private static final Set<String> VALID_ABC = Set.of("A", "B", "C");

    @Resource
    private WmsCheckCyclePlanMapper checkCyclePlanMapper;

    @Override
    public Long createCheckCyclePlan(WmsCheckCyclePlanSaveReqVO createReqVO) {
        validateAbc(createReqVO.getAbcClassification());
        validateUnique(null, createReqVO.getWarehouseId(), createReqVO.getAbcClassification());
        WmsCheckCyclePlanDO plan = BeanUtils.toBean(createReqVO, WmsCheckCyclePlanDO.class);
        if (plan.getEnabled() == null) {
            plan.setEnabled(1);
        }
        if (plan.getNextCheckDate() == null && plan.getCycleDays() != null) {
            plan.setNextCheckDate(LocalDate.now().plusDays(plan.getCycleDays()));
        }
        checkCyclePlanMapper.insert(plan);
        return plan.getId();
    }

    @Override
    public void updateCheckCyclePlan(WmsCheckCyclePlanSaveReqVO updateReqVO) {
        validateCheckCyclePlanExists(updateReqVO.getId());
        validateAbc(updateReqVO.getAbcClassification());
        validateUnique(updateReqVO.getId(), updateReqVO.getWarehouseId(), updateReqVO.getAbcClassification());
        WmsCheckCyclePlanDO updateObj = BeanUtils.toBean(updateReqVO, WmsCheckCyclePlanDO.class);
        checkCyclePlanMapper.updateById(updateObj);
    }

    @Override
    public void deleteCheckCyclePlan(Long id) {
        validateCheckCyclePlanExists(id);
        checkCyclePlanMapper.deleteById(id);
    }

    @Override
    public WmsCheckCyclePlanDO getCheckCyclePlan(Long id) {
        return checkCyclePlanMapper.selectById(id);
    }

    @Override
    public PageResult<WmsCheckCyclePlanDO> getCheckCyclePlanPage(WmsCheckCyclePlanPageReqVO pageReqVO) {
        return checkCyclePlanMapper.selectPage(pageReqVO);
    }

    @Override
    public List<WmsCheckCyclePlanDO> getDueCheckCyclePlanList(LocalDate today) {
        return checkCyclePlanMapper.selectListDueToday(today);
    }

    @Override
    public List<WmsCheckCyclePlanDO> getEnabledCheckCyclePlanList() {
        return checkCyclePlanMapper.selectListAllEnabled();
    }

    @Override
    public void updateNextCheckDate(Long id, LocalDate nextCheckDate) {
        WmsCheckCyclePlanDO updateObj = new WmsCheckCyclePlanDO();
        updateObj.setId(id);
        updateObj.setNextCheckDate(nextCheckDate);
        checkCyclePlanMapper.updateById(updateObj);
    }

    @Override
    public WmsCheckCyclePlanDO validateCheckCyclePlanExists(Long id) {
        WmsCheckCyclePlanDO plan = checkCyclePlanMapper.selectById(id);
        if (plan == null) {
            throw exception(CHECK_CYCLE_PLAN_NOT_EXISTS);
        }
        return plan;
    }

    private void validateAbc(String abc) {
        if (abc == null || !VALID_ABC.contains(abc.toUpperCase())) {
            throw exception(CHECK_CYCLE_PLAN_ABC_INVALID);
        }
    }

    private void validateUnique(Long id, Long warehouseId, String abc) {
        WmsCheckCyclePlanDO existing = checkCyclePlanMapper.selectByWarehouseAndAbc(warehouseId, abc);
        if (existing == null) {
            return;
        }
        if (id == null || ObjectUtil.notEqual(existing.getId(), id)) {
            throw exception(CHECK_CYCLE_PLAN_DUPLICATE);
        }
    }

}