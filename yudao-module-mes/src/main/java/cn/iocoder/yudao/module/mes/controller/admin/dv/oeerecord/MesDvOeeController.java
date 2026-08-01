package cn.iocoder.yudao.module.mes.controller.admin.dv.oeerecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.dv.oeerecord.vo.MesDvOeeCalculateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.oeerecord.vo.MesDvOeeRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.oeerecord.vo.MesDvOeeRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.oeerecord.vo.MesDvOeeTrendRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.oeerecord.MesDvOeeRecordDO;
import cn.iocoder.yudao.module.mes.service.dv.oeerecord.MesDvOeeService;
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

@Tag(name = "管理后台 - MES OEE 设备综合效率")
@RestController
@RequestMapping("/mes/dv/oee")
@Validated
public class MesDvOeeController {

    @Resource
    private MesDvOeeService oeeService;

    @PostMapping("/calculate")
    @Operation(summary = "计算 OEE")
    @PreAuthorize("@ss.hasPermission('mes:dv-oee:calculate')")
    public CommonResult<MesDvOeeRecordRespVO> calculateOee(@Valid @RequestBody MesDvOeeCalculateReqVO reqVO) {
        MesDvOeeRecordDO record = oeeService.calculateOee(reqVO.getMachineryId(), reqVO.getStartDate(), reqVO.getEndDate());
        return success(BeanUtils.toBean(record, MesDvOeeRecordRespVO.class));
    }

    @GetMapping("/trend")
    @Operation(summary = "OEE 趋势图数据")
    @PreAuthorize("@ss.hasPermission('mes:dv-oee:query')")
    public CommonResult<List<MesDvOeeTrendRespVO>> getOeeTrend(
            @RequestParam("machineryId") Long machineryId,
            @RequestParam(value = "days", defaultValue = "7") Integer days) {
        List<MesDvOeeRecordDO> list = oeeService.getOeeTrend(machineryId, days);
        return success(BeanUtils.toBean(list, MesDvOeeTrendRespVO.class));
    }

    @GetMapping("/record/list")
    @Operation(summary = "OEE 记录列表")
    @PreAuthorize("@ss.hasPermission('mes:dv-oee:query')")
    public CommonResult<PageResult<MesDvOeeRecordRespVO>> getOeeRecordPage(@Valid MesDvOeeRecordPageReqVO pageReqVO) {
        PageResult<MesDvOeeRecordDO> pageResult = oeeService.getOeeRecordPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MesDvOeeRecordRespVO.class));
    }

}
