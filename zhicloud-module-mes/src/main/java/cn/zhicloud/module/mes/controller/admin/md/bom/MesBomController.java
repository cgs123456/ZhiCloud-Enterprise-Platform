package cn.zhicloud.module.mes.controller.admin.md.bom;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.mes.controller.admin.md.bom.vo.MesBomExplodeRespVO;
import cn.zhicloud.module.mes.controller.admin.md.bom.vo.MesBomPageReqVO;
import cn.zhicloud.module.mes.controller.admin.md.bom.vo.MesBomRespVO;
import cn.zhicloud.module.mes.controller.admin.md.bom.vo.MesBomSaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.md.bom.MesBomDO;
import cn.zhicloud.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.zhicloud.module.mes.service.md.bom.MesBomService;
import cn.zhicloud.module.mes.service.md.item.MesMdItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 独立 BOM")
@RestController
@RequestMapping("/mes/md/bom")
@Validated
public class MesBomController {

    @Resource
    private MesBomService bomService;
    @Resource
    private MesMdItemService itemService;

    @PostMapping("/create")
    @Operation(summary = "创建 BOM")
    @PreAuthorize("@ss.hasPermission('mes:md-bom:create')")
    public CommonResult<Long> createBom(@Valid @RequestBody MesBomSaveReqVO createReqVO) {
        return success(bomService.createBom(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 BOM")
    @PreAuthorize("@ss.hasPermission('mes:md-bom:update')")
    public CommonResult<Boolean> updateBom(@Valid @RequestBody MesBomSaveReqVO updateReqVO) {
        bomService.updateBom(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 BOM")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:md-bom:delete')")
    public CommonResult<Boolean> deleteBom(@RequestParam("id") Long id) {
        bomService.deleteBom(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 BOM")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:md-bom:query')")
    public CommonResult<MesBomRespVO> getBom(@RequestParam("id") Long id) {
        MesBomDO bom = bomService.getBom(id);
        return success(buildBomRespVO(bom));
    }

    @GetMapping("/page")
    @Operation(summary = "获得 BOM 分页")
    @PreAuthorize("@ss.hasPermission('mes:md-bom:query')")
    public CommonResult<PageResult<MesBomRespVO>> getBomPage(@Valid MesBomPageReqVO pageReqVO) {
        PageResult<MesBomDO> pageResult = bomService.getBomPage(pageReqVO);
        return success(new PageResult<>(buildBomRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/explode")
    @Operation(summary = "BOM 递归展开")
    @PreAuthorize("@ss.hasPermission('mes:md-bom:query')")
    public CommonResult<MesBomExplodeRespVO> explodeBom(
            @RequestParam("productId") Long productId,
            @RequestParam(value = "quantity", defaultValue = "1") BigDecimal quantity) {
        List<MesBomService.BomExplodeNode> nodes = bomService.explodeBom(productId, quantity);
        MesBomExplodeRespVO respVO = new MesBomExplodeRespVO();
        respVO.setProductId(productId);
        respVO.setQuantity(quantity);
        List<MesBomExplodeRespVO.BomRequirement> requirements = nodes.stream()
                .map(n -> new MesBomExplodeRespVO.BomRequirement(n.getProductId(), n.getLevel(), n.getQuantity(), n.getUnit()))
                .toList();
        respVO.setRequirements(requirements);
        return success(respVO);
    }

    @GetMapping("/calculate-cost")
    @Operation(summary = "BOM 成本卷积")
    @PreAuthorize("@ss.hasPermission('mes:md-bom:query')")
    public CommonResult<BigDecimal> calculateBomCost(@RequestParam("productId") Long productId) {
        return success(bomService.calculateBomCost(productId));
    }

    // ==================== 拼接 VO ====================

    private MesBomRespVO buildBomRespVO(MesBomDO bom) {
        if (bom == null) {
            return null;
        }
        MesBomRespVO vo = BeanUtils.toBean(bom, MesBomRespVO.class);
        fillProductInfo(vo, bom.getProductId());
        return vo;
    }

    private List<MesBomRespVO> buildBomRespVOList(List<MesBomDO> list) {
        List<MesBomRespVO> voList = BeanUtils.toBean(list, MesBomRespVO.class);
        for (MesBomRespVO vo : voList) {
            fillProductInfo(vo, vo.getProductId());
        }
        return voList;
    }

    private void fillProductInfo(MesBomRespVO vo, Long productId) {
        if (productId == null) {
            return;
        }
        MesMdItemDO item = itemService.getItem(productId);
        if (item != null) {
            vo.setProductCode(item.getCode());
            vo.setProductName(item.getName());
        }
    }

}