package cn.iocoder.yudao.module.mes.controller.admin.md.bom.substitute;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.md.bom.substitute.vo.MesBomSubstitutePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.bom.substitute.vo.MesBomSubstituteRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.bom.substitute.vo.MesBomSubstituteSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.bom.MesBomSubstituteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.service.md.bom.substitute.MesBomSubstituteService;
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

@Tag(name = "管理后台 - MES BOM 替代料")
@RestController
@RequestMapping("/mes/md/bom-substitute")
@Validated
public class MesBomSubstituteController {

    @Resource
    private MesBomSubstituteService bomSubstituteService;
    @Resource
    private MesMdItemService itemService;

    @PostMapping("/create")
    @Operation(summary = "创建 BOM 替代料")
    @PreAuthorize("@ss.hasPermission('mes:md-bom:create')")
    public CommonResult<Long> createBomSubstitute(@Valid @RequestBody MesBomSubstituteSaveReqVO createReqVO) {
        return success(bomSubstituteService.createBomSubstitute(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 BOM 替代料")
    @PreAuthorize("@ss.hasPermission('mes:md-bom:update')")
    public CommonResult<Boolean> updateBomSubstitute(@Valid @RequestBody MesBomSubstituteSaveReqVO updateReqVO) {
        bomSubstituteService.updateBomSubstitute(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 BOM 替代料")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:md-bom:delete')")
    public CommonResult<Boolean> deleteBomSubstitute(@RequestParam("id") Long id) {
        bomSubstituteService.deleteBomSubstitute(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 BOM 替代料")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:md-bom:query')")
    public CommonResult<MesBomSubstituteRespVO> getBomSubstitute(@RequestParam("id") Long id) {
        MesBomSubstituteDO substitute = bomSubstituteService.getBomSubstitute(id);
        return success(buildRespVO(substitute));
    }

    @GetMapping("/page")
    @Operation(summary = "获得 BOM 替代料分页")
    @PreAuthorize("@ss.hasPermission('mes:md-bom:query')")
    public CommonResult<PageResult<MesBomSubstituteRespVO>> getBomSubstitutePage(@Valid MesBomSubstitutePageReqVO pageReqVO) {
        PageResult<MesBomSubstituteDO> pageResult = bomSubstituteService.getBomSubstitutePage(pageReqVO);
        return success(new PageResult<>(buildRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/list-by-detail-id")
    @Operation(summary = "按 BOM 明细 ID 获得替代料列表（按优先级升序）")
    @Parameter(name = "bomDetailId", description = "BOM 明细 ID", required = true, example = "10")
    @PreAuthorize("@ss.hasPermission('mes:md-bom:query')")
    public CommonResult<List<MesBomSubstituteRespVO>> getSubstitutesByBomDetailId(
            @RequestParam("bomDetailId") Long bomDetailId) {
        return success(buildRespVOList(bomSubstituteService.getSubstitutesByBomDetailId(bomDetailId)));
    }

    // ==================== 拼接 VO ====================

    private MesBomSubstituteRespVO buildRespVO(MesBomSubstituteDO substitute) {
        if (substitute == null) {
            return null;
        }
        MesBomSubstituteRespVO vo = BeanUtils.toBean(substitute, MesBomSubstituteRespVO.class);
        fillItemInfo(vo, substitute.getSubstituteItemId());
        return vo;
    }

    private List<MesBomSubstituteRespVO> buildRespVOList(List<MesBomSubstituteDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<Long, MesMdItemDO> itemMap = itemService.getItemMap(
                convertSet(list, MesBomSubstituteDO::getSubstituteItemId));
        return BeanUtils.toBean(list, MesBomSubstituteRespVO.class, vo -> {
            MesMdItemDO item = itemMap.get(vo.getSubstituteItemId());
            if (item != null) {
                vo.setSubstituteItemCode(item.getCode());
                vo.setSubstituteItemName(item.getName());
            }
        });
    }

    private void fillItemInfo(MesBomSubstituteRespVO vo, Long substituteItemId) {
        if (substituteItemId == null) {
            return;
        }
        MesMdItemDO item = itemService.getItem(substituteItemId);
        if (item != null) {
            vo.setSubstituteItemCode(item.getCode());
            vo.setSubstituteItemName(item.getName());
        }
    }

}