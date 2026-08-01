package cn.iocoder.yudao.module.wms.controller.admin.billing;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.wms.controller.admin.billing.vo.contract.WmsBillingContractItemRespVO;
import cn.iocoder.yudao.module.wms.controller.admin.billing.vo.contract.WmsBillingContractPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.billing.vo.contract.WmsBillingContractRespVO;
import cn.iocoder.yudao.module.wms.controller.admin.billing.vo.contract.WmsBillingContractSaveReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.billing.WmsBillingContractDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.billing.WmsBillingContractItemDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.merchant.WmsMerchantDO;
import cn.iocoder.yudao.module.wms.service.billing.WmsBillingContractService;
import cn.iocoder.yudao.module.wms.service.md.merchant.WmsMerchantService;
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
import java.util.Map;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSetByFlatMap;

@Tag(name = "管理后台 - WMS 计费合同")
@RestController
@RequestMapping("/wms/billing-contract")
@Validated
public class WmsBillingContractController {

    @Resource
    private WmsBillingContractService billingContractService;
    @Resource
    private WmsMerchantService merchantService;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建计费合同")
    @PreAuthorize("@ss.hasPermission('wms:billing-contract:create')")
    public CommonResult<Long> createBillingContract(@Valid @RequestBody WmsBillingContractSaveReqVO createReqVO) {
        return success(billingContractService.createBillingContract(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新计费合同")
    @PreAuthorize("@ss.hasPermission('wms:billing-contract:update')")
    public CommonResult<Boolean> updateBillingContract(@Valid @RequestBody WmsBillingContractSaveReqVO updateReqVO) {
        billingContractService.updateBillingContract(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除计费合同")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('wms:billing-contract:delete')")
    public CommonResult<Boolean> deleteBillingContract(@RequestParam("id") Long id) {
        billingContractService.deleteBillingContract(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得计费合同")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:billing-contract:query')")
    public CommonResult<WmsBillingContractRespVO> getBillingContract(@RequestParam("id") Long id) {
        WmsBillingContractDO contract = billingContractService.getBillingContract(id);
        if (contract == null) {
            return success(null);
        }
        // 获得合同条款列表
        List<WmsBillingContractItemDO> items = billingContractService.getContractItemList(id);
        // 拼接结果返回
        WmsBillingContractRespVO respVO = buildContractRespVO(contract)
                .setItems(BeanUtils.toBean(items, WmsBillingContractItemRespVO.class));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得计费合同分页")
    @PreAuthorize("@ss.hasPermission('wms:billing-contract:query')")
    public CommonResult<PageResult<WmsBillingContractRespVO>> getBillingContractPage(@Valid WmsBillingContractPageReqVO pageReqVO) {
        PageResult<WmsBillingContractDO> pageResult = billingContractService.getBillingContractPage(pageReqVO);
        return success(new PageResult<>(buildContractRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    // ==================== 拼接 VO ====================

    private WmsBillingContractRespVO buildContractRespVO(WmsBillingContractDO contract) {
        if (contract == null) {
            return null;
        }
        List<WmsBillingContractRespVO> list = buildContractRespVOList(Collections.singletonList(contract));
        return CollUtil.getFirst(list);
    }

    private List<WmsBillingContractRespVO> buildContractRespVOList(List<WmsBillingContractDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 获取相关的商户、用户等数据
        Map<Long, WmsMerchantDO> merchantMap = merchantService.getMerchantMap(convertSet(list, WmsBillingContractDO::getOwnerId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertSetByFlatMap(list,
                contract -> Stream.of(parseUserId(contract.getCreator()), parseUserId(contract.getUpdater()))));
        // 拼接数据
        return BeanUtils.toBean(list, WmsBillingContractRespVO.class, vo -> {
            MapUtils.findAndThen(merchantMap, vo.getOwnerId(), merchant -> vo.setOwnerName(merchant.getName()));
            MapUtils.findAndThen(userMap, parseUserId(vo.getCreator()), user -> vo.setCreatorName(user.getNickname()));
        });
    }

    private Long parseUserId(String userId) {
        return NumberUtil.parseLong(userId, null);
    }

}
