package cn.zhicloud.module.fms.controller.admin.config;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.fms.controller.admin.config.vo.digest.FmsDigestRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.digest.FmsDigestSaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsDigestDO;
import cn.zhicloud.module.fms.service.config.FmsDigestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - FMS 常用摘要")
@RestController
@RequestMapping("/fms/config/digest")
@Validated
public class FmsDigestController {

    @Resource
    private FmsDigestService digestService;

    @GetMapping("/list")
    @Operation(summary = "获得常用摘要列表")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:digest:query')")
    public CommonResult<List<FmsDigestRespVO>> getDigestList(@RequestParam("accountSetId") Long accountSetId) {
        List<FmsDigestDO> list = digestService.getDigestList(accountSetId);
        return success(BeanUtils.toBean(list, FmsDigestRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得常用摘要精简列表")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:digest:query')")
    public CommonResult<List<FmsDigestRespVO>> getDigestSimpleList(@RequestParam("accountSetId") Long accountSetId) {
        List<FmsDigestDO> list = digestService.getDigestList(accountSetId);
        return success(BeanUtils.toBean(list, FmsDigestRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建常用摘要")
    @PreAuthorize("@ss.hasPermission('fms:config:digest:create')")
    public CommonResult<Long> createDigest(@Valid @RequestBody FmsDigestSaveReqVO createReqVO) {
        return success(digestService.createDigest(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新常用摘要")
    @PreAuthorize("@ss.hasPermission('fms:config:digest:update')")
    public CommonResult<Boolean> updateDigest(@Valid @RequestBody FmsDigestSaveReqVO updateReqVO) {
        digestService.updateDigest(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除常用摘要")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:digest:delete')")
    public CommonResult<Boolean> deleteDigest(@RequestParam("accountSetId") Long accountSetId,
                                            @RequestParam("id") Long id) {
        digestService.deleteDigest(accountSetId, id);
        return success(true);
    }

}
