package cn.zhicloud.module.crm.controller.admin.clue.vo.channel;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CRM 线索渠道 Response VO")
@Data
@ExcelIgnoreUnannotated
public class CrmClueChannelRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "渠道名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "官网渠道")
    @ExcelProperty("渠道名称")
    private String channelName;

    @Schema(description = "渠道类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("渠道类型")
    private Integer channelType;

    @Schema(description = "渠道编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "OFFICIAL")
    @ExcelProperty("渠道编码")
    private String channelCode;

    @Schema(description = "API 地址", example = "https://www.zhicloud.cn/api/clue")
    @ExcelProperty("API 地址")
    private String apiUrl;

    @Schema(description = "API 密钥", example = "123456")
    private String apiKey;

    @Schema(description = "自动分配人", example = "1024")
    @ExcelProperty("自动分配人")
    private Long autoAssignUserId;
    @Schema(description = "自动分配人名称", example = "小明")
    @ExcelProperty("自动分配人名称")
    private String autoAssignUserName;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
