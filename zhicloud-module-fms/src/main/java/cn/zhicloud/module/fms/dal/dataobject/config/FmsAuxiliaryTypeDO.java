package cn.zhicloud.module.fms.dal.dataobject.config;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

/**
 * FMS 辅助核算类别 DO（账套级主数据，租户表）
 *
 * @author 智云
 */
@TableName(value = "fms_auxiliary_type")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FmsAuxiliaryTypeDO extends TenantBaseDO {

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
     * 类别名称
     */
    private String name;
    /**
     * 辅助核算类型
     */
    private Integer type;
    /**
     * 是否系统预置
     */
    private Boolean systemPreset;

}
