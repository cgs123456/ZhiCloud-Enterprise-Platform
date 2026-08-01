package cn.iocoder.yudao.module.erp.controller.admin.collaboration.cpfr.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP CPFR 异常分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpCpfrExceptionPageReqVO extends PageParam {

    @Schema(description = "预测编号", example = "1024")
    private Long forecastId;

    @Schema(description = "异常类型（10 预测偏差超限 / 20 库存异常 / 30 补货异常）", example = "10")
    private Integer exceptionType;

    @Schema(description = "处理状态（10 待处理 / 20 处理中 / 30 已解决）", example = "10")
    private Integer handlingStatus;

}
