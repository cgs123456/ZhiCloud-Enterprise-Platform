package cn.zhicloud.module.oa.dal.mysql.approvaltemplate;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.oa.controller.admin.approvaltemplate.vo.OaApprovalTemplatePageReqVO;
import cn.zhicloud.module.oa.dal.dataobject.approvaltemplate.OaApprovalTemplateDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * OA 审批模板 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface OaApprovalTemplateMapper extends BaseMapperX<OaApprovalTemplateDO> {

    default PageResult<OaApprovalTemplateDO> selectPage(OaApprovalTemplatePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OaApprovalTemplateDO>()
                .likeIfPresent(OaApprovalTemplateDO::getCode, reqVO.getCode())
                .likeIfPresent(OaApprovalTemplateDO::getName, reqVO.getName())
                .eqIfPresent(OaApprovalTemplateDO::getCategory, reqVO.getCategory())
                .eqIfPresent(OaApprovalTemplateDO::getStatus, reqVO.getStatus())
                .orderByAsc(OaApprovalTemplateDO::getSort)
                .orderByDesc(OaApprovalTemplateDO::getId));
    }

    default OaApprovalTemplateDO selectByCode(String code) {
        return selectOne(OaApprovalTemplateDO::getCode, code);
    }

}
