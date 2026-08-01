package cn.iocoder.yudao.module.oa.dal.mysql.approvaltemplate;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.oa.controller.admin.approvaltemplate.vo.OaApprovalTemplatePageReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.approvaltemplate.OaApprovalTemplateDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * OA 审批模板 Mapper
 *
 * @author yudao
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
