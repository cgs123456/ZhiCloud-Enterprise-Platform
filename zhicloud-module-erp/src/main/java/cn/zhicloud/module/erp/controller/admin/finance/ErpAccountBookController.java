package cn.zhicloud.module.erp.controller.admin.finance;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.accountbook.ErpAccountBookPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.accountbook.ErpAccountBookRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.accountbook.ErpAccountBookSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpAccountBookDO;
import cn.zhicloud.module.erp.enums.finance.ErpAccountBookStatusEnum;
import cn.zhicloud.module.erp.enums.finance.ErpAccountingStandardEnum;
import cn.zhicloud.module.erp.service.finance.ErpAccountBookService;
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
 * ERP 账簿 Controller（P1-多账簿）
 *
 * <p>提供账簿主数据 CRUD + 切换主账簿接口。
 *
 * @author 智云
 */
@Tag(name = "管理后台 - ERP 账簿")
@RestController
@RequestMapping("/erp/account-book")
@Validated
public class ErpAccountBookController {

    @Resource
    private ErpAccountBookService accountBookService;

    @PostMapping("/create")
    @Operation(summary = "创建账簿")
    @PreAuthorize("@ss.hasPermission('erp:account-book:create')")
    public CommonResult<Long> createAccountBook(@Valid @RequestBody ErpAccountBookSaveReqVO createReqVO) {
        return success(accountBookService.createAccountBook(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新账簿")
    @PreAuthorize("@ss.hasPermission('erp:account-book:update')")
    public CommonResult<Boolean> updateAccountBook(@Valid @RequestBody ErpAccountBookSaveReqVO updateReqVO) {
        accountBookService.updateAccountBook(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除账簿")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:account-book:delete')")
    public CommonResult<Boolean> deleteAccountBook(@RequestParam("id") Long id) {
        accountBookService.deleteAccountBook(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取账簿")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:account-book:query')")
    public CommonResult<ErpAccountBookRespVO> getAccountBook(@RequestParam("id") Long id) {
        ErpAccountBookDO accountBook = accountBookService.getAccountBook(id);
        if (accountBook == null) {
            return success(null);
        }
        return success(convertToRespVO(accountBook));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询账簿")
    @PreAuthorize("@ss.hasPermission('erp:account-book:query')")
    public CommonResult<PageResult<ErpAccountBookRespVO>> getAccountBookPage(@Valid ErpAccountBookPageReqVO pageReqVO) {
        PageResult<ErpAccountBookDO> pageResult = accountBookService.getAccountBookPage(pageReqVO);
        PageResult<ErpAccountBookRespVO> result = new PageResult<>(
                pageResult.getList() == null ? new ArrayList<>()
                        : pageResult.getList().stream().map(this::convertToRespVO).toList(),
                pageResult.getTotal());
        return success(result);
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得启用的账簿精简列表", description = "用于前端下拉选项")
    public CommonResult<List<ErpAccountBookRespVO>> getAccountBookSimpleList() {
        List<ErpAccountBookDO> list = accountBookService.getEnabledAccountBookList();
        if (list == null) {
            return success(new ArrayList<>());
        }
        return success(list.stream().map(this::convertToRespVO).toList());
    }

    @PutMapping("/set-primary")
    @Operation(summary = "切换主账簿", description = "将指定账簿设为同准则下的主账簿，原主账簿自动置为非主账簿")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:account-book:update')")
    public CommonResult<Boolean> setPrimary(@RequestParam("id") Long id) {
        accountBookService.setPrimary(id);
        return success(true);
    }

    // ==================== 内部辅助方法 ====================

    private ErpAccountBookRespVO convertToRespVO(ErpAccountBookDO accountBook) {
        ErpAccountBookRespVO respVO = BeanUtils.toBean(accountBook, ErpAccountBookRespVO.class);
        if (respVO.getAccountingStandard() != null) {
            for (ErpAccountingStandardEnum standardEnum : ErpAccountingStandardEnum.values()) {
                if (standardEnum.getType().equals(respVO.getAccountingStandard())) {
                    respVO.setAccountingStandardName(standardEnum.getName());
                    break;
                }
            }
        }
        if (respVO.getStatus() != null) {
            for (ErpAccountBookStatusEnum statusEnum : ErpAccountBookStatusEnum.values()) {
                if (statusEnum.getStatus().equals(respVO.getStatus())) {
                    respVO.setStatusName(statusEnum.getName());
                    break;
                }
            }
        }
        return respVO;
    }

}
