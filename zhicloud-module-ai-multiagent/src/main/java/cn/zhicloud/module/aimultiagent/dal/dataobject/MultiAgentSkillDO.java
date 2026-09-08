package cn.zhicloud.module.aimultiagent.dal.dataobject;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 多 Agent 技能 DO（Level-2）
 *
 * <p>技能两级结构的第二级：具体技能，通过 toolName 绑定
 * WorkerToolExecutor 的真实工具；toolName 为空表示纯提示词技能。
 * config_json 中的 endpoint_url（如有）在写入时经 SSRF 校验。
 *
 * @author zhicloud
 */
@TableName(value = "aimultiagent_skill", autoResultMap = true)
@KeySequence("aimultiagent_skill")
@Data
public class MultiAgentSkillDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 所属分组 aimultiagent_skill_group.id
     */
    private Long groupId;
    /**
     * 技能编码（组内唯一，如 wms:receipt_order_list）
     */
    private String code;
    /**
     * 技能名称
     */
    private String name;
    /**
     * 绑定的 WorkerToolExecutor 工具名（为空表示纯提示词技能）
     */
    private String toolName;
    /**
     * 技能描述
     */
    private String description;
    /**
     * 技能配置 JSON（可选 endpoint_url，写入时做 SSRF 校验）
     */
    private String configJson;
    /**
     * 状态（0开启 1关闭）
     */
    private Integer status;
    /**
     * 排序
     */
    private Integer sort;

}
