package cn.zhicloud.module.erp.controller.admin.purchase.evaluation;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.purchase.evaluation.vo.ErpSupplierEvaluationItemRespVO;
import cn.zhicloud.module.erp.controller.admin.purchase.evaluation.vo.ErpSupplierEvaluationPageReqVO;
import cn.zhicloud.module.erp.controller.admin.purchase.evaluation.vo.ErpSupplierEvaluationRespVO;
import cn.zhicloud.module.erp.controller.admin.purchase.evaluation.vo.ErpSupplierEvaluationSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.evaluation.ErpSupplierEvaluationDO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.evaluation.ErpSupplierEvaluationItemDO;
import cn.zhicloud.module.erp.service.purchase.ErpSupplierService;
import cn.zhicloud.module.erp.service.purchase.evaluation.ErpSupplierEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 供应商评估")
@RestController
@RequestMapping("/erp/supplier-evaluation")
@Validated
public class ErpSupplierEvaluationController {

    @Resource
    private ErpSupplierEvaluationService evaluationService;
    @Resource
    private ErpSupplierService supplierService;

    @PostMapping("/create")
    @Operation(summary = "创建供应商评估")
    @PreAuthorize("@ss.hasPermission('erp:supplier-evaluation:create')")
    public CommonResult<Long> createEvaluation(@Valid @RequestBody ErpSupplierEvaluationSaveReqVO createReqVO) {
        return success(evaluationService.createEvaluation(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商评估")
    @PreAuthorize("@ss.hasPermission('erp:supplier-evaluation:update')")
    public CommonResult<Boolean> updateEvaluation(@Valid @RequestBody ErpSupplierEvaluationSaveReqVO updateReqVO) {
        evaluationService.updateEvaluation(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商评估")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:supplier-evaluation:delete')")
    public CommonResult<Boolean> deleteEvaluation(@RequestParam("ids") List<Long> ids) {
        evaluationService.deleteEvaluation(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商评估")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:supplier-evaluation:query')")
    public CommonResult<ErpSupplierEvaluationRespVO> getEvaluation(@RequestParam("id") Long id) {
        ErpSupplierEvaluationDO evaluation = evaluationService.getEvaluation(id);
        if (evaluation == null) {
            return success(null);
        }
        ErpSupplierEvaluationRespVO respVO = BeanUtils.toBean(evaluation, ErpSupplierEvaluationRespVO.class);
        fillSupplierName(respVO, evaluation.getSupplierId());
        // 查询指标项
        List<ErpSupplierEvaluationItemDO> items = evaluationService.getEvaluationItemListByEvaluationId(id);
        respVO.setItems(BeanUtils.toBean(items, ErpSupplierEvaluationItemRespVO.class));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得供应商评估分页")
    @PreAuthorize("@ss.hasPermission('erp:supplier-evaluation:query')")
    public CommonResult<PageResult<ErpSupplierEvaluationRespVO>> getEvaluationPage(@Valid ErpSupplierEvaluationPageReqVO pageReqVO) {
        PageResult<ErpSupplierEvaluationDO> pageResult = evaluationService.getEvaluationPage(pageReqVO);
        PageResult<ErpSupplierEvaluationRespVO> respPage = BeanUtils.toBean(pageResult, ErpSupplierEvaluationRespVO.class);
        // 填充供应商名称
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(respPage.getList(), ErpSupplierEvaluationRespVO::getSupplierId));
        respPage.getList().forEach(vo -> {
            if (vo.getSupplierId() != null) {
                ErpSupplierDO supplier = supplierMap.get(vo.getSupplierId());
                if (supplier != null) {
                    vo.setSupplierName(supplier.getName());
                }
            }
        });
        return success(respPage);
    }

    @PostMapping("/calculate")
    @Operation(summary = "自动计算供应商评估")
    @PreAuthorize("@ss.hasPermission('erp:supplier-evaluation:calculate')")
    public CommonResult<Long> calculateEvaluation(@RequestParam("supplierId") Long supplierId,
                                                  @RequestParam("period") String period) {
        return success(evaluationService.calculateEvaluation(supplierId, period));
    }

    @GetMapping("/item/list")
    @Operation(summary = "获得供应商评估指标项列表")
    @Parameter(name = "evaluationId", description = "评估编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:supplier-evaluation:query')")
    public CommonResult<List<ErpSupplierEvaluationItemRespVO>> getEvaluationItemList(@RequestParam("evaluationId") Long evaluationId) {
        List<ErpSupplierEvaluationItemDO> list = evaluationService.getEvaluationItemListByEvaluationId(evaluationId);
        return success(BeanUtils.toBean(list, ErpSupplierEvaluationItemRespVO.class));
    }

    private void fillSupplierName(ErpSupplierEvaluationRespVO vo, Long supplierId) {
        if (supplierId == null) {
            return;
        }
        Map<Long, ErpSupplierDO> map = supplierService.getSupplierMap(java.util.Collections.singleton(supplierId));
        ErpSupplierDO supplier = map.get(supplierId);
        if (supplier != null) {
            vo.setSupplierName(supplier.getName());
        }
    }

}
