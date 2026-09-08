package cn.zhicloud.module.aimultiagent.service.skill;

import cn.zhicloud.module.aimultiagent.controller.admin.skill.vo.MultiAgentSkillGroupRespVO;
import cn.zhicloud.module.aimultiagent.controller.admin.skill.vo.MultiAgentSkillRespVO;
import cn.zhicloud.module.aimultiagent.controller.admin.skill.vo.MultiAgentSkillSaveReqVO;
import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentSkillDO;

import java.util.List;

/**
 * 多 Agent 技能目录 Service
 *
 * <p>维护两级技能目录（分组 Level-1 → 技能 Level-2）。技能写入时若 configJson
 * 携带 endpoint_url（endpointUrl/url），经 SSRF 校验，不安全直接拒绝保存。
 *
 * @author zhicloud
 */
public interface MultiAgentSkillService {

    /**
     * 查询完整技能目录（分组及其下属技能，仅返回未删除）
     *
     * @return 分组列表（每组携带技能列表）
     */
    List<MultiAgentSkillGroupRespVO> getSkillCatalog();

    /**
     * 按工具名查询技能
     *
     * @param toolName WorkerToolExecutor 工具名
     * @return 技能，无则返回 null
     */
    MultiAgentSkillDO getSkillByToolName(String toolName);

    /**
     * 新建技能
     *
     * @param reqVO 新建请求
     * @return 技能编号
     */
    Long createSkill(MultiAgentSkillSaveReqVO reqVO);

    /**
     * 更新技能
     *
     * @param id    技能编号
     * @param reqVO 更新请求
     */
    void updateSkill(Long id, MultiAgentSkillSaveReqVO reqVO);

    /**
     * 更新技能状态（启用/禁用）
     *
     * @param id     技能编号
     * @param status 状态（0开启 1关闭）
     */
    void updateSkillStatus(Long id, Integer status);

    /**
     * 删除技能（逻辑删除）
     *
     * @param id 技能编号
     */
    void deleteSkill(Long id);

    /**
     * 查询技能详情
     *
     * @param id 技能编号
     * @return 技能详情
     */
    MultiAgentSkillRespVO getSkill(Long id);

}
