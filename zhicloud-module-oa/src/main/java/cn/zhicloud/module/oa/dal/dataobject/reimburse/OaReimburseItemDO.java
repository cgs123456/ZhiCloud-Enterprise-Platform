package cn.zhicloud.module.oa.dal.dataobject.reimburse;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * OA 报销明细 DO
 *
 * @author zhicloud
 */
@TableName("oa_reimburse_item")
@KeySequence("oa_reimburse_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OaReimburseItemDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 报销单 ID
     */
    private Long reimburseId;
    /**
     * 科目（如机票/住宿/餐饮）
     */
    private String subject;
    /**
     * 发生日期
     */
    private LocalDate occurrenceDate;
    /**
     * 金额
     */
    private BigDecimal amount;
    /**
     * 发票张数
     */
    private Integer invoiceCount;
    /**
     * 说明
     */
    private String description;

}
