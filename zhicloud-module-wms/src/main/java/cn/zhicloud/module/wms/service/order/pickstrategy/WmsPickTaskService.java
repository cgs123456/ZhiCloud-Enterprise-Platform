package cn.zhicloud.module.wms.service.order.pickstrategy;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.wms.controller.admin.order.pickstrategy.vo.WmsPickTaskPageReqVO;
import cn.zhicloud.module.wms.dal.dataobject.order.pickstrategy.WmsPickTaskDO;

import java.math.BigDecimal;
import java.util.List;

/**
 * WMS 拣货任务 Service 接口
 *
 * @author 智云
 */
public interface WmsPickTaskService {

    /**
     * 获得拣货任务
     *
     * @param id 编号
     * @return 拣货任务
     */
    WmsPickTaskDO getPickTask(Long id);

    /**
     * 获得拣货任务分页
     *
     * @param pageReqVO 分页查询
     * @return 拣货任务分页
     */
    PageResult<WmsPickTaskDO> getPickTaskPage(WmsPickTaskPageReqVO pageReqVO);

    /**
     * 确认拣货
     *
     * @param taskId 拣货任务编号
     * @param pickedQuantity 已拣数量
     */
    void confirmPick(Long taskId, BigDecimal pickedQuantity);

    /**
     * 获取指定拣货员的待拣/已拣任务列表
     *
     * @param pickerUserId 拣货员用户编号
     * @return 拣货任务列表
     */
    List<WmsPickTaskDO> getMyPickTasks(Long pickerUserId);

    /**
     * 根据出库单编号获取拣货任务列表
     *
     * @param shipmentOrderId 出库单编号
     * @return 拣货任务列表
     */
    List<WmsPickTaskDO> getPickTasksByShipmentOrderId(Long shipmentOrderId);

}
