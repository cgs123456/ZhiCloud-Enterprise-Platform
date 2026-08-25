package cn.zhicloud.module.oa.controller.admin.portal;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.module.oa.controller.admin.portal.vo.OaPortalDashboardVO;
import cn.zhicloud.module.oa.service.portal.OaPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - OA 工作台门户")
@RestController
@RequestMapping("/oa/portal")
@Validated
public class OaPortalController {

    @Resource
    private OaPortalService portalService;

    @GetMapping("/dashboard")
    @Operation(summary = "获取工作台门户数据")
    @PreAuthorize("@ss.hasPermission('oa:announcement:query')")
    public CommonResult<OaPortalDashboardVO> getDashboard() {
        return success(portalService.getDashboard());
    }

}
