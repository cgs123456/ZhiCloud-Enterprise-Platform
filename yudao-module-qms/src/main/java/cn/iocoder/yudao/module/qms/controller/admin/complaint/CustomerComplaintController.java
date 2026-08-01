package cn.iocoder.yudao.module.qms.controller.admin.complaint;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.complaint.vo.CustomerComplaintPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.complaint.vo.CustomerComplaintRespVO;
import cn.iocoder.yudao.module.qms.controller.admin.complaint.vo.CustomerComplaintSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.complaint.CustomerComplaintDO;
import cn.iocoder.yudao.module.qms.service.complaint.CustomerComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * QMS 客户投诉 Controller
 *
 * @author yudao
 */
@Tag(name = "管理后台 - QMS 客户投诉")
@RestController
@RequestMapping("/qms/customer-complaint")
@Validated
public class CustomerComplaintController {

    @Resource
    private CustomerComplaintService customerComplaintService;

    @PostMapping("/create")
    @Operation(summary = "创建客户投诉")
    @PreAuthorize("@ss.hasPermission('qms:customer-complaint:create')")
    public CommonResult<Long> createCustomerComplaint(@Valid @RequestBody CustomerComplaintSaveReqVO createReqVO) {
        return success(customerComplaintService.createCustomerComplaint(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新客户投诉")
    @PreAuthorize("@ss.hasPermission('qms:customer-complaint:update')")
    public CommonResult<Boolean> updateCustomerComplaint(@Valid @RequestBody CustomerComplaintSaveReqVO updateReqVO) {
        customerComplaintService.updateCustomerComplaint(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客户投诉")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:customer-complaint:delete')")
    public CommonResult<Boolean> deleteCustomerComplaint(@RequestParam("id") Long id) {
        customerComplaintService.deleteCustomerComplaint(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得客户投诉")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:customer-complaint:query')")
    public CommonResult<CustomerComplaintRespVO> getCustomerComplaint(@RequestParam("id") Long id) {
        CustomerComplaintDO complaint = customerComplaintService.getCustomerComplaint(id);
        return success(BeanUtils.toBean(complaint, CustomerComplaintRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得客户投诉分页")
    @PreAuthorize("@ss.hasPermission('qms:customer-complaint:query')")
    public CommonResult<PageResult<CustomerComplaintRespVO>> getCustomerComplaintPage(@Valid CustomerComplaintPageReqVO pageReqVO) {
        PageResult<CustomerComplaintDO> pageResult = customerComplaintService.getCustomerComplaintPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CustomerComplaintRespVO.class));
    }

    @PutMapping("/advance-status")
    @Operation(summary = "推进投诉状态", description = "已登记 -> 调查中 -> 处理中")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:customer-complaint:update')")
    public CommonResult<Boolean> advanceStatus(@RequestParam("id") Long id) {
        customerComplaintService.advanceStatus(id);
        return success(true);
    }

    @PutMapping("/close")
    @Operation(summary = "关闭投诉")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:customer-complaint:update')")
    public CommonResult<Boolean> closeComplaint(@RequestParam("id") Long id) {
        customerComplaintService.closeComplaint(id);
        return success(true);
    }

}