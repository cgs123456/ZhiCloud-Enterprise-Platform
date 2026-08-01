package cn.iocoder.yudao.module.ai.dal.dataobject.prompt;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.ai.enums.prompt.AiPromptTemplateCategoryEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * AI Prompt 模板 DO
 *
 * @author yudao
 */
@TableName(value = "ai_prompt_template", autoResultMap = true)
@KeySequence("ai_prompt_template_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
public class AiPromptTemplateDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 模板名称
     */
    private String name;
    /**
     * 模板编码
     */
    private String code;
    /**
     * 分类
     * <p>
     * 枚举 {@link AiPromptTemplateCategoryEnum}
     */
    private String category;
    /**
     * 模板内容（含变量占位符，如 {variableName}）
     */
    private String content;
    /**
     * 变量列表（JSON 格式）
     */
    private String variables;
    /**
     * 描述
     */
    private String description;
    /**
     * 状态
     * <p>
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
