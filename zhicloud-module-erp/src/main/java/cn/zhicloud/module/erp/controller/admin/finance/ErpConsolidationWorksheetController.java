package cn.zhicloud.module.erp.controller.admin.finance;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationWorksheetPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationWorksheetRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationWorksheetSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpConsolidationWorksheetDO;
import cn.zhicloud.module.erp.enums.finance.ErpConsolidationEliminationTypeEnum;
import cn.zhicloud.module.erp.enums.finance.ErpWorksheetStatusEnum;
import cn.zhicloud.module.erp.service.finance.ErpConsolidationWorksheetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * ERP 合并工作底稿 Controller（P1-合并报表引擎）
 *
 * <p>提供工作底稿 CRUD + 审核 / 驳回接口。
 *
 * @author 智云
 */
@Tag(name = "管理后台 - ERP 合并工作底稿")
@RestController
@RequestMapping("/erp/consolidation-worksheet")
@Validated
public class ErpConsolidationWorksheetController {

    @Resource
    private ErpConsolidationWorksheetService consolidationWorksheetService;

    @PostMapping("/create")
    @Operation(summary = "创建合并工作底稿")
    @PreAuthorize("@ss.hasPermission('erp:consolidation-worksheet:create')")
    public CommonResult<Long> createWorksheet(@Valid @RequestBody ErpConsolidationWorksheetSaveReqVO createReqVO) {
        return success(consolidationWorksheetService.createWorksheet(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新合并工作底稿")
    @PreAuthorize("@ss.hasPermission('erp:consolidation-worksheet:update')")
    public CommonResult<Boolean> updateWorksheet(@Valid @RequestBody ErpConsolidationWorksheetSaveReqVO updateReqVO) {
        consolidationWorksheetService.updateWorksheet(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除合并工作底稿")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:consolidation-worksheet:delete')")
    public CommonResult<Boolean> deleteWorksheet(@RequestParam("id") Long id) {
        consolidationWorksheetService.deleteWorksheet(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取合并工作底稿")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:consolidation-worksheet:query')")
    public CommonResult<ErpConsolidationWorksheetRespVO> getWorksheet(@RequestParam("id") Long id) {
        ErpConsolidationWorksheetDO worksheet = consolidationWorksheetService.getWorksheet(id);
        if (worksheet == null) {
            return success(null);
        }
        return success(convertToRespVO(worksheet));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询合并工作底稿")
    @PreAuthorize("@ss.hasPermission('erp:consolidation-worksheet:query')")
    public CommonResult<PageResult<ErpConsolidationWorksheetRespVO>> getWorksheetPage(
            @Valid ErpConsolidationWorksheetPageReqVO pageReqVO) {
        PageResult<ErpConsolidationWorksheetDO> pageResult = consolidationWorksheetService.getWorksheetPage(pageReqVO);
        PageResult<ErpConsolidationWorksheetRespVO> result = new PageResult<>(
                pageResult.getList() == null ? new ArrayList<>()
                        : pageResult.getList().stream().map(this::convertToRespVO).toList(),
                pageResult.getTotal());
        return success(result);
    }

    @PutMapping("/approve")
    @Operation(summary = "审核合并工作底稿")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:consolidation-worksheet:approve')")
    public CommonResult<Boolean> approveWorksheet(@RequestParam("id") Long id) {
        consolidationWorksheetService.approveWorksheet(id);
        return success(true);
    }

    @PutMapping("/reject")
    @Operation(summary = "驳回合并工作底稿")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:consolidation-worksheet:approve')")
    public CommonResult<Boolean> rejectWorksheet(@RequestParam("id") Long id) {
        consolidationWorksheetService.rejectWorksheet(id);
        return success(true);
    }

    // ==================== 内部辅助方法 ====================

    private ErpConsolidationWorksheetRespVO convertToRespVO(ErpConsolidationWorksheetDO worksheet) {
        ErpConsolidationWorksheetRespVO respVO = BeanUtils.toBean(worksheet, ErpConsolidationWorksheetRespVO.class);
        if (respVO.getEliminationType() != null) {
            for (ErpConsolidationEliminationTypeEnum typeEnum : ErpConsolidationEliminationTypeEnum.values()) {
                if (typeEnum.getType().equals(respVO.getEliminationType())) {
                    respVO.setEliminationTypeName(typeEnum.getName());
                    break;
                }
            }
        }
        if (respVO.getStatus() != null) {
            for (ErpWorksheetStatusEnum statusEnum : ErpWorksheetStatusEnum.values()) {
                if (statusEnum.getStatus().equals(respVO.getStatus())) {
                    respVO.setStatusName(statusEnum.getName());
                    break;
                }
            }
        }
        return respVO;
    }

}
