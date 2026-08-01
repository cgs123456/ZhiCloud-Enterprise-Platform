package cn.iocoder.yudao.module.qms.dal.mysql.sqm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.qms.controller.admin.sqm.vo.SupplierAuditPageReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.sqm.SupplierAuditDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * QMS 供应商审核 Mapper
 *
 * @author yudao
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