package cn.zhicloud.module.aimultiagent.dal.mysql;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentSkillDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 多 Agent 技能 Mapper（Level-2）
 *
 * @author zhicloud
 */
@Mapper
public interface MultiAgentSkillMapper extends BaseMapperX<MultiAgentSkillDO> {

    /**
     * 按技能编码查询
     *
     * @param code 技能编码
     * @return 技能，无则返回 null
     */
    default MultiAgentSkillDO selectByCode(String code) {
        return selectOne(new LambdaQueryWrapperX<MultiAgentSkillDO>()
                .eq(MultiAgentSkillDO::getCode, code));
    }

    /**
     * 按绑定的工具名查询
     *
     * @param toolName WorkerToolExecutor 工具名
     * @return 技能，无则返回 null
     */
    default MultiAgentSkillDO selectByToolName(String toolName) {
        return selectOne(new LambdaQueryWrapperX<MultiAgentSkillDO>()
                .eq(MultiAgentSkillDO::getToolName, toolName));
    }

    /**
     * 按分组查询技能（按 sort 升序）
     *
     * @param groupId 分组编号
     * @return 技能列表
     */
    default List<MultiAgentSkillDO> selectListByGroupId(Long groupId) {
        return selectList(new LambdaQueryWrapperX<MultiAgentSkillDO>()
                .eq(MultiAgentSkillDO::getGroupId, groupId)
                .orderByAsc(MultiAgentSkillDO::getSort)
                .orderByAsc(MultiAgentSkillDO::getId));
    }

    /**
     * 查询全部开启的技能
     *
     * @return 开启的技能列表
     */
    default List<MultiAgentSkillDO> selectListByStatusOn() {
        return selectList(new LambdaQueryWrapperX<MultiAgentSkillDO>()
                .eq(MultiAgentSkillDO::getStatus, 0)
                .orderByAsc(MultiAgentSkillDO::getId));
    }

}
