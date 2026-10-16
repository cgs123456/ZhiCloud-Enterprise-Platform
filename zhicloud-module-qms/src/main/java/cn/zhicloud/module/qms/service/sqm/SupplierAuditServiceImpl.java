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
        // 状态无条件置为初始值，忽略调用方传入的任何 status。
        // §12.235.5：此前「仅在 null 时补默认」，调用方即可在**建单**这一步直接写入终态，
        // 跳过整个流转（该类可能连流转方法都没有），产生「未审即批/未做即完成」的记录。
        // 状态只能由本模块的流转方法产生。
        supplierAudit.setStatus(SupplierAuditStatusEnum.PLANNED.getStatus());
        supplierAuditMapper.insert(supplierAudit);
        return supplierAudit.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSupplierAudit(SupplierAuditSaveReqVO updateReqVO) {
        validateSupplierAuditExists(updateReqVO.getId());
        SupplierAuditDO updateObj = BeanUtils.toBean(updateReqVO, SupplierAuditDO.class);
        // 状态不接受外部写入（§12.236.7）。本类**没有任何流转方法**，
        // 若允许通用修改写状态，则「新增 + 修改」两步就能到达任意终态，
        // 新增侧刚加的护栏形同虚设。
        // MyBatis-Plus 的 updateById 跳过 null 字段，故置 null 的效果是
        // 该列不进 UPDATE 语句 —— 调用方的 status 无法生效。
        updateObj.setStatus(null);
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