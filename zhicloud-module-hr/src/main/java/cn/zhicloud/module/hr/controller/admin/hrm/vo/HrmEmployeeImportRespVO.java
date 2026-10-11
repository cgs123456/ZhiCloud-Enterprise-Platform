package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - HRM 员工档案导入结果 Response VO")
@Data
public class HrmEmployeeImportRespVO {

    @Schema(description = "新增成功的工号")
    private List<String> createJobNumbers;

    @Schema(description = "更新成功的工号")
    private List<String> updateJobNumbers;

    @Schema(description = "跳过的工号")
    private List<String> skipJobNumbers;

    @Schema(description = "导入失败的工号及原因")
    private Map<String, String> failureJobNumbers;

}
