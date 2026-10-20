package cn.zhicloud.module.wms.controller.admin.pda;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.module.wms.controller.app.pda.vo.WmsPdaTaskRespVO;
import cn.zhicloud.module.wms.service.pda.WmsPdaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - WMS PDA 作业
 *
 * <p>只暴露 PDA 待执行任务的只读查询，供管理后台「PDA 作业」页面使用。
 * 扫码、收货、上架等现场操作仍只保留在 {@code controller.app} 的 APP API 下，
 * 避免把现场写操作接口额外暴露到管理后台。
 */
@Tag(name = "管理后台 - WMS PDA 作业")
@RestController
@RequestMapping("/wms/pda")
@Validated
public class WmsPdaAdminController {

    @Resource
    private WmsPdaService pdaService;

    @GetMapping("/task/list")
    @Operation(summary = "获取待执行任务（拣货/上架/盘点）")
    public CommonResult<List<WmsPdaTaskRespVO>> getTaskList() {
        return success(pdaService.getTaskList());
    }

}
