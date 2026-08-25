package cn.zhicloud.module.qms.controller.admin.inspectionrecord;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.inspectionrecord.vo.InspectionRecordPageReqVO;
import cn.zhicloud.module.qms.controller.admin.inspectionrecord.vo.InspectionRecordRespVO;
import cn.zhicloud.module.qms.controller.admin.inspectionrecord.vo.InspectionRecordSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.inspectionrecord.InspectionRecordDO;
import cn.zhicloud.module.qms.service.inspectionrecord.InspectionRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - QMS 检验记录")
@RestController
@RequestMapping("/qms/inspection-record")
@Validated
public class InspectionRecordController {

    @Resource
    private InspectionRecordService inspectionRecordService;

    @PostMapping("/create")
    @Operation(summary = "创建检验记录")
    @PreAuthorize("@ss.hasPermission('qms:inspection-record:create')")
    public CommonResult<Long> createInspectionRecord(@Valid @RequestBody InspectionRecordSaveReqVO createReqVO) {
        return success(inspectionRecordService.createInspectionRecord(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新检验记录")
    @PreAuthorize("@ss.hasPermission('qms:inspection-record:update')")
    public CommonResult<Boolean> updateInspectionRecord(@Valid @RequestBody InspectionRecordSaveReqVO updateReqVO) {
        inspectionRecordService.updateInspectionRecord(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除检验记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:inspection-record:delete')")
    public CommonResult<Boolean> deleteInspectionRecord(@RequestParam("id") Long id) {
        inspectionRecordService.deleteInspectionRecord(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得检验记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:inspection-record:query')")
    public CommonResult<InspectionRecordRespVO> getInspectionRecord(@RequestParam("id") Long id) {
        InspectionRecordDO inspectionRecord = inspectionRecordService.getInspectionRecord(id);
        return success(BeanUtils.toBean(inspectionRecord, InspectionRecordRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得检验记录分页")
    @PreAuthorize("@ss.hasPermission('qms:inspection-record:query')")
    public CommonResult<PageResult<InspectionRecordRespVO>> getInspectionRecordPage(@Valid InspectionRecordPageReqVO pageReqVO) {
        PageResult<InspectionRecordDO> pageResult = inspectionRecordService.getInspectionRecordPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, InspectionRecordRespVO.class));
    }

}
