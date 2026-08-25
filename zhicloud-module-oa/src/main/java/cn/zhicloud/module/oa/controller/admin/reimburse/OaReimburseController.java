package cn.zhicloud.module.oa.controller.admin.reimburse;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.excel.core.util.ExcelUtils;
import cn.zhicloud.module.oa.controller.admin.reimburse.vo.OaReimburseItemVO;
import cn.zhicloud.module.oa.controller.admin.reimburse.vo.OaReimbursePageReqVO;
import cn.zhicloud.module.oa.controller.admin.reimburse.vo.OaReimburseRespVO;
import cn.zhicloud.module.oa.controller.admin.reimburse.vo.OaReimburseSaveReqVO;
import cn.zhicloud.module.oa.dal.dataobject.reimburse.OaReimburseDO;
import cn.zhicloud.module.oa.dal.dataobject.reimburse.OaReimburseItemDO;
import cn.zhicloud.module.oa.dal.mysql.reimburse.OaReimburseItemMapper;
import cn.zhicloud.module.oa.service.reimburse.OaReimburseService;
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
import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;
import static cn.zhicloud.framework.common.pojo.PageParam.PAGE_SIZE_NONE;

@Tag(name = "管理后台 - OA 报销管理")
@RestController
@RequestMapping("/oa/reimburse")
@Validated
public class OaReimburseController {

    @Resource
    private OaReimburseService reimburseService;
    @Resource
    private OaReimburseItemMapper reimburseItemMapper;

    @PostMapping("/create")
    @Operation(summary = "创建报销单")
    @PreAuthorize("@ss.hasPermission('oa:reimburse:create')")
    public CommonResult<Long> createReimburse(@Valid @RequestBody OaReimburseSaveReqVO createReqVO) {
        return success(reimburseService.createReimburse(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新报销单")
    @PreAuthorize("@ss.hasPermission('oa:reimburse:update')")
    public CommonResult<Boolean> updateReimburse(@Valid @RequestBody OaReimburseSaveReqVO updateReqVO) {
        reimburseService.updateReimburse(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除报销单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:reimburse:delete')")
    public CommonResult<Boolean> deleteReimburse(@RequestParam("id") Long id) {
        reimburseService.deleteReimburse(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得报销单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('oa:reimburse:query')")
    public CommonResult<OaReimburseRespVO> getReimburse(@RequestParam("id") Long id) {
        OaReimburseDO reimburse = reimburseService.getReimburse(id);
        OaReimburseRespVO respVO = BeanUtils.toBean(reimburse, OaReimburseRespVO.class);
        if (respVO != null) {
            List<OaReimburseItemDO> items = reimburseItemMapper.selectListByReimburseId(id);
            respVO.setItems(BeanUtils.toBean(items, OaReimburseItemVO.class));
        }
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得报销单分页")
    @PreAuthorize("@ss.hasPermission('oa:reimburse:query')")
    public CommonResult<PageResult<OaReimburseRespVO>> getReimbursePage(@Valid OaReimbursePageReqVO pageReqVO) {
        PageResult<OaReimburseDO> pageResult = reimburseService.getReimbursePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, OaReimburseRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出报销单 Excel")
    @PreAuthorize("@ss.hasPermission('oa:reimburse:export')")
    public void exportReimburseExcel(@Valid OaReimbursePageReqVO exportReqVO,
                                     HttpServletResponse response) throws IOException {
        exportReqVO.setPageSize(PAGE_SIZE_NONE);
        List<OaReimburseDO> list = reimburseService.getReimbursePage(exportReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "报销单.xls", "数据", OaReimburseRespVO.class,
                BeanUtils.toBean(list, OaReimburseRespVO.class));
    }

    @PutMapping("/submit")
    @Operation(summary = "发起报销审批")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:reimburse:update')")
    public CommonResult<Boolean> submitReimburse(@RequestParam("id") Long id) {
        reimburseService.submitReimburse(id);
        return success(true);
    }

}
