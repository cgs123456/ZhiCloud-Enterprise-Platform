package cn.zhicloud.module.crm.controller.admin.clue;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.module.crm.controller.admin.clue.vo.channel.CrmClueReceiveReqVO;
import cn.zhicloud.module.crm.service.clue.channel.CrmClueChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * CRM 线索接收 Controller（供外部系统调用）
 *
 * @author dhb52
 */
@Tag(name = "外部系统 - CRM 线索接收")
@RestController
@RequestMapping("/crm/clue-receive")
@Validated
public class CrmClueReceiveController {

    @Resource
    private CrmClueChannelService clueChannelService;

    @PostMapping("/external")
    @Operation(summary = "接收外部线索")
    @PermitAll // 无需登录，供外部系统调用
    public CommonResult<Long> receiveExternalClue(@Valid @RequestBody CrmClueReceiveReqVO reqVO) {
        return success(clueChannelService.receiveClue(reqVO));
    }

}
