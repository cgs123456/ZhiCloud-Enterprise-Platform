package cn.zhicloud.module.fms.controller.admin.config;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.fms.controller.admin.config.vo.voucherword.FmsVoucherWordRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.voucherword.FmsVoucherWordSaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsVoucherWordDO;
import cn.zhicloud.module.fms.service.config.FmsVoucherWordService;
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

@Tag(name = "管理后台 - FMS 凭证字")
@RestController
@RequestMapping("/fms/config/voucher-word")
@Validated
public class FmsVoucherWordController {

    @Resource
    private FmsVoucherWordService voucherWordService;

    @GetMapping("/list")
    @Operation(summary = "获得凭证字列表")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:voucher-word:query')")
    public CommonResult<List<FmsVoucherWordRespVO>> getVoucherWordList(@RequestParam("accountSetId") Long accountSetId) {
        List<FmsVoucherWordDO> list = voucherWordService.getVoucherWordList(accountSetId);
        return success(BeanUtils.toBean(list, FmsVoucherWordRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得凭证字精简列表")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:voucher-word:query')")
    public CommonResult<List<FmsVoucherWordRespVO>> getVoucherWordSimpleList(@RequestParam("accountSetId") Long accountSetId) {
        List<FmsVoucherWordDO> list = voucherWordService.getVoucherWordList(accountSetId);
        return success(BeanUtils.toBean(list, FmsVoucherWordRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建凭证字")
    @PreAuthorize("@ss.hasPermission('fms:config:voucher-word:create')")
    public CommonResult<Long> createVoucherWord(@Valid @RequestBody FmsVoucherWordSaveReqVO createReqVO) {
        return success(voucherWordService.createVoucherWord(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新凭证字")
    @PreAuthorize("@ss.hasPermission('fms:config:voucher-word:update')")
    public CommonResult<Boolean> updateVoucherWord(@Valid @RequestBody FmsVoucherWordSaveReqVO updateReqVO) {
        voucherWordService.updateVoucherWord(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除凭证字")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:voucher-word:delete')")
    public CommonResult<Boolean> deleteVoucherWord(@RequestParam("accountSetId") Long accountSetId,
                                            @RequestParam("id") Long id) {
        voucherWordService.deleteVoucherWord(accountSetId, id);
        return success(true);
    }

}
