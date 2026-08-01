package cn.iocoder.yudao.module.erp.dal.dataobject.finance;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.erp.enums.finance.ErpAccountBookStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpAccountingStandardEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 账簿主数据 DO（P1-多账簿）
 *
 * <p>账簿主数据，支持多会计准则并行账簿（CAS / IFRS / US_GAAP 等）。
 * <ul>
 *   <li>同一会计准则下最多一个主账簿（is_primary=true 唯一）</li>
 *   <li>凭证（{@link ErpGlVoucherDO#getAccountBookId()}）引用本表，空表示默认主账簿</li>
 *   <li>本位币引用 {@link ErpCurrencyDO#getId()}</li>
 * </ul>
 *
 * @author 芋道源码
 */
@TableName("erp_account_book")
@KeySequence("erp_account_book_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAccountBookDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 账簿编码（如 BOOK-CAS、BOOK-IFRS）
     */
    private String code;
    /**
     * 账簿名称（如 中国会计准则账簿）
     */
    private String name;
    /**
     * 会计准则
     *
     * 枚举 {@link ErpAccountingStandardEnum}
     */
    private Integer accountingStandard;
    /**
     * 本位币编号
     *
     * 关联 {@link ErpCurrencyDO#getId()}
     */
    private Long currencyId;
    /**
     * 是否主账簿（同一会计准则下最多一个主账簿）
     */
    private Boolean isPrimary;
    /**
     * 状态
     *
     * 枚举 {@link ErpAccountBookStatusEnum}
     */
    private Integer status;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 备注
     */
    private String remark;

}
