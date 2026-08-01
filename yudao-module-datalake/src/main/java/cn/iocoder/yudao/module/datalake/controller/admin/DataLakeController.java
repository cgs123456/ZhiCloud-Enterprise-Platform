package cn.iocoder.yudao.module.datalake.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.datalake.service.DataArchivalService;
import cn.iocoder.yudao.module.datalake.service.IcebergCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 数据湖仓管理接口
 *
 * <p>提供数据湖表的查询、归档状态查询、归档触发等管理接口。
 * 仅当 {@code yudao.datalake.enabled=true} 且 {@link IcebergCatalogService} Bean 存在时加载。
 *
 * <h3>接口列表</h3>
 * <ul>
 *   <li>GET /datalake/namespaces：列出所有命名空间</li>
 *   <li>GET /datalake/tables：列出指定命名空间的表</li>
 *   <li>GET /datalake/table-schema：获取表结构</li>
 *   <li>GET /datalake/archive-status：查询归档状态</li>
 *   <li>POST /datalake/archive：触发归档（占位实现）</li>
 * </ul>
 *
 * @author yudao
 */
@Tag(name = "管理后台 - 数据湖仓")
@RestController
@RequestMapping("/datalake")
@Validated
@ConditionalOnBean(IcebergCatalogService.class)
@Slf4j
public class DataLakeController {

    @Resource
    private IcebergCatalogService icebergCatalogService;

    @Resource
    private DataArchivalService dataArchivalService;

    @GetMapping("/namespaces")
    @Operation(summary = "列出所有命名空间")
    @PreAuthorize("@ss.hasPermission('datalake:query')")
    public CommonResult<List<String>> listNamespaces() {
        return success(icebergCatalogService.listNamespaces());
    }

    @GetMapping("/tables")
    @Operation(summary = "列出指定命名空间的表")
    @Parameter(name = "namespace", description = "命名空间名称", required = true, example = "ods")
    @PreAuthorize("@ss.hasPermission('datalake:query')")
    public CommonResult<List<String>> listTables(
            @RequestParam("namespace") @NotBlank String namespace) {
        return success(icebergCatalogService.listTables(namespace));
    }

    @GetMapping("/table-schema")
    @Operation(summary = "获取数据湖表结构")
    @PreAuthorize("@ss.hasPermission('datalake:query')")
    public CommonResult<Map<String, String>> getTableSchema(
            @RequestParam("namespace") @NotBlank String namespace,
            @RequestParam("table") @NotBlank String table) {
        return success(icebergCatalogService.getTableSchema(namespace, table));
    }

    @GetMapping("/archive-status")
    @Operation(summary = "查询归档状态")
    @Parameter(name = "tableName", description = "业务表名", required = true, example = "mes_pro_work_order")
    @PreAuthorize("@ss.hasPermission('datalake:query')")
    public CommonResult<Map<String, Object>> getArchiveStatus(
            @RequestParam("tableName") @NotBlank String tableName) {
        return success(dataArchivalService.getArchiveStatus(tableName));
    }

    @PostMapping("/archive")
    @Operation(summary = "触发归档（占位实现，实际由 Flink CDC / 批量 ETL 完成）")
    @PreAuthorize("@ss.hasPermission('datalake:archive')")
    public CommonResult<String> archiveTable(
            @RequestParam("tableName") @NotBlank String tableName,
            @RequestParam("beforeDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate beforeDate) {
        String taskId = dataArchivalService.archiveTable(tableName, beforeDate);
        return success(taskId);
    }

}
