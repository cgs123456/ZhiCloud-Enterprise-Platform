package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.profitability.ErpProfitabilityAnalysisPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.profitability.ErpProfitabilityAnalysisSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpProfitabilityAnalysisDO;
import jakarta.validation.Valid;

/**
 * ERP 获利能力分析 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpProfitabilityAnalysisService {

    /**
     * 创建获利分析记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createProfitabilityAnalysis(@Valid ErpProfitabilityAnalysisSaveReqVO createReqVO);

    /**
     * 更新获利分析记录
     *
     * @param updateReqVO 更新信息
     */
    void updateProfitabilityAnalysis(@Valid ErpProfitabilityAnalysisSaveReqVO updateReqVO);

    /**
     * 删除获利分析记录
     *
     * @param id 编号
     */
    void deleteProfitabilityAnalysis(Long id);

    /**
     * 获得获利分析记录
     *
     * @param id 编号
     * @return 获利分析记录
     */
    ErpProfitabilityAnalysisDO getProfitabilityAnalysis(Long id);

    /**
     * 获得获利分析分页
     *
     * @param pageReqVO 分页查询
     * @return 获利分析分页
     */
    PageResult<ErpProfitabilityAnalysisDO> getProfitabilityAnalysisPage(ErpProfitabilityAnalysisPageReqVO pageReqVO);

    /**
     * 计算指定利润中心在某期间的获利能力
     *
     * <p>基于传入的收入和成本，计算利润 = 收入 - 成本，利润率 = 利润 / 收入。
     * <p>如果同利润中心 + 期间已有记录则更新，否则新建。
     *
     * @param profitCenterId 利润中心 ID
     * @param periodId       会计期间 ID
     * @return 获利分析记录
     */
    ErpProfitabilityAnalysisDO calculateProfitability(Long profitCenterId, Long periodId);

}
