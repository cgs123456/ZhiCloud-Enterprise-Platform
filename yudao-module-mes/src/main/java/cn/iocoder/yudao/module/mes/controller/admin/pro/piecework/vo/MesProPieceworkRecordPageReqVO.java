package cn.iocoder.yudao.module.mes.controller.admin.pro.piecework.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - MES 计件工资明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProPieceworkRecordPageReqVO extends PageParam {

    @Schema(description = "报工单编号", example = "1024")
    private Long feedbackId;

    @Schema(description = "报工用户编号", example = "100")
    private Long feedbackUserId;

    @Schema(description = "生产工单编号", example = "200")
    private Long workOrderId;

    @Schema(description = "工序编号", example = "300")
    private Long processId;

    @Schema(description = "产品物料编号", example = "400")
    private Long itemId;

    @Schema(description = "工作站编号", example = "500")
    private Long workstationId;

    @Schema(description = "所属月份（yyyyMM）", example = "202607")
    private String periodMonth;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
