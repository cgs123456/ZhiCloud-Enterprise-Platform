package cn.iocoder.yudao.module.wms.dal.dataobject.billing;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.merchant.WmsMerchantDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * WMS 3PL 计费合同 DO
 *
 * @author 芋道源码
 */
@TableName("wms_billing_contract")
@KeySequence("wms_billing_contract_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsBillingContractDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;
    /**
     * 合同号
     */
    private String contractNo;
    /**
     * 货主编号
     *
     * 关联 {@link WmsMerchantDO#getId()}
     */
    private Long ownerId;
    /**
     * 合同名称
     */
    private String contractName;
    /**
     * 生效日期
     */
    private LocalDate startDate;
    /**
     * 失效日期
     */
    private LocalDate endDate;
    /**
     * 合同状态
     *
     * 10 生效 / 20 失效 / 30 已终止
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
