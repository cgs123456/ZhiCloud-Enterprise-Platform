package cn.zhicloud.module.wms.service.order.pickstrategy;

import cn.hutool.core.util.ObjectUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.security.core.util.SecurityFrameworkUtils;
import cn.zhicloud.module.wms.controller.admin.order.pickstrategy.vo.WmsPickTaskPageReqVO;
import cn.zhicloud.module.wms.dal.dataobject.order.pickstrategy.WmsPickTaskDO;
import cn.zhicloud.module.wms.dal.mysql.order.pickstrategy.WmsPickTaskMapper;
import cn.zhicloud.module.wms.service.md.location.WmsLocationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.PICK_TASK_NOT_EXISTS;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.PICK_TASK_NOT_YOURS;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.PICK_TASK_STATUS_NOT_PICKABLE;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.WMS_PICK_QUANTITY_NEGATIVE;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.WMS_PICK_QUANTITY_EXCEEDS;

/**
 * WMS 拣货任务 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class WmsPickTaskServiceImpl implements WmsPickTaskService {

    /**
     * 拣货任务状态：待拣
     */
    private static final int STATUS_PENDING = 10;
    /**
     * 拣货任务状态：已拣
     */
    private static final int STATUS_PICKED = 20;

    @Resource
    private WmsPickTaskMapper pickTaskMapper;
    @Resource
    private WmsLocationService locationService; // V205：确认拣货库位门禁（ADR 259 D7）


    @Override
    public WmsPickTaskDO getPickTask(Long id) {
        return pickTaskMapper.selectById(id);
    }

    @Override
    public PageResult<WmsPickTaskDO> getPickTaskPage(WmsPickTaskPageReqVO pageReqVO) {
        return pickTaskMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPick(Long taskId, BigDecimal pickedQuantity) {
        // 1. 校验存在
        WmsPickTaskDO task = pickTaskMapper.selectById(taskId);
        if (task == null) {
            throw exception(PICK_TASK_NOT_EXISTS);
        }
        // 2. IDOR 防护：任务必须归属当前登录用户（21 CFR Part 11 防抵赖要求）
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null || !loginUserId.equals(task.getPickerUserId())) {
            throw exception(PICK_TASK_NOT_YOURS);
        }
        // 3. 校验状态可拣（待拣或已拣）
        if (ObjectUtil.notEqual(task.getStatus(), STATUS_PENDING)
                && ObjectUtil.notEqual(task.getStatus(), STATUS_PICKED)) {
            throw exception(PICK_TASK_STATUS_NOT_PICKABLE);
        }
        // 3.5 V205 库位门禁（ADR 259 D7 拣货真值化）：任务绑定真库位时，确认前校验可用——
        //     锁定/禁用库位的任务拒绝确认（与入库/上架同一冻结语义）
        if (task.getLocationId() != null) {
            locationService.validateLocationUsable(task.getLocationId());
        }
        // 4. 数量非负 + 不超上限（防止数量污染）
        if (pickedQuantity == null || pickedQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw exception(WMS_PICK_QUANTITY_NEGATIVE);
        }
        if (task.getQuantity() != null && pickedQuantity.compareTo(task.getQuantity()) > 0) {
            throw exception(WMS_PICK_QUANTITY_EXCEEDS);
        }
        // 5. 更新已拣数量与状态
        WmsPickTaskDO updateObj = new WmsPickTaskDO();
        updateObj.setId(taskId);
        updateObj.setPickedQuantity(pickedQuantity);
        updateObj.setStatus(STATUS_PICKED);
        updateObj.setPickTime(LocalDateTime.now());
        pickTaskMapper.updateById(updateObj);
    }

    @Override
    public List<WmsPickTaskDO> getMyPickTasks(Long pickerUserId) {
        if (pickerUserId == null) {
            return List.of();
        }
        return pickTaskMapper.selectListByPickerUserId(pickerUserId);
    }

    @Override
    public List<WmsPickTaskDO> getPickTasksByShipmentOrderId(Long shipmentOrderId) {
        if (shipmentOrderId == null) {
            return List.of();
        }
        return pickTaskMapper.selectListByShipmentOrderId(shipmentOrderId);
    }

}
