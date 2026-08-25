package cn.zhicloud.module.wms.controller.admin.billing;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.collection.MapUtils;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import cn.zhicloud.module.system.api.user.dto.AdminUserRespDTO;
import cn.zhicloud.module.wms.controller.admin.billing.vo.bill.WmsBillingBillLineRespVO;
import cn.zhicloud.module.wms.controller.admin.billing.vo.bill.WmsBillingBillPageReqVO;
import cn.zhicloud.module.wms.controller.admin.billing.vo.bill.WmsBillingBillRespVO;
import cn.zhicloud.module.wms.controller.admin.billing.vo.bill.WmsBillingBillSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.billing.WmsBillingBillDO;
import cn.zhicloud.module.wms.dal.dataobject.billing.WmsBillingBillLineDO;
import cn.zhicloud.module.wms.dal.dataobject.md.merchant.WmsMerchantDO;
import cn.zhicloud.module.wms.service.billing.WmsBillingBillService;
import cn.zhicloud.module.wms.service.md.merchant.WmsMerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;
import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertSetByFlatMap;

@Tag(name = "管理后台 - WMS 计费账单")
@RestController
@RequestMapping("/wms/billing-bill")
@Validated
public class WmsBillingBillController {

    @Resource
    private WmsBillingBillService billingBillService;
    @Resource
    private WmsMerchantService merchantService;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建计费账单")
    @PreAuthorize("@ss.hasPermission('wms:billing-bill:create')")
    public CommonResult<Long> createBillingBill(@Valid @RequestBody WmsBillingBillSaveReqVO createReqVO) {
        return success(billingBillService.createBillingBill(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新计费账单")
    @PreAuthorize("@ss.hasPermission('wms:billing-bill:update')")
    public CommonResult<Boolean> updateBillingBill(@Valid @RequestBody WmsBillingBillSaveReqVO updateReqVO) {
        billingBillService.updateBillingBill(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除计费账单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('wms:billing-bill:delete')")
    public CommonResult<Boolean> deleteBillingBill(@RequestParam("id") Long id) {
        billingBillService.deleteBillingBill(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得计费账单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:billing-bill:query')")
    public CommonResult<WmsBillingBillRespVO> getBillingBill(@RequestParam("id") Long id) {
        WmsBillingBillDO bill = billingBillService.getBillingBill(id);
        if (bill == null) {
            return success(null);
        }
        // 获得账单明细列表
        List<WmsBillingBillLineDO> lines = billingBillService.getBillLineList(id);
        // 拼接结果返回
        WmsBillingBillRespVO respVO = buildBillRespVO(bill)
                .setLines(BeanUtils.toBean(lines, WmsBillingBillLineRespVO.class));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得计费账单分页")
    @PreAuthorize("@ss.hasPermission('wms:billing-bill:query')")
    public CommonResult<PageResult<WmsBillingBillRespVO>> getBillingBillPage(@Valid WmsBillingBillPageReqVO pageReqVO) {
        PageResult<WmsBillingBillDO> pageResult = billingBillService.getBillingBillPage(pageReqVO);
        return success(new PageResult<>(buildBillRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    @PostMapping("/generate")
    @Operation(summary = "生成计费账单（调用计费引擎）")
    @PreAuthorize("@ss.hasPermission('wms:billing-bill:generate')")
    public CommonResult<Long> generateBill(@RequestParam("ownerId") Long ownerId,
                                           @RequestParam("start") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND) LocalDateTime start,
                                           @RequestParam("end") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND) LocalDateTime end) {
        return success(billingBillService.generateBill(ownerId, start, end));
    }

    // ==================== 拼接 VO ====================

    private WmsBillingBillRespVO buildBillRespVO(WmsBillingBillDO bill) {
        if (bill == null) {
            return null;
        }
        List<WmsBillingBillRespVO> list = buildBillRespVOList(Collections.singletonList(bill));
        return CollUtil.getFirst(list);
    }

    private List<WmsBillingBillRespVO> buildBillRespVOList(List<WmsBillingBillDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 获取相关的商户、用户等数据
        Map<Long, WmsMerchantDO> merchantMap = merchantService.getMerchantMap(convertSet(list, WmsBillingBillDO::getOwnerId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertSetByFlatMap(list,
                bill -> Stream.of(parseUserId(bill.getCreator()), parseUserId(bill.getUpdater()))));
        // 拼接数据
        return BeanUtils.toBean(list, WmsBillingBillRespVO.class, vo -> {
            MapUtils.findAndThen(merchantMap, vo.getOwnerId(), merchant -> vo.setOwnerName(merchant.getName()));
            MapUtils.findAndThen(userMap, parseUserId(vo.getCreator()), user -> vo.setCreatorName(user.getNickname()));
        });
    }

    private Long parseUserId(String userId) {
        return NumberUtil.parseLong(userId, null);
    }

}
