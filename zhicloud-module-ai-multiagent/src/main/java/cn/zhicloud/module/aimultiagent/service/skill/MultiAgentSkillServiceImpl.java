package cn.zhicloud.module.aimultiagent.service.skill;

import cn.hutool.core.util.StrUtil;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.aimultiagent.controller.admin.skill.vo.MultiAgentSkillGroupRespVO;
import cn.zhicloud.module.aimultiagent.controller.admin.skill.vo.MultiAgentSkillRespVO;
import cn.zhicloud.module.aimultiagent.controller.admin.skill.vo.MultiAgentSkillSaveReqVO;
import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentSkillDO;
import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentSkillGroupDO;
import cn.zhicloud.module.aimultiagent.dal.mysql.MultiAgentSkillGroupMapper;
import cn.zhicloud.module.aimultiagent.dal.mysql.MultiAgentSkillMapper;
import cn.zhicloud.module.aimultiagent.util.MultiAgentSsrfGuard;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.aimultiagent.enums.ErrorCodeConstants.*;

/**
 * 多 Agent 技能目录 Service 实现
 *
 * @author zhicloud
 */
@Service
@Validated
@Slf4j
public class MultiAgentSkillServiceImpl implements MultiAgentSkillService {

    @Resource
    private MultiAgentSkillGroupMapper skillGroupMapper;
    @Resource
    private MultiAgentSkillMapper skillMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<MultiAgentSkillGroupRespVO> getSkillCatalog() {
        List<MultiAgentSkillGroupDO> groups = skillGroupMapper.selectList();
        List<MultiAgentSkillGroupRespVO> result = new ArrayList<>(groups.size());
        for (MultiAgentSkillGroupDO group : groups) {
            MultiAgentSkillGroupRespVO groupVO = BeanUtils.toBean(group, MultiAgentSkillGroupRespVO.class);
            List<MultiAgentSkillDO> skills = skillMapper.selectListByGroupId(group.getId());
            List<MultiAgentSkillRespVO> skillVOs = BeanUtils.toBean(skills, MultiAgentSkillRespVO.class);
            // 回填分组编码/名称，便于前端展示
            for (MultiAgentSkillRespVO skillVO : skillVOs) {
                skillVO.setGroupCode(group.getCode());
                skillVO.setGroupName(group.getName());
            }
            groupVO.setSkills(skillVOs);
            result.add(groupVO);
        }
        return result;
    }

    @Override
    public MultiAgentSkillDO getSkillByToolName(String toolName) {
        if (StrUtil.isBlank(toolName)) {
            return null;
        }
        return skillMapper.selectByToolName(toolName.trim());
    }

    @Override
    public Long createSkill(MultiAgentSkillSaveReqVO reqVO) {
        validateGroupExists(reqVO.getGroupId());
        validateCodeDuplicate(null, reqVO.getCode());
        validateConfigJson(reqVO.getConfigJson());
        MultiAgentSkillDO skill = BeanUtils.toBean(reqVO, MultiAgentSkillDO.class);
        if (skill.getStatus() == null) {
            skill.setStatus(0);
        }
        if (skill.getSort() == null) {
            skill.setSort(0);
        }
        skillMapper.insert(skill);
        return skill.getId();
    }

    @Override
    public void updateSkill(Long id, MultiAgentSkillSaveReqVO reqVO) {
        validateSkillExists(id);
        validateGroupExists(reqVO.getGroupId());
        validateCodeDuplicate(id, reqVO.getCode());
        validateConfigJson(reqVO.getConfigJson());
        MultiAgentSkillDO skill = BeanUtils.toBean(reqVO, MultiAgentSkillDO.class);
        skill.setId(id);
        skillMapper.updateById(skill);
    }

    @Override
    public void updateSkillStatus(Long id, Integer status) {
        validateSkillExists(id);
        MultiAgentSkillDO skill = new MultiAgentSkillDO();
        skill.setId(id);
        skill.setStatus(status);
        skillMapper.updateById(skill);
    }

    @Override
    public void deleteSkill(Long id) {
        validateSkillExists(id);
        skillMapper.deleteById(id);
    }

    @Override
    public MultiAgentSkillRespVO getSkill(Long id) {
        MultiAgentSkillDO skill = validateSkillExists(id);
        MultiAgentSkillRespVO respVO = BeanUtils.toBean(skill, MultiAgentSkillRespVO.class);
        MultiAgentSkillGroupDO group = skillGroupMapper.selectById(skill.getGroupId());
        if (group != null) {
            respVO.setGroupCode(group.getCode());
            respVO.setGroupName(group.getName());
        }
        return respVO;
    }

    // ==================== 校验 ====================

    private void validateGroupExists(Long groupId) {
        if (groupId == null || skillGroupMapper.selectById(groupId) == null) {
            throw exception(SKILL_GROUP_NOT_EXISTS);
        }
    }

    private MultiAgentSkillDO validateSkillExists(Long id) {
        MultiAgentSkillDO skill = skillMapper.selectById(id);
        if (skill == null) {
            throw exception(SKILL_NOT_EXISTS);
        }
        return skill;
    }

    private void validateCodeDuplicate(Long id, String code) {
        MultiAgentSkillDO exist = skillMapper.selectByCode(code);
        if (exist != null && !exist.getId().equals(id)) {
            throw exception(SKILL_CODE_DUPLICATE, code);
        }
    }

    /**
     * 校验技能配置 JSON：格式须合法；若携带 endpoint_url / endpointUrl / url，
     * 须通过 SSRF 安全校验，否则拒绝保存。
     *
     * @param configJson 技能配置 JSON（可为空）
     */
    private void validateConfigJson(String configJson) {
        if (StrUtil.isBlank(configJson)) {
            return;
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(configJson);
        } catch (Exception e) {
            log.warn("[validateConfigJson][技能配置 JSON 解析失败]");
            throw exception(SKILL_CONFIG_INVALID);
        }
        for (String key : new String[]{"endpoint_url", "endpointUrl", "url"}) {
            JsonNode node = root.get(key);
            if (node != null && node.isTextual() && StrUtil.isNotBlank(node.asText())) {
                // SSRF 校验失败直接抛 SKILL_URL_BLOCKED，拒绝保存
                MultiAgentSsrfGuard.validateSafeUrl(node.asText());
            }
        }
    }

}
