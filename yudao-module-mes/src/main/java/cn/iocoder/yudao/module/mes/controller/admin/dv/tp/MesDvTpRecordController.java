package cn.iocoder.yudao.module.mes.controller.admin.dv.tp;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.dv.tp.vo.MesDvTpRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.tp.vo.MesDvTpRecordRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.tp.MesDvTpRecordDO;
import cn.iocoder.yudao.module.mes.service.dv.tp.MesDvTpRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES TPM 执行记录")
@RestController
@RequestMapping("/mes/dv/tp-record")
@Validated
public class MesDvTpRecordController {

    @Resource
    private MesDvTpRecordService tpRecordService;

    @GetMapping("/get")
    @Operation(summary = "获得 TPM 执行记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:dv-tp-record:query')")
    public CommonResult<MesDvTpRecordRespVO> getTpRecord(@RequestParam("id") Long id) {
        MesDvTpRecordDO record = tpRecordService.getTpRecord(id);
        return success(BeanUtils.toBean(record, MesDvTpRecordRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得 TPM 执行记录分页")
    @PreAuthorize("@ss.hasPermission('mes:dv-tp-record:query')")
    public CommonResult<PageResult<MesDvTpRecordRespVO>> getTpRecordPage(@Valid MesDvTpRecordPageReqVO pageReqVO) {
        PageResult<MesDvTpRecordDO> pageResult = tpRecordService.getTpRecordPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MesDvTpRecordRespVO.class));
    }

}