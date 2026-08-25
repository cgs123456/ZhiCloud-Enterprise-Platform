package cn.zhicloud.module.aimultiagent.dal.dataobject;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 多 Agent 拓扑配置 DO
 *
 * @author zhicloud
 */
@TableName(value = "aimultiagent_topology", autoResultMap = true)
@KeySequence("aimultiagent_topology")
@Data
public class MultiAgentTopologyDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 拓扑名称
     */
    private String name;
    /**
     * 描述
     */
    private String description;
    /**
     * Supervisor 系统提示词
     */
    private String supervisorSystemPrompt;
    /**
     * Worker 配置 JSON（workerName, tools, systemPrompt）
     */
    private String workerConfig;
    /**
     * 最大调用深度（防死循环）
     */
    private Integer maxDepth;
    /**
     * Token 预算上限
     */
    private Integer maxTokenBudget;
    /**
     * 状态（0启用 1停用）
     */
    private Integer status;
    /**
     * 配置 schema 版本（如 v1）
     */
    private String version;

}
