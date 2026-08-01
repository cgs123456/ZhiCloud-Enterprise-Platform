package cn.iocoder.yudao.module.mes.controller.admin.md.ecn.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - MES ECN 工程变更单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesMdEcnOrderPageReqVO extends PageParam {

    @Schema(description = "ECN 单号", example = "ECN202503001")
    private String no;

    @Schema(description = "变更名称", example = "X 产品 BOM 替换物料")
    private String ecnName;

    @Schema(description = "变更类型（10 新增 BOM 20 修改 BOM 30 删除 BOM 40 替换物料）", example = "20")
    private Integer changeType;

    @Schema(description = "原 BOM 编号", example = "1")
    private Long bomId;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "申请人", example = "1")
    private Long applicantUserId;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
