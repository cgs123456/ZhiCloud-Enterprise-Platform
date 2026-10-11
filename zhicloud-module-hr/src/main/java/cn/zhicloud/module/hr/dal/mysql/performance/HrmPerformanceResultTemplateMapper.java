package cn.zhicloud.module.hr.dal.mysql.performance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.controller.admin.hrm.performance.vo.HrmPerformanceTemplatePageReqVO;
import cn.zhicloud.module.hr.dal.dataobject.performance.HrmPerformanceResultTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 绩效结果模板 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmPerformanceResultTemplateMapper extends BaseMapperX<HrmPerformanceResultTemplateDO> {

    default PageResult<HrmPerformanceResultTemplateDO> selectPage(HrmPerformanceTemplatePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrmPerformanceResultTemplateDO>()
                .likeIfPresent(HrmPerformanceResultTemplateDO::getName, reqVO.getName())
                .orderByDesc(HrmPerformanceResultTemplateDO::getId));
    }

    default List<HrmPerformanceResultTemplateDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<HrmPerformanceResultTemplateDO>()
                .orderByDesc(HrmPerformanceResultTemplateDO::getId));
    }

    default HrmPerformanceResultTemplateDO selectByName(String name) {
        return selectOne(HrmPerformanceResultTemplateDO::getName, name);
    }

}
