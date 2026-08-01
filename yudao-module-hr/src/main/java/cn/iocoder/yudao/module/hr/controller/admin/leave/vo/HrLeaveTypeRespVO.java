package cn.iocoder.yudao.module.hr.controller.admin.leave.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - HR 假期类型 Response VO")
@Data
public class HrLeaveTypeRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "假期类型名称", example = "年假")
    private String name;

    @Schema(description = "编码", example = "ANNUAL")
    private String code;

    @Schema(description = "是否带薪", example = "1")
    private Integer isPaid;

    @Schema(description = "是否扣薪", example = "0")
    private Integer deductSalary;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}