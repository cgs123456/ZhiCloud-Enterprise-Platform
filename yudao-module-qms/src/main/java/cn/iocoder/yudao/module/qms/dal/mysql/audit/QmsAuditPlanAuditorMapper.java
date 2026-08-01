package cn.iocoder.yudao.module.qms.dal.mysql.audit;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.qms.dal.dataobject.audit.QmsAuditPlanAuditorDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * QMS 审核组成员 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface QmsAuditPlanAuditorMapper extends BaseMapperX<QmsAuditPlanAuditorDO> {

    default List<QmsAuditPlanAuditorDO> selectListByPlanId(Long planId) {
        return selectList(QmsAuditPlanAuditorDO::getPlanId, planId);
    }

}
