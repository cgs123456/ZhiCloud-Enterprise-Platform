package cn.zhicloud.module.erp.service.purchase.evaluation;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.purchase.evaluation.vo.ErpSupplierEvaluationPageReqVO;
import cn.zhicloud.module.erp.controller.admin.purchase.evaluation.vo.ErpSupplierEvaluationSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.evaluation.ErpSupplierEvaluationDO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.evaluation.ErpSupplierEvaluationItemDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP 供应商评估 Service 接口
 *
 * @author 智云
 */
public interface ErpSupplierEvaluationService {

    /**
     * 创建供应商评估
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createEvaluation(@Valid ErpSupplierEvaluationSaveReqVO createReqVO);

    /**
     * 更新供应商评估
     *
     * @param updateReqVO 更新信息
     */
    void updateEvaluation(@Valid ErpSupplierEvaluationSaveReqVO updateReqVO);

    /**
     * 删除供应商评估
     *
     * @param ids 编号数组
     */
    void deleteEvaluation(List<Long> ids);

    /**
     * 获得供应商评估
     *
     * @param id 编号
     * @return 供应商评估
     */
    ErpSupplierEvaluationDO getEvaluation(Long id);

    /**
     * 获得供应商评估分页
     *
     * @param pageReqVO 分页查询
     * @return 供应商评估分页
     */
    PageResult<ErpSupplierEvaluationDO> getEvaluationPage(ErpSupplierEvaluationPageReqVO pageReqVO);

    /**
     * 获得评估指标项列表
     *
     * @param evaluationId 评估编号
     * @return 指标项列表
     */
    List<ErpSupplierEvaluationItemDO> getEvaluationItemListByEvaluationId(Long evaluationId);

    /**
     * 自动计算供应商评估
     *
     * <p>从采购到货及时率、退货率、价格波动等自动计算评分：
     * 1. 交期评分 = 到货及时率 * 100
     * 2. 质量评分 = (1 - 退货率) * 100
     * 3. 价格评分 = 价格稳定度评分
     * 4. 服务评分 = 默认 80
     * 5. 综合评分 = 加权平均
     * 6. 等级 A(>=90) / B(>=80) / C(>=60) / D(<60)
     *
     * @param supplierId 供应商编号
     * @param period     评估周期 yyyyMM
     * @return 评估编号
     */
    Long calculateEvaluation(Long supplierId, String period);

    /**
     * 校验供应商评估存在
     *
     * @param id 编号
     * @return 供应商评估
     */
    ErpSupplierEvaluationDO validateEvaluation(Long id);

}
