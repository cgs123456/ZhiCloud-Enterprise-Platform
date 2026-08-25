package cn.zhicloud.module.crm.controller.admin.visit;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.collection.MapUtils;
import cn.zhicloud.framework.common.util.number.NumberUtils;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.crm.controller.admin.visit.vo.CrmVisitRecordPageReqVO;
import cn.zhicloud.module.crm.controller.admin.visit.vo.CrmVisitRecordRespVO;
import cn.zhicloud.module.crm.controller.admin.visit.vo.CrmVisitRecordSaveReqVO;
import cn.zhicloud.module.crm.dal.dataobject.contact.CrmContactDO;
import cn.zhicloud.module.crm.dal.dataobject.customer.CrmCustomerDO;
import cn.zhicloud.module.crm.dal.dataobject.visit.CrmVisitRecordDO;
import cn.zhicloud.module.crm.service.contact.CrmContactService;
import cn.zhicloud.module.crm.service.customer.CrmCustomerService;
import cn.zhicloud.module.crm.service.visit.CrmVisitRecordService;
import cn.zhicloud.module.system.api.dept.DeptApi;
import cn.zhicloud.module.system.api.dept.dto.DeptRespDTO;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import cn.zhicloud.module.system.api.user.dto.AdminUserRespDTO;
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

import static cn.zhicloud.framework.common.pojo.CommonResult.success;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertListByFlatMap;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.zhicloud.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - CRM 拜访签到记录")
@RestController
@RequestMapping("/crm/visit-record")
@Validated
public class CrmVisitRecordController {

    @Resource
    private CrmVisitRecordService visitRecordService;
    @Resource
    private CrmCustomerService customerService;
    @Resource
    private CrmContactService contactService;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建拜访签到记录")
    @PreAuthorize("@ss.hasPermission('crm:visit-record:create')")
    public CommonResult<Long> createVisitRecord(@Valid @RequestBody CrmVisitRecordSaveReqVO createReqVO) {
        return success(visitRecordService.createVisitRecord(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新拜访签到记录")
    @PreAuthorize("@ss.hasPermission('crm:visit-record:update')")
    public CommonResult<Boolean> updateVisitRecord(@Valid @RequestBody CrmVisitRecordSaveReqVO updateReqVO) {
        visitRecordService.updateVisitRecord(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除拜访签到记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('crm:visit-record:delete')")
    public CommonResult<Boolean> deleteVisitRecord(@RequestParam("id") Long id) {
        visitRecordService.deleteVisitRecord(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得拜访签到记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('crm:visit-record:query')")
    public CommonResult<CrmVisitRecordRespVO> getVisitRecord(@RequestParam("id") Long id) {
        CrmVisitRecordDO visitRecord = visitRecordService.getVisitRecord(id);
        return success(buildVisitRecordDetail(visitRecord));
    }

    private CrmVisitRecordRespVO buildVisitRecordDetail(CrmVisitRecordDO visitRecord) {
        if (visitRecord == null) {
            return null;
        }
        return buildVisitRecordDetailList(Collections.singletonList(visitRecord)).get(0);
    }

    @GetMapping("/page")
    @Operation(summary = "获得拜访签到记录分页")
    @PreAuthorize("@ss.hasPermission('crm:visit-record:query')")
    public CommonResult<PageResult<CrmVisitRecordRespVO>> getVisitRecordPage(@Valid CrmVisitRecordPageReqVO pageReqVO) {
        PageResult<CrmVisitRecordDO> pageResult = visitRecordService.getVisitRecordPage(pageReqVO);
        return success(new PageResult<>(buildVisitRecordDetailList(pageResult.getList()), pageResult.getTotal()));
    }

    private List<CrmVisitRecordRespVO> buildVisitRecordDetailList(List<CrmVisitRecordDO> visitRecordList) {
        if (CollUtil.isEmpty(visitRecordList)) {
            return Collections.emptyList();
        }
        // 1.1 获取客户列表
        Map<Long, CrmCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(visitRecordList, CrmVisitRecordDO::getCustomerId));
        // 1.2 获取联系人列表
        Map<Long, CrmContactDO> contactMap = contactService.getContactMap(
                convertSet(visitRecordList, CrmVisitRecordDO::getContactId));
        // 1.3 获取创建人、负责人列表
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertListByFlatMap(visitRecordList,
                visitRecord -> Stream.of(NumberUtils.parseLong(visitRecord.getCreator()), visitRecord.getOwnerUserId())));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(userMap.values(), AdminUserRespDTO::getDeptId));
        // 2. 拼接结果
        return BeanUtils.toBean(visitRecordList, CrmVisitRecordRespVO.class, (visitRecordVO) -> {
            // 2.1 拼接客户名称
            MapUtils.findAndThen(customerMap, visitRecordVO.getCustomerId(), customer -> visitRecordVO.setCustomerName(customer.getName()));
            // 2.2 拼接联系人名称
            MapUtils.findAndThen(contactMap, visitRecordVO.getContactId(), contact -> visitRecordVO.setContactName(contact.getName()));
            // 2.3 拼接负责人、创建人名称
            MapUtils.findAndThen(userMap, NumberUtils.parseLong(visitRecordVO.getCreator()),
                    user -> visitRecordVO.setCreatorName(user.getNickname()));
            MapUtils.findAndThen(userMap, visitRecordVO.getOwnerUserId(), user -> {
                visitRecordVO.setOwnerUserName(user.getNickname());
                MapUtils.findAndThen(deptMap, user.getDeptId(), dept -> visitRecordVO.setOwnerUserDeptName(dept.getName()));
            });
        });
    }

}
