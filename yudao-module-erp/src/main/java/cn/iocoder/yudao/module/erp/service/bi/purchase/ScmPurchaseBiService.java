package cn.iocoder.yudao.module.erp.service.bi.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.bi.vo.ScmBiPriceFluctuationRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.bi.vo.ScmBiPurchaseOnTimeRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.bi.vo.ScmBiSupplierScoreRespVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 供应链 BI 采购分析 Service 接口
 *
 * @author 芋道源码
 */
public interface ScmPurchaseBiService {

    /**
     * 获取采购到货及时率
     *
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 采购到货及时率
     */
    ScmBiPurchaseOnTimeRespVO getPurchaseOnTimeRate(LocalDateTime beginTime, LocalDateTime endTime);

    /**
     * 获取采购价格波动
     *
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 价格波动列表
     */
    List<ScmBiPriceFluctuationRespVO> getPriceFluctuation(LocalDateTime beginTime, LocalDateTime endTime);

    /**
     * 获取供应商绩效评分
     *
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 供应商绩效列表
     */
    List<ScmBiSupplierScoreRespVO> getSupplierPerformance(LocalDateTime beginTime, LocalDateTime endTime);

}
