package cn.zhicloud.module.erp.controller.admin.finance;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.budget.ErpBudgetDetailRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.budget.ErpBudgetPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.budget.ErpBudgetRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.budget.ErpBudgetSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpBudgetDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpBudgetDetailDO;
import cn.zhicloud.module.erp.enums.finance.ErpBudgetStatusEnum;
import cn.zhicloud.module.erp.enums.finance.ErpBudgetTypeEnum;
import cn.zhicloud.module.erp.service.finance.ErpBudgetService;
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
 * ERP 预算 Controller（P0-14）
 *
 * <p>提供预算 CRUD + 审批。
 * 预算明细随主表一并返回（嵌套结构），减少前端请求次数。
 *
 * @author 智云
 */
@Tag(name = "管理后台 - ERP 预算")
@RestController
@RequestMapping("/erp/budget")
@Validated
public class ErpBudgetController {

    @Resource
    private ErpBudgetService budgetService;

    @PostMapping("/create")
    @Operation(summary = "创建预算", description = "校验期间唯一性、明细金额合计 = 总额，初始化状态为草稿")
    @PreAuthorize("@ss.hasPermission('erp:budget:create')")
    public CommonResult<Long> createBudget(@Valid @RequestBody ErpBudgetSaveReqVO createReqVO) {
        return success(budgetService.createBudget(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新预算", description = "仅草稿状态可更新")
    @PreAuthorize("@ss.hasPermission('erp:budget:update')")
    public CommonResult<Boolean> updateBudget(@Valid @RequestBody ErpBudgetSaveReqVO updateReqVO) {
        budgetService.updateBudget(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除预算", description = "仅草稿状态可删除，级联删除明细")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:budget:delete')")
    public CommonResult<Boolean> deleteBudget(@RequestParam("id") Long id) {
        budgetService.deleteBudget(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取预算", description = "返回预算主表 + 明细列表")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:budget:query')")
    public CommonResult<ErpBudgetRespVO> getBudget(@RequestParam("id") Long id) {
        ErpBudgetDO budget = budgetService.getBudget(id);
        if (budget == null) {
            return success(null);
        }
        ErpBudgetRespVO respVO = convertToRespVO(budget);
        // 填充明细
        List<ErpBudgetDetailDO> details = budgetService.getBudgetDetailListByBudgetId(id);
        if (details != null) {
            respVO.setDetails(details.stream()
                    .map(d -> BeanUtils.toBean(d, ErpBudgetDetailRespVO.class))
                    .toList());
        } else {
            respVO.setDetails(new ArrayList<>());
        }
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询预算")
    @PreAuthorize("@ss.hasPermission('erp:budget:query')")
    public CommonResult<PageResult<ErpBudgetRespVO>> getBudgetPage(@Valid ErpBudgetPageReqVO pageReqVO) {
        PageResult<ErpBudgetDO> pageResult = budgetService.getBudgetPage(pageReqVO);
        PageResult<ErpBudgetRespVO> result = new PageResult<>(
                pageResult.getList() == null ? null
                        : pageResult.getList().stream().map(this::convertToRespVO).toList(),
                pageResult.getTotal());
        return success(result);
    }

    @PutMapping("/approve")
    @Operation(summary = "审批预算", description = "预算状态由草稿变为已审批")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:budget:approve')")
    public CommonResult<Boolean> approveBudget(@RequestParam("id") Long id) {
        budgetService.approveBudget(id);
        return success(true);
    }

    // ==================== 内部辅助方法 ====================

    private ErpBudgetRespVO convertToRespVO(ErpBudgetDO budget) {
        ErpBudgetRespVO respVO = BeanUtils.toBean(budget, ErpBudgetRespVO.class);
        if (respVO.getBudgetType() != null) {
            for (ErpBudgetTypeEnum typeEnum : ErpBudgetTypeEnum.values()) {
                if (typeEnum.getType().equals(respVO.getBudgetType())) {
                    respVO.setBudgetTypeName(typeEnum.getName());
                    break;
                }
            }
        }
        if (respVO.getStatus() != null) {
            for (ErpBudgetStatusEnum statusEnum : ErpBudgetStatusEnum.values()) {
                if (statusEnum.getStatus().equals(respVO.getStatus())) {
                    respVO.setStatusName(statusEnum.getName());
                    break;
                }
            }
        }
        return respVO;
    }

}
