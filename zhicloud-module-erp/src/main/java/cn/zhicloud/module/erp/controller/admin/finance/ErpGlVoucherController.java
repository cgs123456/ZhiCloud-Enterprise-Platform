package cn.zhicloud.module.erp.controller.admin.finance;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherEntryReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherEntryRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherEntryDO;
import cn.zhicloud.module.erp.enums.finance.ErpGlVoucherStatusEnum;
import cn.zhicloud.module.erp.service.finance.ErpGlVoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * ERP 会计凭证 Controller（P0-7）
 *
 * <p>提供会计凭证 CRUD + 审核/反审核。
 * 凭证分录随主表一并返回（嵌套结构），减少前端请求次数。
 *
 * @author 智云
 */
@Tag(name = "管理后台 - ERP 会计凭证")
@RestController
@RequestMapping("/erp/gl-voucher")
@Validated
public class ErpGlVoucherController {

    @Resource
    private ErpGlVoucherService glVoucherService;

    @PostMapping("/create")
    @Operation(summary = "创建会计凭证", description = "校验借贷平衡、科目末级，自动填充分录科目编码/名称")
    @PreAuthorize("@ss.hasPermission('erp:gl-voucher:create')")
    public CommonResult<Long> createGlVoucher(@Valid @RequestBody ErpGlVoucherSaveReqVO createReqVO) {
        return success(glVoucherService.createGlVoucher(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新会计凭证", description = "仅草稿状态可更新")
    @PreAuthorize("@ss.hasPermission('erp:gl-voucher:update')")
    public CommonResult<Boolean> updateGlVoucher(@Valid @RequestBody ErpGlVoucherSaveReqVO updateReqVO) {
        glVoucherService.updateGlVoucher(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会计凭证", description = "仅草稿状态可删除")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:gl-voucher:delete')")
    public CommonResult<Boolean> deleteGlVoucher(@RequestParam("id") Long id) {
        glVoucherService.deleteGlVoucher(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取会计凭证", description = "返回凭证主表 + 分录列表")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:gl-voucher:query')")
    public CommonResult<ErpGlVoucherRespVO> getGlVoucher(@RequestParam("id") Long id) {
        ErpGlVoucherDO voucher = glVoucherService.getGlVoucher(id);
        if (voucher == null) {
            return success(null);
        }
        ErpGlVoucherRespVO respVO = convertToRespVO(voucher);
        // 填充分录
        List<ErpGlVoucherEntryDO> entries = glVoucherService.getGlVoucherEntryList(id);
        if (entries != null) {
            respVO.setEntries(entries.stream()
                    .map(e -> BeanUtils.toBean(e, ErpGlVoucherEntryRespVO.class))
                    .toList());
        } else {
            respVO.setEntries(new ArrayList<>());
        }
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询会计凭证")
    @PreAuthorize("@ss.hasPermission('erp:gl-voucher:query')")
    public CommonResult<PageResult<ErpGlVoucherRespVO>> getGlVoucherPage(@Valid ErpGlVoucherPageReqVO pageReqVO) {
        PageResult<ErpGlVoucherDO> pageResult = glVoucherService.getGlVoucherPage(pageReqVO);
        PageResult<ErpGlVoucherRespVO> result = new PageResult<>(
                pageResult.getList() == null ? null
                        : pageResult.getList().stream().map(this::convertToRespVO).toList(),
                pageResult.getTotal());
        return success(result);
    }

    @PostMapping("/approve")
    @Operation(summary = "审核会计凭证", description = "凭证状态变为已审核，更新对应科目的累计发生额与期末余额")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:gl-voucher:approve')")
    public CommonResult<Boolean> approveGlVoucher(@RequestParam("id") Long id) {
        glVoucherService.approveGlVoucher(id);
        return success(true);
    }

    @PostMapping("/reverse-approve")
    @Operation(summary = "反审核会计凭证", description = "凭证状态回到草稿，回滚对应科目的累计发生额与期末余额")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:gl-voucher:approve')")
    public CommonResult<Boolean> reverseApproveGlVoucher(@RequestParam("id") Long id) {
        glVoucherService.reverseApproveGlVoucher(id);
        return success(true);
    }

    // ==================== 内部辅助方法 ====================

    private ErpGlVoucherRespVO convertToRespVO(ErpGlVoucherDO voucher) {
        ErpGlVoucherRespVO respVO = BeanUtils.toBean(voucher, ErpGlVoucherRespVO.class);
        if (respVO.getStatus() != null) {
            for (ErpGlVoucherStatusEnum statusEnum : ErpGlVoucherStatusEnum.values()) {
                if (statusEnum.getStatus().equals(respVO.getStatus())) {
                    respVO.setStatusName(statusEnum.getName());
                    break;
                }
            }
        }
        if (respVO.getVoucherType() != null) {
            respVO.setVoucherTypeName(getVoucherTypeName(respVO.getVoucherType()));
        }
        return respVO;
    }

    /**
     * 凭证类型名称映射：10 收款 / 20 付款 / 30 转账 / 40 记账
     */
    private String getVoucherTypeName(Integer voucherType) {
        return switch (voucherType) {
            case 10 -> "收款";
            case 20 -> "付款";
            case 30 -> "转账";
            case 40 -> "记账";
            default -> "未知";
        };
    }

}
