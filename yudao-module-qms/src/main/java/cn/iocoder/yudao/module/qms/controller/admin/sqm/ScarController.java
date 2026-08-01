package cn.iocoder.yudao.module.qms.controller.admin.sqm;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.sqm.vo.ScarPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.sqm.vo.ScarRespVO;
import cn.iocoder.yudao.module.qms.controller.admin.sqm.vo.ScarSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.sqm.ScarDO;
import cn.iocoder.yudao.module.qms.service.sqm.ScarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * QMS SCAR 供应商纠正措施请求 Controller
 *
 * @author yudao
 */
@Tag(name = "管理后台 - QMS SCAR 供应商纠正措施")
@RestController
@RequestMapping("/qms/scar")
@Validated
public class ScarController {

    @Resource
    private ScarService scarService;

    @PostMapping("/create")
    @Operation(summary = "创建 SCAR")
    @PreAuthorize("@ss.hasPermission('qms:scar:create')")
    public CommonResult<Long> createScar(@Valid @RequestBody ScarSaveReqVO createReqVO) {
        return success(scarService.createScar(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 SCAR")
    @PreAuthorize("@ss.hasPermission('qms:scar:update')")
    public CommonResult<Boolean> updateScar(@Valid @RequestBody ScarSaveReqVO updateReqVO) {
        scarService.updateScar(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 SCAR")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:scar:delete')")
    public CommonResult<Boolean> deleteScar(@RequestParam("id") Long id) {
        scarService.deleteScar(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 SCAR")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:scar:query')")
    public CommonResult<ScarRespVO> getScar(@RequestParam("id") Long id) {
        ScarDO scar = scarService.getScar(id);
        return success(BeanUtils.toBean(scar, ScarRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得 SCAR 分页")
    @PreAuthorize("@ss.hasPermission('qms:scar:query')")
    public CommonResult<PageResult<ScarRespVO>> getScarPage(@Valid ScarPageReqVO pageReqVO) {
        PageResult<ScarDO> pageResult = scarService.getScarPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ScarRespVO.class));
    }

    @PutMapping("/close")
    @Operation(summary = "关闭 SCAR")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:scar:update')")
    public CommonResult<Boolean> closeScar(@RequestParam("id") Long id) {
        scarService.closeScar(id);
        return success(true);
    }

}