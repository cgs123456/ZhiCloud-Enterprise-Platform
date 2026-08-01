package cn.iocoder.yudao.module.crm.controller.admin.salesorder;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.number.NumberUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.crm.controller.admin.salesorder.vo.CrmSaleOrderPageReqVO;
import cn.iocoder.yudao.module.crm.controller.admin.salesorder.vo.CrmSaleOrderRespVO;
import cn.iocoder.yudao.module.crm.controller.admin.salesorder.vo.CrmSaleOrderSaveReqVO;
import cn.iocoder.yudao.module.crm.dal.dataobject.business.CrmBusinessDO;
import cn.iocoder.yudao.module.crm.dal.dataobject.contact.CrmContactDO;
import cn.iocoder.yudao.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.iocoder.yudao.module.crm.dal.dataobject.customer.CrmCustomerDO;
import cn.iocoder.yudao.module.crm.dal.dataobject.salesorder.CrmSaleOrderDO;
import cn.iocoder.yudao.module.crm.dal.dataobject.salesorder.CrmSaleOrderItemDO;
import cn.iocoder.yudao.module.crm.service.business.CrmBusinessService;
import cn.iocoder.yudao.module.crm.service.contact.CrmContactService;
import cn.iocoder.yudao.module.crm.service.contract.CrmContractService;
import cn.iocoder.yudao.module.crm.service.customer.CrmCustomerService;
import cn.iocoder.yudao.module.crm.service.salesorder.CrmSaleOrderService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.framework.common.util.collection.MapUtils.findAndThen;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static java.util.Collections.singletonList;

@Tag(name = "管理后台 - CRM 销售订单")
@RestController
@RequestMapping("/crm/sale-order")
@Validated
public class CrmSaleOrderController {

    @Resource
    private CrmSaleOrderService saleOrderService;
    @Resource
    private CrmCustomerService customerService;
    @Resource
    private CrmContractService contractService;
    @Resource
    private CrmBusinessService businessService;
    @Resource
    private CrmContactService contactService;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建销售订单")
    @PreAuthorize("@ss.hasPermission('crm:sale-order:create')")
    public CommonResult<Long> createSaleOrder(@Valid @RequestBody CrmSaleOrderSaveReqVO createReqVO) {
        return success(saleOrderService.createSaleOrder(createReqVO, getLoginUserId()));
    }

