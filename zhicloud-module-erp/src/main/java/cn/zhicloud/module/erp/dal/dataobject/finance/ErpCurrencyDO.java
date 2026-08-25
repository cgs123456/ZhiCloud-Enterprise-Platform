package cn.zhicloud.module.erp.dal.dataobject.finance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 币种 DO
 *
 * <p>币种主数据，支持多币种业务。
 * <ul>
 *   <li>本位币（isBase=true）：仅允许一个币种为本位币</li>
 *   <li>外币汇率通过 {@link ErpExchangeRateDO} 维护</li>
 * </ul>
 *
 * @author 智云
 */
@TableName("erp_currency")
@KeySequence("erp_currency_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpCurrencyDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 币种编码（如 CNY/USD/EUR）
     */
    private String code;
    /**
     * 币种名称
     */
    private String name;
    /**
     * 币种符号（如 ¥/$/€）
     */
    private String symbol;
    /**
     * 是否本位币
     */
    private Boolean isBase;
    /**
     * 开启状态
     *
     * 枚举 {@link cn.zhicloud.framework.common.enums.CommonStatusEnum}
     */
    private Integer enabled;
    /**
     * 备注
     */
    private String remark;

}
