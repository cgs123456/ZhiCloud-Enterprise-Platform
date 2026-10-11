package cn.zhicloud.module.hr.dal.mysql.performance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.controller.admin.hrm.performance.vo.HrmPerformanceTemplatePageReqVO;
import cn.zhicloud.module.hr.dal.dataobject.performance.HrmPerformanceAssessmentTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 绩效考核模板 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmPerformanceAssessmentTemplateMapper extends BaseMapperX<HrmPerformanceAssessmentTemplateDO> {

    default PageResult<HrmPerformanceAssessmentTemplateDO> selectPage(HrmPerformanceTemplatePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrmPerformanceAssessmentTemplateDO>()
                .likeIfPresent(HrmPerformanceAssessmentTemplateDO::getName, reqVO.getName())
                .orderByDesc(HrmPerformanceAssessmentTemplateDO::getId));
    }

    default List<HrmPerformanceAssessmentTemplateDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<HrmPerformanceAssessmentTemplateDO>()
                .orderByDesc(HrmPerformanceAssessmentTemplateDO::getId));
    }

    default HrmPerformanceAssessmentTemplateDO selectByName(String name) {
        return selectOne(HrmPerformanceAssessmentTemplateDO::getName, name);
    }

}