    @PutMapping("/update")
    @Operation(summary = "更新销售订单")
    @PreAuthorize("@ss.hasPermission('crm:sale-order:update')")
    public CommonResult<Boolean> updateSaleOrder(@Valid @RequestBody CrmSaleOrderSaveReqVO updateReqVO) {
        saleOrderService.updateSaleOrder(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除销售订单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('crm:sale-order:delete')")
    public CommonResult<Boolean> deleteSaleOrder(@RequestParam("id") Long id) {
        saleOrderService.deleteSaleOrder(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得销售订单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('crm:sale-order:query')")
    public CommonResult<CrmSaleOrderRespVO> getSaleOrder(@RequestParam("id") Long id) {
        CrmSaleOrderDO order = saleOrderService.getSaleOrder(id);
        return success(buildSaleOrderDetail(order));
    }

    private CrmSaleOrderRespVO buildSaleOrderDetail(CrmSaleOrderDO order) {
        if (order == null) {
            return null;
        }
        CrmSaleOrderRespVO orderVO = buildSaleOrderDetailList(singletonList(order)).get(0);
        // 拼接订单明细
        List<CrmSaleOrderItemDO> items = saleOrderService.getSaleOrderItemListByOrderId(orderVO.getId());
        orderVO.setItems(BeanUtils.toBean(items, CrmSaleOrderRespVO.Item.class));
        return orderVO;
    }

    @GetMapping("/page")
    @Operation(summary = "获得销售订单分页")
    @PreAuthorize("@ss.hasPermission('crm:sale-order:query')")
    public CommonResult<PageResult<CrmSaleOrderRespVO>> getSaleOrderPage(@Valid CrmSaleOrderPageReqVO pageVO) {
        PageResult<CrmSaleOrderDO> pageResult = saleOrderService.getSaleOrderPage(pageVO);
        return success(BeanUtils.toBean(pageResult, CrmSaleOrderRespVO.class).setList(buildSaleOrderDetailList(pageResult.getList())));
    }

    @GetMapping("/page-by-contract")
    @Operation(summary = "获得销售订单分页，基于指定合同")
    public CommonResult<PageResult<CrmSaleOrderRespVO>> getSaleOrderPageByContract(@Valid CrmSaleOrderPageReqVO pageVO) {
        PageResult<CrmSaleOrderDO> pageResult = saleOrderService.getSaleOrderPageByContractId(pageVO);
        return success(BeanUtils.toBean(pageResult, CrmSaleOrderRespVO.class).setList(buildSaleOrderDetailList(pageResult.getList())));
    }

    @GetMapping("/page-by-customer")
    @Operation(summary = "获得销售订单分页，基于指定客户")
    public CommonResult<PageResult<CrmSaleOrderRespVO>> getSaleOrderPageByCustomer(@Valid CrmSaleOrderPageReqVO pageVO) {
        PageResult<CrmSaleOrderDO> pageResult = saleOrderService.getSaleOrderPageByCustomerId(pageVO);
        return success(BeanUtils.toBean(pageResult, CrmSaleOrderRespVO.class).setList(buildSaleOrderDetailList(pageResult.getList())));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出销售订单 Excel")
    @PreAuthorize("@ss.hasPermission('crm:sale-order:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSaleOrderExcel(@Valid CrmSaleOrderPageReqVO exportReqVO,
                                     HttpServletResponse response) throws IOException {
        PageResult<CrmSaleOrderDO> pageResult = saleOrderService.getSaleOrderPage(exportReqVO);
        // 导出 Excel
        ExcelUtils.write(response, "销售订单.xls", "数据", CrmSaleOrderRespVO.class,
                BeanUtils.toBean(pageResult.getList(), CrmSaleOrderRespVO.class));
    }

    @PutMapping("/confirm")
    @Operation(summary = "确认销售订单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('crm:sale-order:update')")
    public CommonResult<Boolean> confirmSaleOrder(@RequestParam("id") Long id) {
        saleOrderService.confirmSaleOrder(id);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交销售订单审批")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('crm:sale-order:update')")
    public CommonResult<Boolean> submitSaleOrder(@RequestParam("id") Long id) {
        saleOrderService.submitSaleOrder(id, getLoginUserId());
        return success(true);
    }

    private List<CrmSaleOrderRespVO> buildSaleOrderDetailList(List<CrmSaleOrderDO> orderList) {
        if (CollUtil.isEmpty(orderList)) {
            return Collections.emptyList();
        }
        // 1.1 获取客户列表
        Map<Long, CrmCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(orderList, CrmSaleOrderDO::getCustomerId));
        // 1.2 获取创建人、负责人列表
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertListByFlatMap(orderList,
                order -> Stream.of(NumberUtils.parseLong(order.getCreator()), order.getOwnerUserId())));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(userMap.values(), AdminUserRespDTO::getDeptId));
        // 1.3 获取合同
        Map<Long, CrmContractDO> contractMap = contractService.getContractMap(
                convertSet(orderList, CrmSaleOrderDO::getContractId));
        // 1.4 获取商机
        Map<Long, CrmBusinessDO> businessMap = businessService.getBusinessMap(
                convertSet(orderList, CrmSaleOrderDO::getBusinessId));
        // 1.5 获取联系人
        Map<Long, CrmContactDO> contactMap = convertMap(contactService.getContactList(convertSet(orderList,
                CrmSaleOrderDO::getContactId)), CrmContactDO::getId);
        // 2. 拼接数据
        return BeanUtils.toBean(orderList, CrmSaleOrderRespVO.class, orderVO -> {
            // 2.1 设置客户信息
            findAndThen(customerMap, orderVO.getCustomerId(), customer -> orderVO.setCustomerName(customer.getName()));
            // 2.2 设置用户信息
            findAndThen(userMap, Long.parseLong(orderVO.getCreator()), user -> orderVO.setCreatorName(user.getNickname()));
            MapUtils.findAndThen(userMap, orderVO.getOwnerUserId(), user -> {
                orderVO.setOwnerUserName(user.getNickname());
                MapUtils.findAndThen(deptMap, user.getDeptId(), dept -> orderVO.setOwnerUserDeptName(dept.getName()));
            });
            // 2.3 设置合同信息
            findAndThen(contractMap, orderVO.getContractId(), contract -> orderVO.setContractName(contract.getName()));
            // 2.4 设置商机信息
            findAndThen(businessMap, orderVO.getBusinessId(), business -> orderVO.setBusinessName(business.getName()));
            // 2.5 设置联系人信息
            findAndThen(contactMap, orderVO.getContactId(), contact -> orderVO.setContactName(contact.getName()));
        });
    }

}
