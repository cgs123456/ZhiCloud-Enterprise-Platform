package cn.zhicloud.module.qms.service.sqm;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.sqm.vo.SupplierAuditPageReqVO;
import cn.zhicloud.module.qms.controller.admin.sqm.vo.SupplierAuditSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.sqm.SupplierAuditDO;
import cn.zhicloud.module.qms.dal.mysql.sqm.SupplierAuditMapper;
import cn.zhicloud.module.qms.enums.qms.SupplierAuditStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.SUPPLIER_AUDIT_NOT_EXISTS;

/**
 * QMS 供应商审核 Service 实现类
 *
 * @author zhicloud
 */
@Service
@Validated
public class SupplierAuditServiceImpl implements SupplierAuditService {

    @Resource
    private SupplierAuditMapper supplierAuditMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSupplierAudit(SupplierAuditSaveReqVO createReqVO) {
        SupplierAuditDO supplierAudit = BeanUtils.toBean(createReqVO, SupplierAuditDO.class);
        if (supplierAudit.getStatus() == null) {
            supplierAudit.setStatus(SupplierAuditStatusEnum.PLANNED.getStatus());
        }
        supplierAuditMapper.insert(supplierAudit);
        return supplierAudit.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSupplierAudit(SupplierAuditSaveReqVO updateReqVO) {
        validateSupplierAuditExists(updateReqVO.getId());
        SupplierAuditDO updateObj = BeanUtils.toBean(updateReqVO, SupplierAuditDO.class);
        supplierAuditMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSupplierAudit(Long id) {
        validateSupplierAuditExists(id);
        supplierAuditMapper.deleteById(id);
    }

    private void validateSupplierAuditExists(Long id) {
        if (supplierAuditMapper.selectById(id) == null) {
            throw exception(SUPPLIER_AUDIT_NOT_EXISTS);
        }
    }

    @Override
    public SupplierAuditDO getSupplierAudit(Long id) {
        return supplierAuditMapper.selectById(id);
    }

    @Override
    public PageResult<SupplierAuditDO> getSupplierAuditPage(SupplierAuditPageReqVO pageReqVO) {
        return supplierAuditMapper.selectPage(pageReqVO);
    }

}