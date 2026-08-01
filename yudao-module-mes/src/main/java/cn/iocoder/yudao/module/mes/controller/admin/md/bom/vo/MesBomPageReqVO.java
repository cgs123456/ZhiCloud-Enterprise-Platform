package cn.iocoder.yudao.module.mes.controller.admin.md.bom.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - MES BOM 分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesBomPageReqVO extends PageParam {

    @Schema(description = "BOM 编号", example = "BOM001")
    private String bomNo;

    @Schema(description = "产品编号", example = "1024")
    private Long productId;

    @Schema(description = "BOM 类型", example = "MANUFACTURING")
    private String bomType;

    @Schema(description = "版本号", example = "V1.0")
    private String version;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}