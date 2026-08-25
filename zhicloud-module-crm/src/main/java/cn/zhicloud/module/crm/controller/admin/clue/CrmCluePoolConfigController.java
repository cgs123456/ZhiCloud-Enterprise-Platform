package cn.zhicloud.module.crm.controller.admin.clue;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.crm.controller.admin.clue.vo.poolconfig.CrmCluePoolConfigRespVO;
import cn.zhicloud.module.crm.controller.admin.clue.vo.poolconfig.CrmCluePoolConfigSaveReqVO;
import cn.zhicloud.module.crm.dal.dataobject.clue.CrmCluePoolConfigDO;
import cn.zhicloud.module.crm.service.clue.CrmCluePoolConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CRM 线索公海配置")
@RestController
@RequestMapping("/crm/clue-pool-config")
@Validated
public class CrmCluePoolConfigController {

    @Resource
    private CrmCluePoolConfigService cluePoolConfigService;

    @GetMapping("/get")
    @Operation(summary = "获取线索公海规则设置")
    @PreAuthorize("@ss.hasPermission('crm:clue-pool-config:query')")
    public CommonResult<CrmCluePoolConfigRespVO> getCluePoolConfig() {
        CrmCluePoolConfigDO poolConfig = cluePoolConfigService.getCluePoolConfig();
        return success(BeanUtils.toBean(poolConfig, CrmCluePoolConfigRespVO.class));
    }

    @PutMapping("/save")
    @Operation(summary = "更新线索公海规则设置")
    @PreAuthorize("@ss.hasPermission('crm:clue-pool-config:update')")
    public CommonResult<Boolean> saveCluePoolConfig(@Valid @RequestBody CrmCluePoolConfigSaveReqVO updateReqVO) {
        cluePoolConfigService.saveCluePoolConfig(updateReqVO);
        return success(true);
    }

}
