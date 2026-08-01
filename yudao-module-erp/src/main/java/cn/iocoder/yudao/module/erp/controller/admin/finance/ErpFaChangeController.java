package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.fachange.ErpFaChangePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.fachange.ErpFaChangeRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.fachange.ErpFaChangeSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFaChangeDO;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFaChangeStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFaChangeTypeEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpFaChangeService;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * ERP 固定资产变动 Controller
 *
 * <p>提供固定资产变动申请的 CRUD + 审核/驳回 + 变动历史查询。
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - ERP 固定资产变动")
@RestController
@RequestMapping("/erp/fa-change")
@Validated
public class ErpFaChangeController {

    @Resource
    private ErpFaChangeService faChangeService;

    @PostMapping("/create")
    @Operation(summary = "创建资产变动申请")
    @PreAuthorize("@ss.hasPermission('erp:fa-change:create')")
    public CommonResult<Long> createFaChange(@Valid @RequestBody ErpFaChangeSaveReqVO createReqVO) {
        return success(faChangeService.createFaChange(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新资产变动申请")
    @PreAuthorize("@ss.hasPermission('erp:fa-change:update')")
    public CommonResult<Boolean> updateFaChange(@Valid @RequestBody ErpFaChangeSaveReqVO updateReqVO) {
        faChangeService.updateFaChange(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除资产变动申请")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:fa-change:delete')")
    public CommonResult<Boolean> deleteFaChange(@RequestParam("id") Long id) {
        faChangeService.deleteFaChange(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取资产变动申请")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:fa-change:query')")
    public CommonResult<ErpFaChangeRespVO> getFaChange(@RequestParam("id") Long id) {
        ErpFaChangeDO faChange = faChangeService.getFaChange(id);
        if (faChange == null) {
            return success(null);
        }
        return success(convertToRespVO(faChange));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询资产变动申请")
    @PreAuthorize("@ss.hasPermission('erp:fa-change:query')")
    public CommonResult<PageResult<ErpFaChangeRespVO>> getFaChangePage(@Valid ErpFaChangePageReqVO pageReqVO) {
        PageResult<ErpFaChangeDO> pageResult = faChangeService.getFaChangePage(pageReqVO);
        PageResult<ErpFaChangeRespVO> result = new PageResult<>(
                pageResult.getList() == null ? null
                        : pageResult.getList().stream().map(this::convertToRespVO).toList(),
                pageResult.getTotal());
        return success(result);
    }

    @PutMapping("/approve")
    @Operation(summary = "审核通过资产变动申请", description = "审核通过后，根据变动类型实际更新固定资产对应字段")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:fa-change:approve')")
    public CommonResult<Boolean> approveFaChange(@RequestParam("id") Long id) {
        faChangeService.approveFaChange(id);
        return success(true);
    }

    @PutMapping("/reject")
    @Operation(summary = "驳回资产变动申请")
    @PreAuthorize("@ss.hasPermission('erp:fa-change:approve')")
    public CommonResult<Boolean> rejectFaChange(@RequestParam("id") Long id,
                                                 @RequestParam("reason") String reason) {
        faChangeService.rejectFaChange(id, reason);
        return success(true);
    }

    @GetMapping("/list-by-asset")
    @Operation(summary = "查询某资产的变动历史")
    @Parameter(name = "assetId", description = "固定资产编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:fa-change:query')")
    public CommonResult<List<ErpFaChangeRespVO>> listByAssetId(@RequestParam("assetId") Long assetId) {
        List<ErpFaChangeDO> list = faChangeService.listByAssetId(assetId);
        List<ErpFaChangeRespVO> result = list == null ? new ArrayList<>()
                : list.stream().map(this::convertToRespVO).toList();
        return success(result);
    }

    // ==================== 内部辅助方法 ====================

    private ErpFaChangeRespVO convertToRespVO(ErpFaChangeDO faChange) {
        ErpFaChangeRespVO respVO = BeanUtils.toBean(faChange, ErpFaChangeRespVO.class);
        if (respVO.getChangeType() != null) {
            for (ErpFaChangeTypeEnum typeEnum : ErpFaChangeTypeEnum.values()) {
                if (typeEnum.getType().equals(respVO.getChangeType())) {
                    respVO.setChangeTypeName(typeEnum.getName());
                    break;
                }
            }
        }
        if (respVO.getStatus() != null) {
            for (ErpFaChangeStatusEnum statusEnum : ErpFaChangeStatusEnum.values()) {
                if (statusEnum.getStatus().equals(respVO.getStatus())) {
                    respVO.setStatusName(statusEnum.getName());
                    break;
                }
            }
        }
        return respVO;
    }

}
