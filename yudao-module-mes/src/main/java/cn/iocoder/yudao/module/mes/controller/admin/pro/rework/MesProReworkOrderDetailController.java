package cn.iocoder.yudao.module.mes.controller.admin.pro.rework;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.rework.vo.MesProReworkOrderDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.rework.vo.MesProReworkOrderDetailSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.rework.MesProReworkOrderDetailDO;
import cn.iocoder.yudao.module.mes.service.pro.rework.MesProReworkOrderDetailService;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 返工工单明细")
@RestController
@RequestMapping("/mes/pro/rework-order-detail")
@Validated
public class MesProReworkOrderDetailController {

    @Resource
    private MesProReworkOrderDetailService reworkOrderDetailService;

    @PostMapping("/create")
    @Operation(summary = "新增返工明细")
    @PreAuthorize("@ss.hasPermission('mes:pro:rework-order:create')")
    public CommonResult<Long> addDetail(@Valid @RequestBody MesProReworkOrderDetailSaveReqVO createReqVO) {
        return success(reworkOrderDetailService.addDetail(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新返工明细")
    @PreAuthorize("@ss.hasPermission('mes:pro:rework-order:update')")
    public CommonResult<Boolean> updateDetail(@Valid @RequestBody MesProReworkOrderDetailSaveReqVO updateReqVO) {
        reworkOrderDetailService.updateDetail(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除返工明细")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro:rework-order:delete')")
    public CommonResult<Boolean> deleteDetail(@RequestParam("id") Long id) {
        reworkOrderDetailService.deleteDetail(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得返工明细")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:pro:rework-order:query')")
    public CommonResult<MesProReworkOrderDetailRespVO> getDetail(@RequestParam("id") Long id) {
        MesProReworkOrderDetailDO detail = reworkOrderDetailService.getDetail(id);
        if (detail == null) {
            return success(null);
        }
        return success(BeanUtils.toBean(detail, MesProReworkOrderDetailRespVO.class));
    }

    @GetMapping("/list-by-rework-order")
    @Operation(summary = "根据返工工单编号获得明细列表")
    @Parameter(name = "reworkOrderId", description = "返工工单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:pro:rework-order:query')")
    public CommonResult<List<MesProReworkOrderDetailRespVO>> listByReworkOrderId(
            @RequestParam("reworkOrderId") Long reworkOrderId) {
        List<MesProReworkOrderDetailDO> list = reworkOrderDetailService.listByReworkOrderId(reworkOrderId);
        if (CollUtil.isEmpty(list)) {
            return success(Collections.emptyList());
        }
        return success(BeanUtils.toBean(list, MesProReworkOrderDetailRespVO.class));
    }

}
