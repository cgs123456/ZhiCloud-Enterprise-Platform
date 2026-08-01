package cn.iocoder.yudao.module.mes.controller.admin.md.bom;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.md.bom.vo.MesBomDetailPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.bom.vo.MesBomDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.bom.vo.MesBomDetailSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.bom.MesBomDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.service.md.bom.MesBomDetailService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - MES BOM 明细")
@RestController
@RequestMapping("/mes/md/bom-detail")
@Validated
public class MesBomDetailController {

    @Resource
    private MesBomDetailService bomDetailService;
    @Resource
    private MesMdItemService itemService;

    @PostMapping("/create")
    @Operation(summary = "创建 BOM 明细")
    @PreAuthorize("@ss.hasPermission('mes:md-bom:create')")
    public CommonResult<Long> createBomDetail(@Valid @RequestBody MesBomDetailSaveReqVO createReqVO) {
        return success(bomDetailService.createBomDetail(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 BOM 明细")
    @PreAuthorize("@ss.hasPermission('mes:md-bom:update')")
    public CommonResult<Boolean> updateBomDetail(@Valid @RequestBody MesBomDetailSaveReqVO updateReqVO) {
        bomDetailService.updateBomDetail(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 BOM 明细")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:md-bom:delete')")
    public CommonResult<Boolean> deleteBomDetail(@RequestParam("id") Long id) {
        bomDetailService.deleteBomDetail(id);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得 BOM 明细分页")
    @PreAuthorize("@ss.hasPermission('mes:md-bom:query')")
    public CommonResult<PageResult<MesBomDetailRespVO>> getBomDetailPage(@Valid MesBomDetailPageReqVO pageReqVO) {
        PageResult<MesBomDetailDO> pageResult = bomDetailService.getBomDetailPage(pageReqVO);
        return success(new PageResult<>(buildRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/list-by-bom-id")
    @Operation(summary = "按 BOM 编号获得明细列表")
    @Parameter(name = "bomId", description = "BOM 主数据编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('mes:md-bom:query')")
    public CommonResult<List<MesBomDetailRespVO>> getBomDetailListByBomId(@RequestParam("bomId") Long bomId) {
        return success(buildRespVOList(bomDetailService.getBomDetailListByBomId(bomId)));
    }

    private List<MesBomDetailRespVO> buildRespVOList(List<MesBomDetailDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<Long, MesMdItemDO> itemMap = itemService.getItemMap(
                convertSet(list, MesBomDetailDO::getProductId));
        return BeanUtils.toBean(list, MesBomDetailRespVO.class, vo -> {
            MesMdItemDO item = itemMap.get(vo.getProductId());
            if (item != null) {
                vo.setProductCode(item.getCode());
                vo.setProductName(item.getName());
            }
        });
    }

}