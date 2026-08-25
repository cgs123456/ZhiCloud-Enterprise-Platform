package cn.zhicloud.module.system.controller.admin.logger;

import cn.zhicloud.framework.apilog.core.annotation.ApiAccessLog;
import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageParam;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.excel.core.util.ExcelUtils;
import cn.zhicloud.framework.translate.core.TranslateUtils;
import cn.zhicloud.module.system.controller.admin.logger.vo.operatelog.OperateLogHashVerifyRespVO;
import cn.zhicloud.module.system.controller.admin.logger.vo.operatelog.OperateLogPageReqVO;
import cn.zhicloud.module.system.controller.admin.logger.vo.operatelog.OperateLogRespVO;
import cn.zhicloud.module.system.dal.dataobject.logger.OperateLogDO;
import cn.zhicloud.module.system.service.logger.OperateLogService;
import org.dromara.core.trans.anno.TransMethodResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

import static cn.zhicloud.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 操作日志")
@RestController
@RequestMapping("/system/operate-log")
@Validated
public class OperateLogController {

    @Resource
    private OperateLogService operateLogService;

    @GetMapping("/get")
    @Operation(summary = "查看操作日志")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:operate-log:query')")
    public CommonResult<OperateLogRespVO> getOperateLog(@RequestParam("id") Long id) {
        OperateLogDO operateLog = operateLogService.getOperateLog(id);
        return success(BeanUtils.toBean(operateLog, OperateLogRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "查看操作日志分页列表")
    @PreAuthorize("@ss.hasPermission('system:operate-log:query')")
    @TransMethodResult
    public CommonResult<PageResult<OperateLogRespVO>> pageOperateLog(@Valid OperateLogPageReqVO pageReqVO) {
        PageResult<OperateLogDO> pageResult = operateLogService.getOperateLogPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, OperateLogRespVO.class));
    }

    @Operation(summary = "导出操作日志")
    @GetMapping("/export-excel")
    @PreAuthorize("@ss.hasPermission('system:operate-log:export')")
    @TransMethodResult
    @ApiAccessLog(operateType = EXPORT)
    public void exportOperateLog(HttpServletResponse response, @Valid OperateLogPageReqVO exportReqVO) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<OperateLogDO> list = operateLogService.getOperateLogPage(exportReqVO).getList();
        ExcelUtils.write(response, "操作日志.xls", "数据列表", OperateLogRespVO.class,
                TranslateUtils.translate(BeanUtils.toBean(list, OperateLogRespVO.class)));
    }

    @GetMapping("/verify-hash")
    @Operation(summary = "验证操作日志 Hash 链完整性", description = "从指定 ID 开始，按 ID 升序重新计算 hash 并验证完整性，返回是否被篡改")
    @Parameter(name = "startId", description = "起始日志 ID（包含），为空则从第一条日志开始", example = "1024")
    @Parameter(name = "limit", description = "本次验证的最大日志条数，默认 1000", example = "1000")
    @PreAuthorize("@ss.hasPermission('system:operate-log:query')")
    public CommonResult<OperateLogHashVerifyRespVO> verifyHash(
            @RequestParam(value = "startId", required = false) Long startId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return success(operateLogService.verifyHashChain(startId, limit));
    }

}
