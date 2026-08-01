package cn.iocoder.yudao.module.crm.dal.mysql.visit;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.crm.controller.admin.visit.vo.CrmVisitRecordPageReqVO;
import cn.iocoder.yudao.module.crm.dal.dataobject.visit.CrmVisitRecordDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * CRM 拜访签到记录 Mapper
 *
 * @author 芋道源码
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
