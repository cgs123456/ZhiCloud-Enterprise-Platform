package cn.zhicloud.module.aimultiagent.dal.mysql;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentSkillGroupDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 多 Agent 技能分组 Mapper（Level-1）
 *
 * @author zhicloud
 */
@Mapper
public interface MultiAgentSkillGroupMapper extends BaseMapperX<MultiAgentSkillGroupDO> {

    /**
     * 按编码查询分组
     *
     * @param code 分组编码
     * @return 分组，无则返回 null
     */
    default MultiAgentSkillGroupDO selectByCode(String code) {
        return selectOne(new LambdaQueryWrapperX<MultiAgentSkillGroupDO>()
                .eq(MultiAgentSkillGroupDO::getCode, code));
    }

    /**
     * 查询全部开启的分组（按 sort 升序）
     *
     * @return 开启的分组列表
     */
    default List<MultiAgentSkillGroupDO> selectListByStatusOn() {
        return selectList(new LambdaQueryWrapperX<MultiAgentSkillGroupDO>()
                .eq(MultiAgentSkillGroupDO::getStatus, 0)
                .orderByAsc(MultiAgentSkillGroupDO::getSort)
                .orderByAsc(MultiAgentSkillGroupDO::getId));
    }

}
