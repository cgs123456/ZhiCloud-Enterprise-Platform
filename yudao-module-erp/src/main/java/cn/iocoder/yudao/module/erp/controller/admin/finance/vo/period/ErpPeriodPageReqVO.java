package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.period;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * ERP 会计期间分页 Request VO（P0-6）
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - ERP 会计期间分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPeriodPageReqVO extends PageParam {

    @Schema(description = "年度", example = "2026")
    private Integer year;

    @Schema(description = "期间编码", example = "202607")
    private String code;

    @Schema(description = "状态", example = "10")
    private Integer status;

}
