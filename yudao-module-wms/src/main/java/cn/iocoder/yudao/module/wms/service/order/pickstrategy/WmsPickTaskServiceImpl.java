package cn.iocoder.yudao.module.wms.service.order.pickstrategy;

import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.wms.controller.admin.order.pickstrategy.vo.WmsPickTaskPageReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.pickstrategy.WmsPickTaskDO;
import cn.iocoder.yudao.module.wms.dal.mysql.order.pickstrategy.WmsPickTaskMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.PICK_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.PICK_TASK_NOT_YOURS;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.PICK_TASK_STATUS_NOT_PICKABLE;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.WMS_PICK_QUANTITY_NEGATIVE;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.WMS_PICK_QUANTITY_EXCEEDS;

/**
 * WMS 拣货任务 Service 实现类
 *
 * @author 芋道源码
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
