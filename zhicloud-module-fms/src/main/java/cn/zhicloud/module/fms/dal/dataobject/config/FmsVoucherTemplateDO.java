package cn.zhicloud.module.fms.dal.dataobject.config;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.util.List;
import java.util.Map;

import lombok.*;

/**
 * FMS 凭证模板 DO（账套级主数据，租户表）
 *
 * @author 智云
 */
@TableName(value = "fms_voucher_template", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FmsVoucherTemplateDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 账套编号
     */
    private Long accountSetId;
    /**
     * 模板名称
     */
    private String name;
    /**
     * 分类编号
     */
    private Long categoryId;
    /**
     * 凭证模板分录数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> entries;

    /** 分录元素类型别名 */

}
