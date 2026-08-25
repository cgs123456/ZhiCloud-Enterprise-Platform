package cn.zhicloud.module.crm.dal.mysql.visit;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.crm.controller.admin.visit.vo.CrmVisitRecordPageReqVO;
import cn.zhicloud.module.crm.dal.dataobject.visit.CrmVisitRecordDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * CRM 拜访签到记录 Mapper
 *
 * @author 智云
 */
@Mapper
public interface CrmVisitRecordMapper extends BaseMapperX<CrmVisitRecordDO> {

    default PageResult<CrmVisitRecordDO> selectPage(CrmVisitRecordPageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<CrmVisitRecordDO>()
                .eqIfPresent(CrmVisitRecordDO::getCustomerId, pageReqVO.getCustomerId())
                .eqIfPresent(CrmVisitRecordDO::getContactId, pageReqVO.getContactId())
                .eqIfPresent(CrmVisitRecordDO::getVisitType, pageReqVO.getVisitType())
                .orderByDesc(CrmVisitRecordDO::getId));
    }

}
