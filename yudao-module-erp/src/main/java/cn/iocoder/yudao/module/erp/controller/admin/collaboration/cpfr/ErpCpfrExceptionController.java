package cn.iocoder.yudao.module.erp.controller.admin.collaboration.cpfr;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.collaboration.cpfr.vo.ErpCpfrExceptionHandleReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.collaboration.cpfr.vo.ErpCpfrExceptionPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.collaboration.cpfr.vo.ErpCpfrExceptionRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.collaboration.cpfr.ErpCpfrExceptionDO;
import cn.iocoder.yudao.module.erp.service.collaboration.cpfr.ErpCpfrExceptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP CPFR 协同异常")
@RestController
@RequestMapping("/erp/cpfr-exception")
@Validated
public class ErpCpfrExceptionController {

    @Resource
    private ErpCpfrExceptionService cpfrExceptionService;

    @DeleteMapping("/delete")
    @Operation(summary = "删除异常")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:cpfr-exception:delete')")
    public CommonResult<Boolean> deleteException(@RequestParam("id") Long id) {
        cpfrExceptionService.deleteException(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得异常")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:cpfr-exception:query')")
    public CommonResult<ErpCpfrExceptionRespVO> getException(@RequestParam("id") Long id) {
        ErpCpfrExceptionDO exception = cpfrExceptionService.getException(id);
        return success(BeanUtils.toBean(exception, ErpCpfrExceptionRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得异常分页")
    @PreAuthorize("@ss.hasPermission('erp:cpfr-exception:query')")
    public CommonResult<PageResult<ErpCpfrExceptionRespVO>> getExceptionPage(
            @Valid ErpCpfrExceptionPageReqVO pageReqVO) {
        PageResult<ErpCpfrExceptionDO> pageResult = cpfrExceptionService.getExceptionPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpCpfrExceptionRespVO.class));
    }

    @PutMapping("/handle")
    @Operation(summary = "处理异常")
    @PreAuthorize("@ss.hasPermission('erp:cpfr-exception:update')")
    public CommonResult<Boolean> handleException(@Valid @RequestBody ErpCpfrExceptionHandleReqVO handleReqVO) {
        cpfrExceptionService.handleException(handleReqVO);
        return success(true);
    }

}
