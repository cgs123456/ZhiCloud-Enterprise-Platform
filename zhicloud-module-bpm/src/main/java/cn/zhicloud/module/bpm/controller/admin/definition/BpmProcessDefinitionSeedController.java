package cn.zhicloud.module.bpm.controller.admin.definition;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.module.bpm.service.definition.BpmProcessDefinitionSeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * 流程定义播种 Controller：给「代码里已按 KEY 发起、但引擎里还没有流程定义」的最小闭环补一个入口。
 *
 * <p>只作用于**当前租户**（不接受 tenantId 入参），避免一个接口能替任意租户部署流程。
 * 可播种的 KEY 是白名单，不做"任意 KEY"：把别的模块的多级审批流程静默播种成一个审批节点，
 * 等于悄悄改掉了那个业务的审批语义。
 *
 * @author 智云
 */
@Tag(name = "管理后台 - 流程定义播种")
@RestController
@RequestMapping("/bpm/process-definition-seed")
@Validated
public class BpmProcessDefinitionSeedController {

    /**
     * 允许播种的流程定义 KEY：报销、请假、合同审批、公文审批
     */
    private static final Set<String> SEEDABLE_KEYS = Set.of("oa_reimburse", "hr_leave_request",
            "crm-contract-audit", "oa_document");

    @Resource
    private BpmProcessDefinitionSeedService seedService;

    @PostMapping("/seed")
    @Operation(summary = "为当前租户播种最小流程定义（已存在激活定义时幂等返回，不重复部署）")
    @Parameter(name = "key", description = "流程定义标识", required = true, example = "oa_reimburse")
    @PreAuthorize("@ss.hasPermission('bpm:model:deploy')")
    public CommonResult<String> seed(@RequestParam("key") String key,
                                     @RequestParam(value = "name", required = false) String name) {
        if (!SEEDABLE_KEYS.contains(key)) {
            return CommonResult.error(400, "不支持播种的流程定义：" + key + "，可选：" + SEEDABLE_KEYS);
        }
        return success(seedService.seed(key, name, TenantContextHolder.getRequiredTenantId()));
    }

}
