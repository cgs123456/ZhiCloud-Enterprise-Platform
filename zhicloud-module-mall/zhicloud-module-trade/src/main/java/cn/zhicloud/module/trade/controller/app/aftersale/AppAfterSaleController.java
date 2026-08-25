package cn.zhicloud.module.trade.controller.app.aftersale;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.trade.controller.app.aftersale.vo.AppAfterSaleCreateReqVO;
import cn.zhicloud.module.trade.controller.app.aftersale.vo.AppAfterSaleDeliveryReqVO;
import cn.zhicloud.module.trade.controller.app.aftersale.vo.AppAfterSalePageReqVO;
import cn.zhicloud.module.trade.controller.app.aftersale.vo.AppAfterSaleRespVO;
import cn.zhicloud.module.trade.dal.dataobject.aftersale.AfterSaleDO;
import cn.zhicloud.module.trade.service.aftersale.AfterSaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;
import static cn.zhicloud.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户 App - 交易售后")
@RestController
@RequestMapping("/trade/after-sale")
@Validated
@Slf4j
public class AppAfterSaleController {

    @Resource
    private AfterSaleService afterSaleService;

    @GetMapping(value = "/page")
    @Operation(summary = "获得售后分页")
    public CommonResult<PageResult<AppAfterSaleRespVO>> getAfterSalePage(AppAfterSalePageReqVO pageReqVO) {
        PageResult<AfterSaleDO> pageResult = afterSaleService.getAfterSalePage(getLoginUserId(), pageReqVO);
        return success(BeanUtils.toBean(pageResult, AppAfterSaleRespVO.class));
    }

    @GetMapping(value = "/get")
    @Operation(summary = "获得售后订单")
    @Parameter(name = "id", description = "售后编号", required = true, example = "1")
    public CommonResult<AppAfterSaleRespVO> getAfterSale(@RequestParam("id") Long id) {
        AfterSaleDO afterSale = afterSaleService.getAfterSale(getLoginUserId(), id);
        return success(BeanUtils.toBean(afterSale, AppAfterSaleRespVO.class));
    }

    @PostMapping(value = "/create")
    @Operation(summary = "申请售后")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:create')")
    public CommonResult<Long> createAfterSale(@Valid @RequestBody AppAfterSaleCreateReqVO createReqVO) {
        return success(afterSaleService.createAfterSale(getLoginUserId(), createReqVO));
    }

    @PutMapping(value = "/delivery")
    @Operation(summary = "退回货物")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:update')")
    public CommonResult<Boolean> deliveryAfterSale(@Valid @RequestBody AppAfterSaleDeliveryReqVO deliveryReqVO) {
        afterSaleService.deliveryAfterSale(getLoginUserId(), deliveryReqVO);
        return success(true);
    }

    @DeleteMapping(value = "/cancel")
    @Operation(summary = "取消售后")
    @Parameter(name = "id", description = "售后编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:delete')")
    public CommonResult<Boolean> cancelAfterSale(@RequestParam("id") Long id) {
        afterSaleService.cancelAfterSale(getLoginUserId(), id);
        return success(true);
    }

}
