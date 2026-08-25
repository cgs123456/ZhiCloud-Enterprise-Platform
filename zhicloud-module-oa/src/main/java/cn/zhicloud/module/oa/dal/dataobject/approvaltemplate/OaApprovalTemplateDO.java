package cn.zhicloud.module.oa.dal.dataobject.approvaltemplate;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * OA 审批模板 DO
 *
 * @author zhicloud
 */
@TableName("oa_approval_template")
@KeySequence("oa_approval_template_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OaApprovalTemplateDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 模板编码
     */
    private String code;
    /**
     * 模板名称
     */
    private String name;
    /**
     * 分类（通用/人事/财务/采购/合同/其他）
     */
    private String category;
    /**
     * BPM 流程定义 KEY
     */
    private String processDefinitionKey;
    /**
     * 表单 JSON Schema（字段定义）
     */
    private String formSchema;
    /**
     * 表单 UI Schema（布局/校验）
     */
    private String formUiSchema;
    /**
     * 描述
     */
    private String description;
    /**
     * 图标
     */
    private String icon;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 状态
     * <p>
     * 0 启用 1 停用
     */
    private Integer status;
    /**
     * 使用次数
     */
    private Integer usageCount;

}
