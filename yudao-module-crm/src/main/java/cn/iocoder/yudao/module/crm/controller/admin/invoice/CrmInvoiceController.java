package cn.iocoder.yudao.module.crm.controller.admin.invoice;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.number.NumberUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.crm.controller.admin.invoice.vo.CrmInvoicePageReqVO;
import cn.iocoder.yudao.module.crm.controller.admin.invoice.vo.CrmInvoiceRespVO;
import cn.iocoder.yudao.module.crm.controller.admin.invoice.vo.CrmInvoiceSaveReqVO;
import cn.iocoder.yudao.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.iocoder.yudao.module.crm.dal.dataobject.customer.CrmCustomerDO;
import cn.iocoder.yudao.module.crm.dal.dataobject.invoice.CrmInvoiceDO;
import cn.iocoder.yudao.module.crm.service.contract.CrmContractService;
import cn.iocoder.yudao.module.crm.service.customer.CrmCustomerService;
import cn.iocoder.yudao.module.crm.service.invoice.CrmInvoiceService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
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
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertListByFlatMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - CRM 开票")
@RestController
@RequestMapping("/crm/invoice")
@Validated
public class CrmInvoiceController {

    @Resource
    private CrmInvoiceService invoiceService;
    @Resource
    private CrmContractService contractService;
    @Resource
    private CrmCustomerService customerService;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建开票")
    @PreAuthorize("@ss.hasPermission('crm:invoice:create')")
    public CommonResult<Long> createInvoice(@Valid @RequestBody CrmInvoiceSaveReqVO createReqVO) {
        return success(invoiceService.createInvoice(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新开票")
    @PreAuthorize("@ss.hasPermission('crm:invoice:update')")
    public CommonResult<Boolean> updateInvoice(@Valid @RequestBody CrmInvoiceSaveReqVO updateReqVO) {
        invoiceService.updateInvoice(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除开票")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('crm:invoice:delete')")
    public CommonResult<Boolean> deleteInvoice(@RequestParam("id") Long id) {
        invoiceService.deleteInvoice(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得开票")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('crm:invoice:query')")
    public CommonResult<CrmInvoiceRespVO> getInvoice(@RequestParam("id") Long id) {
        CrmInvoiceDO invoice = invoiceService.getInvoice(id);
        return success(buildInvoiceDetail(invoice));
    }

    private CrmInvoiceRespVO buildInvoiceDetail(CrmInvoiceDO invoice) {
        if (invoice == null) {
            return null;
        }
        return buildInvoiceDetailList(Collections.singletonList(invoice)).get(0);
    }

    @GetMapping("/page")
    @Operation(summary = "获得开票分页")
    @PreAuthorize("@ss.hasPermission('crm:invoice:query')")
    public CommonResult<PageResult<CrmInvoiceRespVO>> getInvoicePage(@Valid CrmInvoicePageReqVO pageReqVO) {
        PageResult<CrmInvoiceDO> pageResult = invoiceService.getInvoicePage(pageReqVO);
        return success(new PageResult<>(buildInvoiceDetailList(pageResult.getList()), pageResult.getTotal()));
    }

    @PutMapping("/submit")
    @Operation(summary = "提交开票审批")
    @PreAuthorize("@ss.hasPermission('crm:invoice:update')")
    public CommonResult<Boolean> submitInvoice(@RequestParam("id") Long id) {
        invoiceService.submitInvoice(id, getLoginUserId());
        return success(true);
    }

    private List<CrmInvoiceRespVO> buildInvoiceDetailList(List<CrmInvoiceDO> invoiceList) {
        if (CollUtil.isEmpty(invoiceList)) {
            return Collections.emptyList();
        }
        // 1.1 获取客户列表
        Map<Long, CrmCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(invoiceList, CrmInvoiceDO::getCustomerId));
        // 1.2 获取创建人、负责人列表
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertListByFlatMap(invoiceList,
                invoice -> Stream.of(NumberUtils.parseLong(invoice.getCreator()), invoice.getOwnerUserId())));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(userMap.values(), AdminUserRespDTO::getDeptId));
        // 1.3 获得合同列表
        Map<Long, CrmContractDO> contractMap = contractService.getContractMap(
                convertSet(invoiceList, CrmInvoiceDO::getContractId));
        // 2. 拼接结果
        return BeanUtils.toBean(invoiceList, CrmInvoiceRespVO.class, (invoiceVO) -> {
            // 2.1 拼接客户名称
            MapUtils.findAndThen(customerMap, invoiceVO.getCustomerId(), customer -> invoiceVO.setCustomerName(customer.getName()));
            // 2.2 拼接负责人、创建人名称
            MapUtils.findAndThen(userMap, NumberUtils.parseLong(invoiceVO.getCreator()),
                    user -> invoiceVO.setCreatorName(user.getNickname()));
            MapUtils.findAndThen(userMap, invoiceVO.getOwnerUserId(), user -> {
                invoiceVO.setOwnerUserName(user.getNickname());
                MapUtils.findAndThen(deptMap, user.getDeptId(), dept -> invoiceVO.setOwnerUserDeptName(dept.getName()));
            });
            // 2.3 拼接合同信息
            MapUtils.findAndThen(contractMap, invoiceVO.getContractId(), contract -> invoiceVO.setContractName(contract.getName()));
        });
    }

}
