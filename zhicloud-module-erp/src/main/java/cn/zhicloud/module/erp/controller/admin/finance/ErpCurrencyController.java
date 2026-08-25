package cn.zhicloud.module.erp.controller.admin.finance;

import cn.zhicloud.framework.common.enums.CommonStatusEnum;
import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.currency.ErpCurrencyPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.currency.ErpCurrencyRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.currency.ErpCurrencySaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpCurrencyDO;
import cn.zhicloud.module.erp.service.finance.ErpCurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - ERP 币种")
@RestController
@RequestMapping("/erp/currency")
@Validated
public class ErpCurrencyController {

    @Resource
    private ErpCurrencyService currencyService;

    @PostMapping("/create")
    @Operation(summary = "创建币种")
    @PreAuthorize("@ss.hasPermission('erp:currency:create')")
    public CommonResult<Long> createCurrency(@Valid @RequestBody ErpCurrencySaveReqVO createReqVO) {
        return success(currencyService.createCurrency(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新币种")
    @PreAuthorize("@ss.hasPermission('erp:currency:update')")
    public CommonResult<Boolean> updateCurrency(@Valid @RequestBody ErpCurrencySaveReqVO updateReqVO) {
        currencyService.updateCurrency(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除币种")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:currency:delete')")
    public CommonResult<Boolean> deleteCurrency(@RequestParam("id") Long id) {
        currencyService.deleteCurrency(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得币种")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:currency:query')")
    public CommonResult<ErpCurrencyRespVO> getCurrency(@RequestParam("id") Long id) {
        ErpCurrencyDO currency = currencyService.getCurrency(id);
        return success(BeanUtils.toBean(currency, ErpCurrencyRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得币种分页")
    @PreAuthorize("@ss.hasPermission('erp:currency:query')")
    public CommonResult<PageResult<ErpCurrencyRespVO>> getCurrencyPage(@Valid ErpCurrencyPageReqVO pageReqVO) {
        PageResult<ErpCurrencyDO> pageResult = currencyService.getCurrencyPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpCurrencyRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得启用的币种精简列表", description = "用于前端下拉选项")
    public CommonResult<List<ErpCurrencyRespVO>> getCurrencySimpleList() {
        List<ErpCurrencyDO> list = currencyService.getEnabledCurrencyList();
        return success(convertList(list, currency -> BeanUtils.toBean(currency, ErpCurrencyRespVO.class)));
    }

}
