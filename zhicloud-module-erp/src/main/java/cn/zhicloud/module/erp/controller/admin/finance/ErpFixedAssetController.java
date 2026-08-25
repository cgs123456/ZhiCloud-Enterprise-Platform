package cn.zhicloud.module.erp.controller.admin.finance;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.fixedasset.ErpFixedAssetDepreciationRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.fixedasset.ErpFixedAssetPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.fixedasset.ErpFixedAssetRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.fixedasset.ErpFixedAssetSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFixedAssetDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFixedAssetDepreciationDO;
import cn.zhicloud.module.erp.enums.finance.ErpDepreciationMethodEnum;
import cn.zhicloud.module.erp.enums.finance.ErpFixedAssetStatusEnum;
import cn.zhicloud.module.erp.service.finance.ErpFixedAssetService;
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
 * ERP 固定资产 Controller（P0-14）
 *
 * <p>提供固定资产主数据 CRUD + 月度折旧计提 + 折旧记录查询。
 *
 * @author 智云
 */
@Tag(name = "管理后台 - ERP 固定资产")
@RestController
@RequestMapping("/erp/fixed-asset")
@Validated
public class ErpFixedAssetController {

    @Resource
    private ErpFixedAssetService fixedAssetService;

    // ==================== 固定资产主数据 CRUD ====================

    @PostMapping("/create")
    @Operation(summary = "创建固定资产")
    @PreAuthorize("@ss.hasPermission('erp:fixed-asset:create')")
    public CommonResult<Long> createFixedAsset(@Valid @RequestBody ErpFixedAssetSaveReqVO createReqVO) {
        return success(fixedAssetService.createFixedAsset(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新固定资产")
    @PreAuthorize("@ss.hasPermission('erp:fixed-asset:update')")
    public CommonResult<Boolean> updateFixedAsset(@Valid @RequestBody ErpFixedAssetSaveReqVO updateReqVO) {
        fixedAssetService.updateFixedAsset(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除固定资产")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:fixed-asset:delete')")
    public CommonResult<Boolean> deleteFixedAsset(@RequestParam("id") Long id) {
        fixedAssetService.deleteFixedAsset(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取固定资产")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:fixed-asset:query')")
    public CommonResult<ErpFixedAssetRespVO> getFixedAsset(@RequestParam("id") Long id) {
        ErpFixedAssetDO asset = fixedAssetService.getFixedAsset(id);
        if (asset == null) {
            return success(null);
        }
        return success(convertToRespVO(asset));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询固定资产")
    @PreAuthorize("@ss.hasPermission('erp:fixed-asset:query')")
    public CommonResult<PageResult<ErpFixedAssetRespVO>> getFixedAssetPage(@Valid ErpFixedAssetPageReqVO pageReqVO) {
        PageResult<ErpFixedAssetDO> pageResult = fixedAssetService.getFixedAssetPage(pageReqVO);
        PageResult<ErpFixedAssetRespVO> result = new PageResult<>(
                pageResult.getList() == null ? null
                        : pageResult.getList().stream().map(this::convertToRespVO).toList(),
                pageResult.getTotal());
        return success(result);
    }

    // ==================== 折旧计提 ====================

    @PostMapping("/calculate-depreciation")
    @Operation(summary = "计提月度折旧", description = "对指定资产在指定期间计提折旧，生成待审核的折旧记录")
    @PreAuthorize("@ss.hasPermission('erp:fixed-asset:depreciate')")
    public CommonResult<Long> calculateMonthlyDepreciation(
            @RequestParam("fixedAssetId") Long fixedAssetId,
            @RequestParam("periodId") Long periodId) {
        return success(fixedAssetService.calculateMonthlyDepreciation(fixedAssetId, periodId));
    }

    @PutMapping("/approve-depreciation")
    @Operation(summary = "审核折旧记录")
    @Parameter(name = "id", description = "折旧记录编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:fixed-asset:depreciate')")
    public CommonResult<Boolean> approveDepreciation(@RequestParam("id") Long id) {
        fixedAssetService.approveDepreciation(id);
        return success(true);
    }

    @GetMapping("/depreciation-list")
    @Operation(summary = "查询资产折旧记录列表")
    @Parameter(name = "fixedAssetId", description = "固定资产编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:fixed-asset:query')")
    public CommonResult<List<ErpFixedAssetDepreciationRespVO>> getDepreciationList(
            @RequestParam("fixedAssetId") Long fixedAssetId) {
        List<ErpFixedAssetDepreciationDO> list = fixedAssetService.getDepreciationListByFixedAssetId(fixedAssetId);
        List<ErpFixedAssetDepreciationRespVO> result = list == null ? new ArrayList<>()
                : list.stream().map(this::convertToDepreciationRespVO).toList();
        return success(result);
    }

    // ==================== 内部辅助方法 ====================

    private ErpFixedAssetRespVO convertToRespVO(ErpFixedAssetDO asset) {
        ErpFixedAssetRespVO respVO = BeanUtils.toBean(asset, ErpFixedAssetRespVO.class);
        if (respVO.getDepreciationMethod() != null) {
            for (ErpDepreciationMethodEnum methodEnum : ErpDepreciationMethodEnum.values()) {
                if (methodEnum.getMethod().equals(respVO.getDepreciationMethod())) {
                    respVO.setDepreciationMethodName(methodEnum.getName());
                    break;
                }
            }
        }
        if (respVO.getStatus() != null) {
            for (ErpFixedAssetStatusEnum statusEnum : ErpFixedAssetStatusEnum.values()) {
                if (statusEnum.getStatus().equals(respVO.getStatus())) {
                    respVO.setStatusName(statusEnum.getName());
                    break;
                }
            }
        }
        return respVO;
    }

    private ErpFixedAssetDepreciationRespVO convertToDepreciationRespVO(ErpFixedAssetDepreciationDO depreciation) {
        ErpFixedAssetDepreciationRespVO respVO = BeanUtils.toBean(depreciation, ErpFixedAssetDepreciationRespVO.class);
        if (respVO.getStatus() != null) {
            respVO.setStatusName(respVO.getStatus() == 20 ? "已审核" : "待审核");
        }
        return respVO;
    }

}
