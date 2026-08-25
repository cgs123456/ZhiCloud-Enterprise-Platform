package cn.zhicloud.module.bpm.dal.mysql.definition;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.module.bpm.controller.admin.definition.vo.expression.BpmProcessExpressionPageReqVO;
import cn.zhicloud.module.bpm.dal.dataobject.definition.BpmProcessExpressionDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * BPM 流程表达式 Mapper
 *
 * @author 智云
 */
@Mapper
public interface BpmProcessExpressionMapper extends BaseMapperX<BpmProcessExpressionDO> {

    default PageResult<BpmProcessExpressionDO> selectPage(BpmProcessExpressionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<BpmProcessExpressionDO>()
                .likeIfPresent(BpmProcessExpressionDO::getName, reqVO.getName())
                .eqIfPresent(BpmProcessExpressionDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(BpmProcessExpressionDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(BpmProcessExpressionDO::getId));
    }

}