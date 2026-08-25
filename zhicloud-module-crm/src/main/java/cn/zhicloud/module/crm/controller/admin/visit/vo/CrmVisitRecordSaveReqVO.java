package cn.zhicloud.module.crm.controller.admin.visit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - CRM 拜访签到记录创建/更新 Request VO")
@Data
public class CrmVisitRecordSaveReqVO {

    @Schema(description = "编号", example = "25787")
    private Long id;

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "客户编号不能为空")
    private Long customerId;

    @Schema(description = "联系人编号", example = "2")
    private Long contactId;

    @Schema(description = "负责人的用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "负责人编号不能为空")
    private Long ownerUserId;

    @Schema(description = "拜访时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-02-02 10:00:00")
    @NotNull(message = "拜访时间不能为空")
    private LocalDateTime visitTime;

    @Schema(description = "签到时间", example = "2024-02-02 10:00:00")
    private LocalDateTime signInTime;

    @Schema(description = "签到纬度", example = "30.2741000")
    private BigDecimal signInLatitude;

    @Schema(description = "签到经度", example = "120.1551000")
    private BigDecimal signInLongitude;

    @Schema(description = "签到地址", example = "浙江省杭州市西湖区")
    private String signInAddress;

    @Schema(description = "拜访类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "拜访类型不能为空")
    private Integer visitType;

    @Schema(description = "拜访内容", example = "沟通合作事宜")
    private String content;

    @Schema(description = "图片 URL 列表", example = "[\"https://www.zhicloud.cn/1.png\"]")
    private List<String> picUrls;

    @Schema(description = "附件 URL 列表", example = "[\"https://www.zhicloud.cn/1.pdf\"]")
    private List<String> fileUrls;

}
