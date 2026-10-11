package cn.zhicloud.module.hr.controller.admin.hrm;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.system.api.logger.OperateLogApi;
import cn.zhicloud.module.system.api.logger.dto.OperateLogPageReqDTO;
import cn.zhicloud.module.system.api.logger.dto.OperateLogRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * HRM 操作日志 Controller（HRM 详情页的操作日志页签；复用 system 模块 OperateLogApi）
 *
 * bizType → type 映射（对齐前端 HrmBizType 常量）：1招聘职位 2招聘候选人 3员工档案 4绩效考核 5绩效计划。
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - HRM 操作日志")
@RestController
@RequestMapping("/hrm/operate-log")
@Validated
public class HrmOperateLogController {

    /** bizType → OperateLog.type（大模块类型）映射 */
    private static final Map<Integer, String> BIZ_TYPE_MAP = Map.of(
            1, "hrm_recruit_post",
            2, "hrm_recruit_candidate",
            3, "hrm_employee",
            4, "hrm_performance_assessment",
            5, "hrm_performance_plan");

    @Resource
    private OperateLogApi operateLogApi;

    @GetMapping("/page")
    @Operation(summary = "获得 HRM 操作日志分页")
    @Parameter(name = "bizType", description = "业务类型：1招聘职位 2招聘候选人 3员工档案 4绩效考核 5绩效计划", required = true)
    @Parameter(name = "bizId", description = "业务编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:operate-log:query')")
    public CommonResult<PageResult<OperateLogRespDTO>> getOperateLogPage(
            @RequestParam("bizType") @NotNull Integer bizType,
            @RequestParam("bizId") @NotNull Long bizId,
            @RequestParam(value = "pageNo", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        OperateLogPageReqDTO reqDTO = new OperateLogPageReqDTO();
        reqDTO.setType(BIZ_TYPE_MAP.get(bizType));
        reqDTO.setBizId(bizId);
        reqDTO.setPageNo(pageNo != null ? pageNo : 1);
        reqDTO.setPageSize(pageSize != null ? pageSize : 10);
        return success(operateLogApi.getOperateLogPage(reqDTO));
    }

}
