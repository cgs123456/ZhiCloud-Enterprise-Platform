package cn.zhicloud.module.fms.controller.admin.config.vo.accountset;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * FMS 账套关联的 ERP 账簿 Response VO
 *
 * <p>V191（台账 §12.212）：FMS/ERP 财务融合后「账套 → 账簿 → 凭证」层级打通，
 * 账套详情页据此展示「本账套挂了哪些账簿」。
 *
 * <p>刻意做成独立 VO 而非复用 ERP 的 ErpAccountBookRespVO：跨模块 VO 泄漏会让
 * ERP 侧字段变化直接改到 FMS 的接口契约，边界处独立转换（CLAUDE.md 边界纪律）。
 *
 * @author 智云
 */
@Schema(description = "用户后台 - 账套关联的 ERP 账簿")
@Data
public class FmsAccountBookRespVO {

    @Schema(description = "账簿编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "账簿编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "BOOK-CAS")
    private String code;

    @Schema(description = "账簿名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "中国会计准则账簿")
    private String name;

    @Schema(description = "会计准则（10 中国会计准则 / 20 国际会计准则）", example = "10")
    private Integer accountingStandard;

    @Schema(description = "是否主账簿", example = "true")
    private Boolean isPrimary;

    @Schema(description = "状态（0 开启 / 1 关闭）", example = "0")
    private Integer status;

}
