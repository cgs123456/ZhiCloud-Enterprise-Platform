package cn.zhicloud.module.wms.service.md.sn;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.wms.controller.admin.md.sn.vo.WmsSnGenerateReqVO;
import cn.zhicloud.module.wms.controller.admin.md.sn.vo.WmsSnPageReqVO;
import cn.zhicloud.module.wms.controller.admin.md.sn.vo.WmsSnSaveReqVO;
import cn.zhicloud.module.wms.controller.admin.md.sn.vo.WmsSnTraceRespVO;
import cn.zhicloud.module.wms.dal.dataobject.md.sn.WmsSnDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

/**
 * WMS 序列号 Service 接口
 *
 * @author 智云
 */
public interface WmsSnService {

    /**
     * 批量生成序列号（按规则：前缀 + 日期 + 流水号）
     */
    List<WmsSnDO> generateSnList(@Valid WmsSnGenerateReqVO reqVO);

    /**
     * 创建序列号（手工录入）
     */
    Long createSn(@Valid WmsSnSaveReqVO createReqVO);

    /**
     * 更新序列号
     */
    void updateSn(@Valid WmsSnSaveReqVO updateReqVO);

    /**
     * 删除序列号（仅允许删除「已生成」状态的序列号）
     */
    void deleteSn(Long id);

    /**
     * 校验序列号存在
     */
    WmsSnDO validateSnExists(Long id);

    /**
     * 获得序列号
     */
    WmsSnDO getSn(Long id);

    /**
     * 按序列号字符串获取
     */
    WmsSnDO getSnBySn(String sn);

    /**
     * 获得序列号分页
     */
    PageResult<WmsSnDO> getSnPage(WmsSnPageReqVO pageReqVO);

    /**
     * 按编号集合获取序列号列表
     */
    List<WmsSnDO> getSnList(Collection<Long> ids);

    /**
     * 序列号入库绑定（关联库存批次，状态 BOUND -> IN_STOCK）
     */
    void bindInventory(Long snId, Long inventoryId, Long batchId, Long warehouseId, Long zoneId, Long locationId, Long inboundOrderId);

    /**
     * 序列号出库解绑（状态 IN_STOCK -> SHIPPED）
     */
    void unbindAndShip(Long snId, Long outboundOrderId);

    /**
     * 序列号退货（状态 SHIPPED -> RETURNED -> 重新入库后 IN_STOCK）
     */
    void returnSn(Long snId, Long warehouseId, Long locationId);

    /**
     * 序列号追溯（正反向：SN -> 入库单 -> 生产工单 / SN -> 出库单 -> 客户）
     */
    WmsSnTraceRespVO trace(Long snId);

}