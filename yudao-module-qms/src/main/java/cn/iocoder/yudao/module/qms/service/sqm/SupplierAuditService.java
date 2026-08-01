package cn.iocoder.yudao.module.qms.service.sqm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.qms.controller.admin.sqm.vo.SupplierAuditPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.sqm.vo.SupplierAuditSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.sqm.SupplierAuditDO;
import jakarta.validation.Valid;

/**
 * QMS 供应商审核 Service 接口
 *
 * @author yudao
 */
public interface SupplierAuditService {

    Long createSupplierAudit(@Valid SupplierAuditSaveReqVO createReqVO);

    void updateSupplierAudit(@Valid SupplierAuditSaveReqVO updateReqVO);

    void deleteSupplierAudit(Long id);

    SupplierAuditDO getSupplierAudit(Long id);

    PageResult<SupplierAuditDO> getSupplierAuditPage(SupplierAuditPageReqVO pageReqVO);

}