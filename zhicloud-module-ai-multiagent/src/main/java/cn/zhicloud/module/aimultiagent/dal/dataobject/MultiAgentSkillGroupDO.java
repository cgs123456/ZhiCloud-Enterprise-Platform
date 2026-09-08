package cn.zhicloud.module.aimultiagent.dal.dataobject;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 多 Agent 技能分组 DO（Level-1）
 *
 * <p>技能两级结构的第一级：按业务域划分技能分组，编码与
 * {@link cn.zhicloud.module.aimultiagent.enums.SkillCategory} 的 Level-1 保持一致。
 *
 * @author zhicloud
 */
@TableName(value = "aimultiagent_skill_group", autoResultMap = true)
@KeySequence("aimultiagent_skill_group")
@Data
public class MultiAgentSkillGroupDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 分组编码（wms/qms/procurement/sales/report/ai_rag_eval）
     */
    private String code;
    /**
     * 分组名称
     */
    private String name;
    /**
     * 分组描述
     */
    private String description;
    /**
     * 状态（0开启 1关闭）
     */
    private Integer status;
    /**
     * 排序
     */
    private Integer sort;

}
