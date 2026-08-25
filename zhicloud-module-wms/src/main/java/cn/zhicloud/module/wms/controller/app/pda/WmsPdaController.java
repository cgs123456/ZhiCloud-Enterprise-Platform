package cn.zhicloud.module.wms.controller.app.pda;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.module.wms.controller.app.pda.vo.WmsPdaCheckReqVO;
import cn.zhicloud.module.wms.controller.app.pda.vo.WmsPdaLoginReqVO;
import cn.zhicloud.module.wms.controller.app.pda.vo.WmsPdaLoginRespVO;
import cn.zhicloud.module.wms.controller.app.pda.vo.WmsPdaPickReqVO;
import cn.zhicloud.module.wms.controller.app.pda.vo.WmsPdaPutawayReqVO;
import cn.zhicloud.module.wms.controller.app.pda.vo.WmsPdaReceiptReqVO;
import cn.zhicloud.module.wms.controller.app.pda.vo.WmsPdaScanReqVO;
import cn.zhicloud.module.wms.controller.app.pda.vo.WmsPdaScanRespVO;
import cn.zhicloud.module.wms.controller.app.pda.vo.WmsPdaTaskRespVO;
import cn.zhicloud.module.wms.service.pda.WmsPdaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * WMS PDA 移动端 Controller
 *
 * @author 智云
 */
@Tag(name = "用户 App - WMS PDA 移动端")
@RestController
@RequestMapping("/wms/pda")
@Validated
public class WmsPdaController {

    @Resource
    private WmsPdaService pdaService;

    @PostMapping("/login")
    @Operation(summary = "PDA 设备登录")
    @PermitAll
    public CommonResult<WmsPdaLoginRespVO> login(@RequestBody @Valid WmsPdaLoginReqVO reqVO) {
        return success(pdaService.login(reqVO));
    }

    @GetMapping("/task/list")
    @Operation(summary = "获取待执行任务（拣货/上架/盘点）")
    public CommonResult<List<WmsPdaTaskRespVO>> getTaskList() {
        return success(pdaService.getTaskList());
    }

    @PostMapping("/task/scan-location")
    @Operation(summary = "扫码库位，返回库存信息")
    @PreAuthorize("@ss.hasPermission('wms:pda:scan')")
    public CommonResult<WmsPdaScanRespVO> scanLocation(@RequestBody @Valid WmsPdaScanReqVO reqVO) {
        reqVO.setScanType(WmsPdaScanReqVO.SCAN_TYPE_LOCATION);
        return success(pdaService.scan(reqVO));
    }

    @PostMapping("/task/scan-item")
    @Operation(summary = "扫码物料，返回 SKU 信息")
    @PreAuthorize("@ss.hasPermission('wms:pda:scan')")
    public CommonResult<WmsPdaScanRespVO> scanItem(@RequestBody @Valid WmsPdaScanReqVO reqVO) {
        reqVO.setScanType(WmsPdaScanReqVO.SCAN_TYPE_ITEM);
        return success(pdaService.scan(reqVO));
    }

    @PostMapping("/receipt/confirm")
    @Operation(summary = "PDA 收货确认")
    @PreAuthorize("@ss.hasPermission('wms:pda:receipt')")
    public CommonResult<Boolean> confirmReceipt(@RequestBody @Valid WmsPdaReceiptReqVO reqVO) {
        pdaService.confirmReceipt(reqVO);
        return success(true);
    }

    @PostMapping("/putaway/execute")
    @Operation(summary = "PDA 上架执行")
    @PreAuthorize("@ss.hasPermission('wms:pda:putaway')")
    public CommonResult<Boolean> executePutaway(@RequestBody @Valid WmsPdaPutawayReqVO reqVO) {
        pdaService.executePutaway(reqVO);
        return success(true);
    }

    @PostMapping("/pick/execute")
    @Operation(summary = "PDA 拣货执行")
    @PreAuthorize("@ss.hasPermission('wms:pda:pick')")
    public CommonResult<Boolean> executePick(@RequestBody @Valid WmsPdaPickReqVO reqVO) {
        pdaService.executePick(reqVO);
        return success(true);
    }

    @PostMapping("/check/execute")
    @Operation(summary = "PDA 盘点录入")
    @PreAuthorize("@ss.hasPermission('wms:pda:check')")
    public CommonResult<Boolean> executeCheck(@RequestBody @Valid WmsPdaCheckReqVO reqVO) {
        pdaService.executeCheck(reqVO);
        return success(true);
    }

}
