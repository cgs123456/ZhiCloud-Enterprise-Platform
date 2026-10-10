package cn.zhicloud.module.fms.dal.dataobject.config;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

/**
 * FMS 辅助核算项目 DO（账套级主数据，租户表）
 *
 * @author 智云
 */
@TableName(value = "fms_auxiliary_item")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FmsAuxiliaryItemDO extends TenantBaseDO {

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
     * 辅助核算类别编号
     */
    private Long auxiliaryTypeId;
    /**
     * 项目编码
     */
    private String code;
    /**
     * 项目名称
     */
    private String name;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;
    /**
     * 规格
     */
    private String specification;
    /**
     * 单位
     */
    private String unit;

}
