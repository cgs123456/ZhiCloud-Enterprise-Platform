package cn.iocoder.yudao.module.qms.controller.admin.fmea;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.fmea.vo.FmeaItemPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.fmea.vo.FmeaItemRespVO;
import cn.iocoder.yudao.module.qms.controller.admin.fmea.vo.FmeaItemSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.fmea.FmeaItemDO;
import cn.iocoder.yudao.module.qms.enums.qms.FmeaRiskLevelEnum;
import cn.iocoder.yudao.module.qms.service.fmea.FmeaItemServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * QMS FMEA 条目 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - QMS FMEA 条目")
@RestController
@RequestMapping("/qms/fmea/item")
@Validated
public class FmeaItemController {

    @Resource
    private cn.iocoder.yudao.module.qms.service.fmea.FmeaItemService fmeaItemService;

    @PostMapping("/create")
    @Operation(summary = "创建 FMEA 条目", description = "RPN 由 Service 自动计算")
    @PreAuthorize("@ss.hasPermission('qms:fmea:create')")
    public CommonResult<Long> createFmeaItem(@Valid @RequestBody FmeaItemSaveReqVO createReqVO) {
        return success(fmeaItemService.createFmeaItem(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 FMEA 条目", description = "更新时自动重算 RPN 与风险等级")
    @PreAuthorize("@ss.hasPermission('qms:fmea:update')")
    public CommonResult<Boolean> updateFmeaItem(@Valid @RequestBody FmeaItemSaveReqVO updateReqVO) {
        fmeaItemService.updateFmeaItem(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 FMEA 条目")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:fmea:delete')")
    public CommonResult<Boolean> deleteFmeaItem(@RequestParam("id") Long id) {
        fmeaItemService.deleteFmeaItem(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 FMEA 条目")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:fmea:query')")
    public CommonResult<FmeaItemRespVO> getFmeaItem(@RequestParam("id") Long id) {
        FmeaItemDO fmeaItem = fmeaItemService.getFmeaItem(id);
        return success(convertToRespVO(fmeaItem));
    }

    @GetMapping("/page")
    @Operation(summary = "获得 FMEA 条目分页")
    @PreAuthorize("@ss.hasPermission('qms:fmea:query')")
    public CommonResult<PageResult<FmeaItemRespVO>> getFmeaItemPage(@Valid FmeaItemPageReqVO pageReqVO) {
        PageResult<FmeaItemDO> pageResult = fmeaItemService.getFmeaItemPage(pageReqVO);
        List<FmeaItemRespVO> list = BeanUtils.toBean(pageResult.getList(), FmeaItemRespVO.class);
        list.forEach(this::fillRiskLevel);
        return success(new PageResult<>(list, pageResult.getTotal()));
    }

    @GetMapping("/list-by-fmea")
    @Operation(summary = "获得 FMEA 文档下的全部条目")
    @Parameter(name = "fmeaId", description = "FMEA 文档 ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:fmea:query')")
    public CommonResult<List<FmeaItemRespVO>> getFmeaItemListByFmeaId(@RequestParam("fmeaId") Long fmeaId) {
        List<FmeaItemDO> list = fmeaItemService.getFmeaItemListByFmeaId(fmeaId);
        List<FmeaItemRespVO> respList = BeanUtils.toBean(list, FmeaItemRespVO.class);
        respList.forEach(this::fillRiskLevel);
        return success(respList);
    }

    /**
     * DO 转 RespVO
     */
    private FmeaItemRespVO convertToRespVO(FmeaItemDO fmeaItem) {
        if (fmeaItem == null) {
            return null;
        }
        FmeaItemRespVO respVO = BeanUtils.toBean(fmeaItem, FmeaItemRespVO.class);
        fillRiskLevel(respVO);
        return respVO;
    }

    /**
     * 填充风险等级信息
     */
    private void fillRiskLevel(FmeaItemRespVO respVO) {
        if (respVO == null || respVO.getRpn() == null) {
            return;
        }
        FmeaRiskLevelEnum level = FmeaItemServiceImpl.determineRiskLevel(
                respVO.getRpn(), respVO.getSeverity(), respVO.getOccurrence(), respVO.getDetection());
        respVO.setRiskLevelName(level.getName());
        respVO.setRiskLevelColor(level.getColor());
    }

}
