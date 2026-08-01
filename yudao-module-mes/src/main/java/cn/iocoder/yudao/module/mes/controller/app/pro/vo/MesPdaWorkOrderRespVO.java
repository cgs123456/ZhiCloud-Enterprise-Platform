package cn.iocoder.yudao.module.mes.controller.app.pro.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "用户 APP - PDA 工单扫描 Response VO")
@Data
public class MesPdaWorkOrderRespVO {

    @Schema(description = "工单编号", example = "1")
    private Long id;

    @Schema(description = "工单编码", example = "WO202503001")
    private String workOrderNo;

    @Schema(description = "工单名称", example = "生产 A 产品 1000 件")
    private String workOrderName;

    @Schema(description = "产品编号", example = "100")
    private Long productId;

    @Schema(description = "产品编码", example = "P001")
    private String productCode;

    @Schema(description = "产品名称", example = "成品 A")
    private String productName;

    @Schema(description = "生产数量", example = "1000.00")
    private BigDecimal quantity;

    @Schema(description = "已生产数量", example = "500.00")
    private BigDecimal quantityProduced;

    @Schema(description = "工单状态", example = "5")
    private Integer status;

    @Schema(description = "工单状态名称", example = "报工中")
    private String statusName;

    @Schema(description = "需求日期")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestDate;

    @Schema(description = "工艺路线编号", example = "1")
    private Long routeId;

    @Schema(description = "工艺路线编码", example = "RT001")
    private String routeCode;

    @Schema(description = "工艺路线名称", example = "工艺路线 A")
    private String routeName;

    @Schema(description = "当前工序列表")
    private List<ProcessInfo> processes;

    @Schema(description = "扫描时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scanTime;

    /**
     * 工序信息
     */
    @Data
    public static class ProcessInfo {
        @Schema(description = "工序编号", example = "1")
        private Long processId;
        @Schema(description = "工序编码", example = "GX001")
        private String processCode;
        @Schema(description = "工序名称", example = "加工")
        private String processName;
        @Schema(description = "工序顺序", example = "1")
        private Integer orderNum;
        @Schema(description = "是否关键工序", example = "true")
        private Boolean keyFlag;
        @Schema(description = "是否需要检验", example = "false")
        private Boolean checkFlag;
    }

}
