package cn.iocoder.yudao.module.erp.service.bi.sale;

import cn.iocoder.yudao.module.erp.controller.admin.bi.vo.ScmBiOrderFulfillmentRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.bi.vo.ScmBiStockoutRespVO;

import java.time.LocalDateTime;

/**
 * 供应链 BI 销售分析 Service 接口
 *
 * @author 芋道源码
 */
public interface ScmSaleBiService {

    /**
     * 获取订单履约率
     *
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 订单履约率
     */
    ScmBiOrderFulfillmentRespVO getOrderFulfillmentRate(LocalDateTime beginTime, LocalDateTime endTime);

    /**
     * 获取缺货率
     *
     * @return 缺货率
     */
    ScmBiStockoutRespVO getStockoutRate();

}
