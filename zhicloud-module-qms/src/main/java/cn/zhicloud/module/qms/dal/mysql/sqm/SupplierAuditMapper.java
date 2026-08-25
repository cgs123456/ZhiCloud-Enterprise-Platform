package cn.zhicloud.module.qms.dal.mysql.sqm;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.qms.controller.admin.sqm.vo.SupplierAuditPageReqVO;
import cn.zhicloud.module.qms.dal.dataobject.sqm.SupplierAuditDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * QMS 供应商审核 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface SupplierAuditMapper extends BaseMapperX<SupplierAuditDO> {

    default PageResult<SupplierAuditDO> selectPage(SupplierAuditPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SupplierAuditDO>()
                .likeIfPresent(SupplierAuditDO::getAuditNo, reqVO.getAuditNo())
                .likeIfPresent(SupplierAuditDO::getAuditName, reqVO.getAuditName())
                .eqIfPresent(SupplierAuditDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(SupplierAuditDO::getStatus, reqVO.getStatus())
                .orderByDesc(SupplierAuditDO::getId));
    }

    default SupplierAuditDO selectByAuditNo(String auditNo) {
        return selectOne(SupplierAuditDO::getAuditNo, auditNo);
    }

}