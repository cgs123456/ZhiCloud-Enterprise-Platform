package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFinanceReceiptItemDO;
import cn.zhicloud.module.erp.service.finance.bo.ErpFinanceReceiptCrmCreateReqBO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

/**
 * ERP 收款单 Service 接口
 *
 * @author 智云
 */
public interface ErpFinanceReceiptService {

    /**
     * 创建收款单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createFinanceReceipt(@Valid ErpFinanceReceiptSaveReqVO createReqVO);

    /**
     * 创建「CRM 回款联动」收款单（跨域集成入口，§12.267 D1）
     *
     * <p>与 {@link #createFinanceReceipt} 的区别：入参是集成契约 BO 而非 HTTP VO——
     * HTTP VO 的账户/财务人员为必填（Bean Validation 对内部调用同样生效），
     * 而 CRM 域没有这两个维度：账户留空由财务审核过账前补选。
     *
     * @param createReqBO 集成建单信息（幂等键由调用方按回款主键推导）
     * @return 编号
     */
    Long createFinanceReceiptFromCrm(ErpFinanceReceiptCrmCreateReqBO createReqBO);

    /**
     * 更新收款单
     *
     * @param updateReqVO 更新信息
     */
    void updateFinanceReceipt(@Valid ErpFinanceReceiptSaveReqVO updateReqVO);

    /**
     * 更新收款单的状态
     *
     * @param id 编号
     * @param status 状态
     */
    void updateFinanceReceiptStatus(Long id, Integer status);

    /**
     * 删除收款单
     *
     * @param ids 编号数组
     */
    void deleteFinanceReceipt(List<Long> ids);

    /**
     * 获得收款单
     *
     * @param id 编号
     * @return 收款单
     */
    ErpFinanceReceiptDO getFinanceReceipt(Long id);

    /**
     * 获得收款单分页
     *
     * @param pageReqVO 分页查询
     * @return 收款单分页
     */
    PageResult<ErpFinanceReceiptDO> getFinanceReceiptPage(ErpFinanceReceiptPageReqVO pageReqVO);

    // ==================== 收款单项 ====================

    /**
     * 获得收款单项列表
     *
     * @param receiptId 收款单编号
     * @return 收款单项列表
     */
    List<ErpFinanceReceiptItemDO> getFinanceReceiptItemListByReceiptId(Long receiptId);

    /**
     * 获得收款单项 List
     *
     * @param receiptIds 收款单编号数组
     * @return 收款单项 List
     */
    List<ErpFinanceReceiptItemDO> getFinanceReceiptItemListByReceiptIds(Collection<Long> receiptIds);

}