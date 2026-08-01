package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.glaccount.ErpGlAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.glaccount.ErpGlAccountRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.glaccount.ErpGlAccountSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.glaccount.ErpGlAccountSimpleRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpGlAccountDO;
import cn.iocoder.yudao.module.erp.enums.finance.ErpGlAccountBalanceDirectionEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpGlAccountTypeEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpGlAccountService;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * ERP 会计科目 Controller（P0-7）
 *
 * <p>提供会计科目 CRUD + 树形查询 + 末级科目精简列表（用于凭证分录选择）。
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - ERP 会计科目")
@RestController
@RequestMapping("/erp/gl-account")
@Validated
public class ErpGlAccountController {

    @Resource
    private ErpGlAccountService glAccountService;

    @PostMapping("/create")
    @Operation(summary = "创建会计科目")
    @PreAuthorize("@ss.hasPermission('erp:gl-account:create')")
    public CommonResult<Long> createGlAccount(@Valid @RequestBody ErpGlAccountSaveReqVO createReqVO) {
        return success(glAccountService.createGlAccount(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新会计科目")
    @PreAuthorize("@ss.hasPermission('erp:gl-account:update')")
    public CommonResult<Boolean> updateGlAccount(@Valid @RequestBody ErpGlAccountSaveReqVO updateReqVO) {
        glAccountService.updateGlAccount(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会计科目")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:gl-account:delete')")
    public CommonResult<Boolean> deleteGlAccount(@RequestParam("id") Long id) {
        glAccountService.deleteGlAccount(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取会计科目")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:gl-account:query')")
    public CommonResult<ErpGlAccountRespVO> getGlAccount(@RequestParam("id") Long id) {
        ErpGlAccountDO account = glAccountService.getGlAccount(id);
        if (account == null) {
            return success(null);
        }
        return success(convertToRespVO(account));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询会计科目")
    @PreAuthorize("@ss.hasPermission('erp:gl-account:query')")
    public CommonResult<PageResult<ErpGlAccountRespVO>> getGlAccountPage(@Valid ErpGlAccountPageReqVO pageReqVO) {
        PageResult<ErpGlAccountDO> pageResult = glAccountService.getGlAccountPage(pageReqVO);
        PageResult<ErpGlAccountRespVO> result = new PageResult<>(
                pageResult.getList() == null ? null
                        : pageResult.getList().stream().map(this::convertToRespVO).toList(),
                pageResult.getTotal());
        return success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "获取所有会计科目列表", description = "用于前端构建树形结构")
    @PreAuthorize("@ss.hasPermission('erp:gl-account:query')")
    public CommonResult<List<ErpGlAccountRespVO>> getGlAccountList() {
        List<ErpGlAccountDO> list = glAccountService.getGlAccountList();
        List<ErpGlAccountRespVO> result = list == null ? new ArrayList<>()
                : list.stream().map(this::convertToRespVO).toList();
        return success(result);
    }

    @GetMapping("/leaf-list")
    @Operation(summary = "获取末级会计科目列表", description = "用于凭证分录选择科目下拉框")
    @PreAuthorize("@ss.hasPermission('erp:gl-account:query')")
    public CommonResult<List<ErpGlAccountSimpleRespVO>> getLeafGlAccountList() {
        List<ErpGlAccountDO> list = glAccountService.getLeafGlAccountList();
        List<ErpGlAccountSimpleRespVO> result = list == null ? new ArrayList<>()
                : list.stream().map(a -> BeanUtils.toBean(a, ErpGlAccountSimpleRespVO.class)).toList();
        return success(result);
    }

    @GetMapping("/children")
    @Operation(summary = "获取子科目列表")
    @Parameter(name = "parentId", description = "父级编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:gl-account:query')")
    public CommonResult<List<ErpGlAccountRespVO>> getGlAccountListByParentId(@RequestParam("parentId") Long parentId) {
        List<ErpGlAccountDO> list = glAccountService.getGlAccountListByParentId(parentId);
        List<ErpGlAccountRespVO> result = list == null ? new ArrayList<>()
                : list.stream().map(this::convertToRespVO).toList();
        return success(result);
    }

    // ==================== 内部辅助方法 ====================

    private ErpGlAccountRespVO convertToRespVO(ErpGlAccountDO account) {
        ErpGlAccountRespVO respVO = BeanUtils.toBean(account, ErpGlAccountRespVO.class);
        if (respVO.getType() != null) {
            for (ErpGlAccountTypeEnum typeEnum : ErpGlAccountTypeEnum.values()) {
                if (typeEnum.getType().equals(respVO.getType())) {
                    respVO.setTypeName(typeEnum.getName());
                    break;
                }
            }
        }
        if (respVO.getBalanceDirection() != null) {
            for (ErpGlAccountBalanceDirectionEnum dirEnum : ErpGlAccountBalanceDirectionEnum.values()) {
                if (dirEnum.getDirection().equals(respVO.getBalanceDirection())) {
                    respVO.setBalanceDirectionName(dirEnum.getName());
                    break;
                }
            }
        }
        return respVO;
    }

}
