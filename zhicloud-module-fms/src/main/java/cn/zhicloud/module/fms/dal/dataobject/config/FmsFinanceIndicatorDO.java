package cn.zhicloud.module.fms.dal.dataobject.config;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

/**
 * FMS 财务指标 DO（账套级主数据，租户表）
 *
 * @author 智云
 */
@TableName(value = "fms_finance_indicator")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FmsFinanceIndicatorDO extends TenantBaseDO {

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
     * 指标名称
     */
    private String name;
    /**
     * 指标编码
     */
    private String code;
    /**
     * 取数报表类型
     */
    private Integer type;
    /**
     * 指标公式
     */
    private String formula;
    /**
     * 展示顺序
     */
    private Integer sort;
    /**
     * 状态
     */
    private Integer status;

}
