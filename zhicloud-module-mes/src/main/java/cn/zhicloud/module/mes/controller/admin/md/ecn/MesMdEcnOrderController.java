package cn.zhicloud.module.mes.controller.admin.md.ecn;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.mes.controller.admin.md.bom.vo.MesBomRespVO;
import cn.zhicloud.module.mes.controller.admin.md.ecn.vo.MesMdEcnOrderItemRespVO;
import cn.zhicloud.module.mes.controller.admin.md.ecn.vo.MesMdEcnOrderPageReqVO;
import cn.zhicloud.module.mes.controller.admin.md.ecn.vo.MesMdEcnOrderRespVO;
import cn.zhicloud.module.mes.controller.admin.md.ecn.vo.MesMdEcnOrderSaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.md.bom.MesBomDO;
import cn.zhicloud.module.mes.dal.dataobject.md.ecn.MesMdEcnOrderDO;
import cn.zhicloud.module.mes.dal.dataobject.md.ecn.MesMdEcnOrderItemDO;
import cn.zhicloud.module.mes.dal.mysql.md.ecn.MesMdEcnOrderItemMapper;
import cn.zhicloud.module.mes.service.md.bom.MesBomService;
import cn.zhicloud.module.mes.service.md.ecn.MesMdEcnOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES ECN 工程变更单")
@RestController
@RequestMapping("/mes/md-ecn-order")
@Validated
public class MesMdEcnOrderController {

    @Resource
    private MesMdEcnOrderService ecnOrderService;
    @Resource
    private MesMdEcnOrderItemMapper ecnOrderItemMapper;
    @Resource
    private MesBomService bomService;

    @PostMapping("/create")
    @Operation(summary = "创建 ECN 工程变更单")
    @PreAuthorize("@ss.hasPermission('mes:md-ecn-order:create')")
    public CommonResult<Long> createEcnOrder(@Valid @RequestBody MesMdEcnOrderSaveReqVO createReqVO) {
        return success(ecnOrderService.createEcnOrder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 ECN 工程变更单")
    @PreAuthorize("@ss.hasPermission('mes:md-ecn-order:update')")
    public CommonResult<Boolean> updateEcnOrder(@Valid @RequestBody MesMdEcnOrderSaveReqVO updateReqVO) {
        ecnOrderService.updateEcnOrder(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 ECN 工程变更单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:md-ecn-order:delete')")
    public CommonResult<Boolean> deleteEcnOrder(@RequestParam("id") Long id) {
        ecnOrderService.deleteEcnOrder(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 ECN 工程变更单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:md-ecn-order:query')")
    public CommonResult<MesMdEcnOrderRespVO> getEcnOrder(@RequestParam("id") Long id) {
        MesMdEcnOrderDO ecnOrder = ecnOrderService.getEcnOrder(id);
        if (ecnOrder == null) {
            return success(null);
        }
        return success(buildEcnOrderRespVO(ecnOrder));
    }

    @GetMapping("/page")
    @Operation(summary = "获得 ECN 工程变更单分页")
    @PreAuthorize("@ss.hasPermission('mes:md-ecn-order:query')")
    public CommonResult<PageResult<MesMdEcnOrderRespVO>> getEcnOrderPage(@Valid MesMdEcnOrderPageReqVO pageReqVO) {
        PageResult<MesMdEcnOrderDO> pageResult = ecnOrderService.getEcnOrderPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MesMdEcnOrderRespVO.class));
    }

    @PutMapping("/submit")
    @Operation(summary = "提交审核")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:md-ecn-order:update')")
    public CommonResult<Boolean> submitEcnOrder(@RequestParam("id") Long id) {
        ecnOrderService.submitEcnOrder(id);
        return success(true);
    }

    @PutMapping("/approve")
    @Operation(summary = "审核 ECN 单")
    @PreAuthorize("@ss.hasPermission('mes:md-ecn-order:approve')")
    public CommonResult<Boolean> approveEcnOrder(@RequestParam("id") Long id,
                                                  @RequestParam("approved") Boolean approved,
                                                  @RequestParam("approveUserId") Long approveUserId) {
        ecnOrderService.approveEcnOrder(id, approved, approveUserId);
        return success(true);
    }

    @PutMapping("/execute")
    @Operation(summary = "执行变更")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:md-ecn-order:execute')")
    public CommonResult<Boolean> executeEcnOrder(@RequestParam("id") Long id) {
        ecnOrderService.executeEcnOrder(id);
        return success(true);
    }

    // ==================== 拼接 VO ====================

    private MesMdEcnOrderRespVO buildEcnOrderRespVO(MesMdEcnOrderDO ecnOrder) {
        MesMdEcnOrderRespVO respVO = BeanUtils.toBean(ecnOrder, MesMdEcnOrderRespVO.class);
        // 原 BOM 编号展示
        if (ecnOrder.getBomId() != null) {
            MesBomDO bom = bomService.getBom(ecnOrder.getBomId());
            if (bom != null) {
                respVO.setBomNo(bom.getBomNo());
            }
        }
        // 新 BOM 编号展示
        if (ecnOrder.getNewBomId() != null) {
            MesBomDO newBom = bomService.getBom(ecnOrder.getNewBomId());
            if (newBom != null) {
                respVO.setNewBomNo(newBom.getBomNo());
            }
        }
        // 变更明细
        List<MesMdEcnOrderItemDO> items = ecnOrderItemMapper.selectListByEcnOrderId(ecnOrder.getId());
        respVO.setItems(BeanUtils.toBean(items, MesMdEcnOrderItemRespVO.class));
        return respVO;
    }

}
