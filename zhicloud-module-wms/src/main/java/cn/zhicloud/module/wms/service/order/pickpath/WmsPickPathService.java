package cn.zhicloud.module.wms.service.order.pickpath;

import cn.zhicloud.module.wms.controller.admin.pickpath.vo.WmsPickPathRespVO;

/**
 * WMS 拣货路径优化 Service 接口
 *
 * @author 智云
 */
public interface WmsPickPathService {

    /**
     * 为出库单生成最优拣货路径
     *
     * @param shipmentOrderId 出库单编号
     * @return 拣货路径
     */
    WmsPickPathRespVO optimizePickPath(Long shipmentOrderId);

    /**
     * 为波次单生成批量拣货路径
     *
     * @param waveOrderId 波次单编号
     * @return 拣货路径
     */
    WmsPickPathRespVO optimizeBatchPickPath(Long waveOrderId);

}
