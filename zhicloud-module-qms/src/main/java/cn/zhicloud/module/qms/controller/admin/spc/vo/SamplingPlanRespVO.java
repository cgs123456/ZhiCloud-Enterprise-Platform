package cn.zhicloud.module.qms.controller.admin.spc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 抽样方案查询 VO（MIL-STD-105E / GB 2828）
 *
 * @author zhicloud
 */
@Schema(description = "管理后台 - 抽样方案查询 Response VO")
@Data
public class SamplingPlanRespVO {

    @Schema(description = "批量范围下限（含）")
    private Long lotSizeFrom;

    @Schema(description = "批量范围上限（含）")
    private Long lotSizeTo;

    @Schema(description = "字码（A~R）")
    private String codeLetter;

    @Schema(description = "检验水平：S1~S4 特殊 / I / II / III 一般")
    private String inspectionLevel;

    @Schema(description = "抽样方案字码对应的样本量")
    private int sampleSize;

    @Schema(description = "AQL（接收质量限，0.065~1000）")
    private BigDecimal aql;

    @Schema(description = "正常检验 Ac（接收数）")
    private int normalAccept;

    @Schema(description = "正常检验 Re（拒收数）")
    private int normalReject;

    @Schema(description = "加严检验 Ac")
    private int tightenedAccept;

    @Schema(description = "加严检验 Re")
    private int tightenedReject;

    @Schema(description = "放宽检验 Ac")
    private int reducedAccept;

    @Schema(description = "放宽检验 Re")
    private int reducedReject;

}
