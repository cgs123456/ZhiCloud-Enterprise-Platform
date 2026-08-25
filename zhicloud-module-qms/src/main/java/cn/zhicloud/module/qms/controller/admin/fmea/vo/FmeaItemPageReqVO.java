package cn.zhicloud.module.qms.controller.admin.fmea.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - QMS FMEA 条目分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FmeaItemPageReqVO extends PageParam {

    @Schema(description = "FMEA 文档 ID", example = "1024")
    private Long fmeaId;

}
