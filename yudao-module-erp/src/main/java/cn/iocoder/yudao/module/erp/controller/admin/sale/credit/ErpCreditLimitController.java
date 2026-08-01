package cn.iocoder.yudao.module.erp.controller.admin.sale.credit;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.credit.vo.ErpCreditLimitPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.credit.vo.ErpCreditLimitRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.credit.vo.ErpCreditLimitSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.credit.ErpCreditLimitDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.credit.ErpCreditLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 客户信用额度")
@RestController
@RequestMapping("/erp/credit-limit")
@Validated
public class ErpCreditLimitController {

    @Resource
    private ErpCreditLimitService creditLimitService;
    @Resource
    private ErpCustomerService customerService;

    @PostMapping("/create")
    @Operation(summary = "创建客户信用额度")
    @PreAuthorize("@ss.hasPermission('erp:credit-limit:create')")
    public CommonResult<Long> createCreditLimit(@Valid @RequestBody ErpCreditLimitSaveReqVO createReqVO) {
        return success(creditLimitService.createCreditLimit(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新客户信用额度")
    @PreAuthorize("@ss.hasPermission('erp:credit-limit:update')")
    public CommonResult<Boolean> updateCreditLimit(@Valid @RequestBody ErpCreditLimitSaveReqVO updateReqVO) {
        creditLimitService.updateCreditLimit(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客户信用额度")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:credit-limit:delete')")
    public CommonResult<Boolean> deleteCreditLimit(@RequestParam("ids") List<Long> ids) {
        creditLimitService.deleteCreditLimit(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得客户信用额度")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:credit-limit:query')")
    public CommonResult<ErpCreditLimitRespVO> getCreditLimit(@RequestParam("id") Long id) {
        ErpCreditLimitDO creditLimit = creditLimitService.getCreditLimit(id);
        if (creditLimit == null) {
            return success(null);
        }
        ErpCreditLimitRespVO respVO = BeanUtils.toBean(creditLimit, ErpCreditLimitRespVO.class);
        fillCustomerName(respVO, creditLimit.getCustomerId());
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得客户信用额度分页")
    @PreAuthorize("@ss.hasPermission('erp:credit-limit:query')")
    public CommonResult<PageResult<ErpCreditLimitRespVO>> getCreditLimitPage(@Valid ErpCreditLimitPageReqVO pageReqVO) {
        PageResult<ErpCreditLimitDO> pageResult = creditLimitService.getCreditLimitPage(pageReqVO);
        PageResult<ErpCreditLimitRespVO> respPage = BeanUtils.toBean(pageResult, ErpCreditLimitRespVO.class);
        // 填充客户名称
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(respPage.getList(), ErpCreditLimitRespVO::getCustomerId));
        respPage.getList().forEach(vo -> fillCustomerName(vo, customerMap));
        return success(respPage);
    }

    private void fillCustomerName(ErpCreditLimitRespVO vo, Long customerId) {
        if (customerId == null) {
            return;
        }
        ErpCustomerDO customer = customerService.getCustomer(customerId);
        if (customer != null) {
            vo.setCustomerName(customer.getName());
        }
    }

    private void fillCustomerName(ErpCreditLimitRespVO vo, Map<Long, ErpCustomerDO> customerMap) {
        if (vo.getCustomerId() == null) {
            return;
        }
        ErpCustomerDO customer = customerMap.get(vo.getCustomerId());
        if (customer != null) {
            vo.setCustomerName(customer.getName());
        }
    }

}
