package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationScopePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationScopeRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationScopeSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpConsolidationScopeDO;
import cn.iocoder.yudao.module.erp.enums.finance.ErpConsolidationMethodEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpConsolidationScopeStatusEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpConsolidationScopeService;
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
 * ERP 合并范围 Controller（P1-合并报表引擎）
 *
 * <p>提供合并范围 CRUD 接口，用于维护母子公司持股关系。
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - ERP 合并范围")
@RestController
@RequestMapping("/erp/consolidation-scope")
@Validated
public class ErpConsolidationScopeController {

    @Resource
    private ErpConsolidationScopeService consolidationScopeService;

    @PostMapping("/create")
    @Operation(summary = "创建合并范围")
    @PreAuthorize("@ss.hasPermission('erp:consolidation-scope:create')")
    public CommonResult<Long> createScope(@Valid @RequestBody ErpConsolidationScopeSaveReqVO createReqVO) {
        return success(consolidationScopeService.createScope(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新合并范围")
    @PreAuthorize("@ss.hasPermission('erp:consolidation-scope:update')")
    public CommonResult<Boolean> updateScope(@Valid @RequestBody ErpConsolidationScopeSaveReqVO updateReqVO) {
        consolidationScopeService.updateScope(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除合并范围")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:consolidation-scope:delete')")
    public CommonResult<Boolean> deleteScope(@RequestParam("id") Long id) {
        consolidationScopeService.deleteScope(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取合并范围")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:consolidation-scope:query')")
    public CommonResult<ErpConsolidationScopeRespVO> getScope(@RequestParam("id") Long id) {
        ErpConsolidationScopeDO scope = consolidationScopeService.getScope(id);
        if (scope == null) {
            return success(null);
        }
        return success(convertToRespVO(scope));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询合并范围")
    @PreAuthorize("@ss.hasPermission('erp:consolidation-scope:query')")
    public CommonResult<PageResult<ErpConsolidationScopeRespVO>> getScopePage(
            @Valid ErpConsolidationScopePageReqVO pageReqVO) {
        PageResult<ErpConsolidationScopeDO> pageResult = consolidationScopeService.getScopePage(pageReqVO);
        PageResult<ErpConsolidationScopeRespVO> result = new PageResult<>(
                pageResult.getList() == null ? new ArrayList<>()
                        : pageResult.getList().stream().map(this::convertToRespVO).toList(),
                pageResult.getTotal());
        return success(result);
    }

    // ==================== 内部辅助方法 ====================

    private ErpConsolidationScopeRespVO convertToRespVO(ErpConsolidationScopeDO scope) {
        ErpConsolidationScopeRespVO respVO = BeanUtils.toBean(scope, ErpConsolidationScopeRespVO.class);
        if (respVO.getConsolidationMethod() != null) {
            for (ErpConsolidationMethodEnum methodEnum : ErpConsolidationMethodEnum.values()) {
                if (methodEnum.getMethod().equals(respVO.getConsolidationMethod())) {
                    respVO.setConsolidationMethodName(methodEnum.getName());
                    break;
                }
            }
        }
        if (respVO.getStatus() != null) {
            for (ErpConsolidationScopeStatusEnum statusEnum : ErpConsolidationScopeStatusEnum.values()) {
                if (statusEnum.getStatus().equals(respVO.getStatus())) {
                    respVO.setStatusName(statusEnum.getName());
                    break;
                }
            }
        }
        return respVO;
    }

}
