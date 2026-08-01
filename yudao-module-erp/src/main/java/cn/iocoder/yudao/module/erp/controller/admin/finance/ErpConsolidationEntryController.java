package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationEntryPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationEntryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationEntrySaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpConsolidationEntryDO;
import cn.iocoder.yudao.module.erp.enums.finance.ErpConsolidationEliminationTypeEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpConsolidationEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * ERP 合并报表抵消分录 Controller（P0-14）
 *
 * <p>提供集团内关联交易抵消分录的 CRUD + 审核接口。
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - ERP 合并报表抵消分录")
@RestController
@RequestMapping("/erp/consolidation-entry")
@Validated
public class ErpConsolidationEntryController {

    /**
     * 状态：草稿
     */
    private static final Integer STATUS_DRAFT = 10;
    /**
     * 状态：已审核
     */
    private static final Integer STATUS_APPROVED = 20;

    @Resource
    private ErpConsolidationEntryService consolidationEntryService;

    @PostMapping("/create")
    @Operation(summary = "创建合并报表抵消分录")
    @PreAuthorize("@ss.hasPermission('erp:consolidation-entry:create')")
    public CommonResult<Long> createConsolidationEntry(@Valid @RequestBody ErpConsolidationEntrySaveReqVO createReqVO) {
        return success(consolidationEntryService.createConsolidationEntry(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新合并报表抵消分录")
    @PreAuthorize("@ss.hasPermission('erp:consolidation-entry:update')")
    public CommonResult<Boolean> updateConsolidationEntry(@Valid @RequestBody ErpConsolidationEntrySaveReqVO updateReqVO) {
        consolidationEntryService.updateConsolidationEntry(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除合并报表抵消分录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:consolidation-entry:delete')")
    public CommonResult<Boolean> deleteConsolidationEntry(@RequestParam("id") Long id) {
        consolidationEntryService.deleteConsolidationEntry(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取合并报表抵消分录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:consolidation-entry:query')")
    public CommonResult<ErpConsolidationEntryRespVO> getConsolidationEntry(@RequestParam("id") Long id) {
        ErpConsolidationEntryDO entry = consolidationEntryService.getConsolidationEntry(id);
        if (entry == null) {
            return success(null);
        }
        return success(convertToRespVO(entry));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询合并报表抵消分录")
    @PreAuthorize("@ss.hasPermission('erp:consolidation-entry:query')")
    public CommonResult<PageResult<ErpConsolidationEntryRespVO>> getConsolidationEntryPage(
            @Valid ErpConsolidationEntryPageReqVO pageReqVO) {
        PageResult<ErpConsolidationEntryDO> pageResult = consolidationEntryService.getConsolidationEntryPage(pageReqVO);
        PageResult<ErpConsolidationEntryRespVO> result = new PageResult<>(
                pageResult.getList() == null ? new ArrayList<>()
                        : pageResult.getList().stream().map(this::convertToRespVO).toList(),
                pageResult.getTotal());
        return success(result);
    }

    @PutMapping("/approve")
    @Operation(summary = "审核合并报表抵消分录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:consolidation-entry:approve')")
    public CommonResult<Boolean> approveConsolidationEntry(@RequestParam("id") Long id) {
        consolidationEntryService.approveConsolidationEntry(id);
        return success(true);
    }

    // ==================== 内部辅助方法 ====================

    private ErpConsolidationEntryRespVO convertToRespVO(ErpConsolidationEntryDO entry) {
        ErpConsolidationEntryRespVO respVO = BeanUtils.toBean(entry, ErpConsolidationEntryRespVO.class);
        // 翻译抵消类型名称
        if (respVO.getEliminationType() != null) {
            for (ErpConsolidationEliminationTypeEnum typeEnum : ErpConsolidationEliminationTypeEnum.values()) {
                if (typeEnum.getType().equals(respVO.getEliminationType())) {
                    respVO.setEliminationTypeName(typeEnum.getName());
                    break;
                }
            }
        }
        // 翻译状态名称
        if (respVO.getStatus() != null) {
            if (STATUS_DRAFT.equals(respVO.getStatus())) {
                respVO.setStatusName("草稿");
            } else if (STATUS_APPROVED.equals(respVO.getStatus())) {
                respVO.setStatusName("已审核");
            }
        }
        return respVO;
    }

}
