package cn.zhicloud.module.crm.service.invoice;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.crm.controller.admin.invoice.vo.CrmInvoicePageReqVO;
import cn.zhicloud.module.crm.controller.admin.invoice.vo.CrmInvoiceSaveReqVO;
import cn.zhicloud.module.crm.dal.dataobject.invoice.CrmInvoiceDO;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * CRM 开票 Service 接口
 *
 * @author 智云
 */
public interface CrmInvoiceService {

    /**
     * 创建开票
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createInvoice(@Valid CrmInvoiceSaveReqVO createReqVO);

    /**
     * 更新开票
     *
     * @param updateReqVO 更新信息
     */
    void updateInvoice(@Valid CrmInvoiceSaveReqVO updateReqVO);

    /**
     * 删除开票
     *
     * @param id 编号
     */
    void deleteInvoice(Long id);

    /**
     * 获得开票
     *
     * @param id 编号
     * @return 开票
     */
    CrmInvoiceDO getInvoice(Long id);

    /**
     * 获得开票列表
     *
     * @param ids 编号
     * @return 开票列表
     */
    List<CrmInvoiceDO> getInvoiceList(Collection<Long> ids);

    /**
     * 获得开票 Map
     *
     * @param ids 编号
     * @return 开票 Map
     */
    default Map<Long, CrmInvoiceDO> getInvoiceMap(Collection<Long> ids) {
        return convertMap(getInvoiceList(ids), CrmInvoiceDO::getId);
    }

    /**
     * 获得开票分页
     *
     * @param pageReqVO 分页查询
     * @return 开票分页
     */
    PageResult<CrmInvoiceDO> getInvoicePage(CrmInvoicePageReqVO pageReqVO);

    /**
     * 获得开票分页，基于指定合同
     *
     * @param pageReqVO 分页查询
     * @return 开票分页
     */
    PageResult<CrmInvoiceDO> getInvoicePageByContractId(CrmInvoicePageReqVO pageReqVO);

    /**
     * 发起开票审批流程
     *
     * @param id     开票编号
     * @param userId 用户编号
     */
    void submitInvoice(Long id, Long userId);

    /**
     * 更新开票流程审批结果
     *
     * @param id        开票编号
     * @param bpmResult BPM 审批结果
     */
    void updateInvoiceAuditStatus(Long id, Integer bpmResult);

    /**
     * 获得合同已开票金额 Map
     *
     * @param contractIds 合同编号
     * @return 开票金额 Map
     */
    Map<Long, BigDecimal> getInvoicePriceMapByContractId(Collection<Long> contractIds);

}
