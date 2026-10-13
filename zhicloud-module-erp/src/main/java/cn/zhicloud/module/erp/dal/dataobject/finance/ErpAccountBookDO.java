package cn.zhicloud.module.erp.dal.dataobject.finance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import cn.zhicloud.module.erp.enums.finance.ErpAccountBookStatusEnum;
import cn.zhicloud.module.erp.enums.finance.ErpAccountingStandardEnum;
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
 * @author 智云
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
     * FMS 账套编号（可空；挂接后凭证可按账套维度汇总，见 V191）
     *
     * <p>刻意可空且不加外键：FMS 与 ERP 是独立 Modulith 模块，跨模块 FK 会让
     * Flyway 建表顺序耦合两模块；存量账簿为 NULL，ERP 已验证的会计事件链零改动。
     */
    private Long accountSetId;
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
