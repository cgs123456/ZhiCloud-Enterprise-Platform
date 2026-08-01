package cn.iocoder.yudao.module.wms.service.order.wave;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.wms.controller.admin.order.wave.vo.order.WmsWaveOrderPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.order.wave.vo.order.WmsWaveOrderSaveReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.wave.WmsWaveOrderDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.wave.WmsWaveOrderDetailDO;

import java.util.List;

/**
 * WMS 波次单 Service 接口
 *
 * <p>波次单把多个出库单按仓库/客户/商品/承运商策略合并为一个拣货批次，
 * 用于批量拣货、批量复核、批量出库。
 *
 * @author 芋道源码
 */
public interface WmsWaveOrderService {

    /**
     * 生成波次单：从所选出库单展开明细
     *
     * @param createReqVO 创建请求（含出库单 ID 列表）
     * @return 波次单 ID
     */
    Long createWaveOrder(WmsWaveOrderSaveReqVO createReqVO);

    /**
     * 更新波次单（仅草稿状态可改）
     */
    void updateWaveOrder(WmsWaveOrderSaveReqVO updateReqVO);

    /**
     * 删除波次单（仅草稿/已作废可删）
     */
    void deleteWaveOrder(Long id);

    /**
     * 获取波次单
     */
    WmsWaveOrderDO getWaveOrder(Long id);

    /**
     * 分页查询波次单
     */
    PageResult<WmsWaveOrderDO> getWaveOrderPage(WmsWaveOrderPageReqVO pageReqVO);

    /**
     * 获取波次单明细列表
     */
    List<WmsWaveOrderDetailDO> getWaveOrderDetailListByWaveOrderId(Long waveOrderId);

}
