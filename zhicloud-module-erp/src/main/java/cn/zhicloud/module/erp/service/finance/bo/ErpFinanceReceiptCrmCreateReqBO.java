package cn.zhicloud.module.erp.service.finance.bo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 「CRM 回款 → ERP 收款单」集成建单请求 BO（D1/§12.267）
 *
 * <p>刻意不复用 {@link cn.zhicloud.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptSaveReqVO}：
 * HTTP VO 的 @NotNull（账户、财务人员、优惠金额）在服务层 {@code @Validated} 下对内部调用同样生效，
 * 而 CRM 域没有账户/财务人员维度。本 BO 只承载集成链路能负责的字段：账户不在契约内，由服务端
 * 按「租户默认账户」补齐（§12.267 D1 修订），财务人员留空、由财务在审核过账前补选。
 *
 * @author 智云集成治理
 */
@Data
@Builder
public class ErpFinanceReceiptCrmCreateReqBO {

    /** 幂等键（调用方按 CRM 回款主键推导，如 crm-receivable-{id}） */
    private String idempotencyKey;

    /** CRM 回款编号（写入明细 bizNo 与收款单备注，用于跨域追溯） */
    private String receivableNo;

    /** CRM 回款主键（写入明细 bizId） */
    private Long receivableId;

    /** ERP 客户编号（CRM 客户经 MDM 映射后的目标编号） */
    private Long customerId;

    /** 收款时间（取 CRM 回款日期） */
    private LocalDateTime receiptTime;

    /** 回款金额（明细应收=已收=本次收款） */
    private BigDecimal price;

}
