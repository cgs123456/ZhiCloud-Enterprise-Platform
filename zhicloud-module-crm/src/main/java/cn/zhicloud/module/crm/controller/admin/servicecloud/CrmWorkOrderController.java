package cn.zhicloud.module.crm.controller.admin.servicecloud;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.apilog.core.annotation.ApiAccessLog;
import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.number.NumberUtils;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.excel.core.util.ExcelUtils;
import cn.zhicloud.module.crm.controller.admin.servicecloud.vo.CrmWorkOrderPageReqVO;
import cn.zhicloud.module.crm.controller.admin.servicecloud.vo.CrmWorkOrderRespVO;
import cn.zhicloud.module.crm.controller.admin.servicecloud.vo.CrmWorkOrderSaveReqVO;
import cn.zhicloud.module.crm.dal.dataobject.contact.CrmContactDO;
import cn.zhicloud.module.crm.dal.dataobject.customer.CrmCustomerDO;
import cn.zhicloud.module.crm.dal.dataobject.product.CrmProductDO;
import cn.zhicloud.module.crm.service.contact.CrmContactService;
import cn.zhicloud.module.crm.service.customer.CrmCustomerService;
import cn.zhicloud.module.crm.service.product.CrmProductService;
import cn.zhicloud.module.crm.service.servicecloud.CrmWorkOrderDO;
import cn.zhicloud.module.crm.service.servicecloud.CrmWorkOrderService;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import cn.zhicloud.module.system.api.user.dto.AdminUserRespDTO;
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

import static cn.zhicloud.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.zhicloud.framework.common.pojo.CommonResult.success;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.*;
import static cn.zhicloud.framework.common.util.collection.MapUtils.findAndThen;

@Tag(name = "管理后台 - CRM 售后工单")
@RestController
@RequestMapping("/crm/work-order")
@Validated
public class CrmWorkOrderController {

    @Resource
    private CrmWorkOrderService workOrderService;
    @Resource
    private CrmCustomerService customerService;
    @Resource
    private CrmContactService contactService;
    @Resource
    private CrmProductService productService;

    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建售后工单")
    @PreAuthorize("@ss.hasPermission('crm:work-order:create')")
    public CommonResult<Long> createWorkOrder(@Valid @RequestBody CrmWorkOrderSaveReqVO createReqVO) {
        return success(workOrderService.createWorkOrder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新售后工单")
    @PreAuthorize("@ss.hasPermission('crm:work-order:update')")
    public CommonResult<Boolean> updateWorkOrder(@Valid @RequestBody CrmWorkOrderSaveReqVO updateReqVO) {
        workOrderService.updateWorkOrder(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除售后工单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('crm:work-order:delete')")
    public CommonResult<Boolean> deleteWorkOrder(@RequestParam("id") Long id) {
        workOrderService.deleteWorkOrder(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得售后工单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('crm:work-order:query')")
    public CommonResult<CrmWorkOrderRespVO> getWorkOrder(@RequestParam("id") Long id) {
        CrmWorkOrderDO workOrder = workOrderService.getWorkOrder(id);
        return success(buildWorkOrderDetailList(Collections.singletonList(workOrder)).get(0));
    }

    @GetMapping("/page")
    @Operation(summary = "获得售后工单分页")
    @PreAuthorize("@ss.hasPermission('crm:work-order:query')")
    public CommonResult<PageResult<CrmWorkOrderRespVO>> getWorkOrderPage(@Valid CrmWorkOrderPageReqVO pageVO) {
        PageResult<CrmWorkOrderDO> pageResult = workOrderService.getWorkOrderPage(pageVO);
        return success(BeanUtils.toBean(pageResult, CrmWorkOrderRespVO.class)
                .setList(buildWorkOrderDetailList(pageResult.getList())));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出售后工单 Excel")
    @PreAuthorize("@ss.hasPermission('crm:work-order:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportWorkOrderExcel(@Valid CrmWorkOrderPageReqVO exportReqVO,
                                     HttpServletResponse response) throws IOException {
        PageResult<CrmWorkOrderDO> pageResult = workOrderService.getWorkOrderPage(exportReqVO);
        // 导出 Excel
        ExcelUtils.write(response, "售后工单.xls", "数据", CrmWorkOrderRespVO.class,
                BeanUtils.toBean(pageResult.getList(), CrmWorkOrderRespVO.class));
    }

    @PutMapping("/assign")
    @Operation(summary = "分配工单")
    @PreAuthorize("@ss.hasPermission('crm:work-order:update')")
    public CommonResult<Boolean> assignWorkOrder(@RequestParam("id") Long id,
                                                 @RequestParam("assigneeUserId") Long assigneeUserId) {
        workOrderService.assignWorkOrder(id, assigneeUserId);
        return success(true);
    }

    @PutMapping("/resolve")
    @Operation(summary = "解决工单")
    @PreAuthorize("@ss.hasPermission('crm:work-order:update')")
    public CommonResult<Boolean> resolveWorkOrder(@RequestParam("id") Long id,
                                                  @RequestParam("resolution") String resolution) {
        workOrderService.resolveWorkOrder(id, resolution);
        return success(true);
    }

    @PutMapping("/close")
    @Operation(summary = "关闭工单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('crm:work-order:update')")
    public CommonResult<Boolean> closeWorkOrder(@RequestParam("id") Long id) {
        workOrderService.closeWorkOrder(id);
        return success(true);
    }

    private List<CrmWorkOrderRespVO> buildWorkOrderDetailList(List<CrmWorkOrderDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 1.1 获取客户列表
        Map<Long, CrmCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(list, CrmWorkOrderDO::getCustomerId));
        // 1.2 获取创建人、处理人列表
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertListByFlatMap(list,
                wo -> Stream.of(NumberUtils.parseLong(wo.getCreator()), wo.getAssigneeUserId())));
        // 1.3 获取联系人
        Map<Long, CrmContactDO> contactMap = convertMap(contactService.getContactList(convertSet(list,
                CrmWorkOrderDO::getContactId)), CrmContactDO::getId);
        // 1.4 获取产品
        Map<Long, CrmProductDO> productMap = productService.getProductMap(
                convertSet(list, CrmWorkOrderDO::getProductId));
        // 2. 拼接数据
        return BeanUtils.toBean(list, CrmWorkOrderRespVO.class, woVO -> {
            // 2.1 设置客户信息
            findAndThen(customerMap, woVO.getCustomerId(), customer -> woVO.setCustomerName(customer.getName()));
            // 2.2 设置创建人、处理人信息
            findAndThen(userMap, NumberUtils.parseLong(woVO.getCreator()), user -> woVO.setCreatorName(user.getNickname()));
            findAndThen(userMap, woVO.getAssigneeUserId(), user -> woVO.setAssigneeUserName(user.getNickname()));
            // 2.3 设置联系人信息
            findAndThen(contactMap, woVO.getContactId(), contact -> woVO.setContactName(contact.getName()));
            // 2.4 设置产品信息
            findAndThen(productMap, woVO.getProductId(), product -> woVO.setProductName(product.getName()));
        });
    }

}
