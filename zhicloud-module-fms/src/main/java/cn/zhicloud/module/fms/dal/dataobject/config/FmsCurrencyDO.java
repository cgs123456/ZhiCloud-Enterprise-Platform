package cn.zhicloud.module.fms.dal.dataobject.config;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;
import java.math.BigDecimal;

/**
 * FMS 币别 DO（账套级主数据，租户表）
 *
 * @author 智云
 */
@TableName(value = "fms_currency")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FmsCurrencyDO extends TenantBaseDO {

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
     * 币别编码
     */
    private String code;
    /**
     * 币别名称
     */
    private String name;
    /**
     * 汇率
     */
    private BigDecimal exchangeRate;
    /**
     * 是否本位币
     */
    private Boolean standard;

}
