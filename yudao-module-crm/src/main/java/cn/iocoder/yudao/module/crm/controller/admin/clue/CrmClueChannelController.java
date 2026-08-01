package cn.iocoder.yudao.module.crm.controller.admin.clue;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.crm.controller.admin.clue.vo.channel.CrmClueChannelPageReqVO;
import cn.iocoder.yudao.module.crm.controller.admin.clue.vo.channel.CrmClueChannelRespVO;
import cn.iocoder.yudao.module.crm.controller.admin.clue.vo.channel.CrmClueChannelSaveReqVO;
import cn.iocoder.yudao.module.crm.service.clue.channel.CrmClueChannelDO;
import cn.iocoder.yudao.module.crm.service.clue.channel.CrmClueChannelService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.common.util.collection.MapUtils.findAndThen;

@Tag(name = "管理后台 - CRM 线索渠道")
@RestController
@RequestMapping("/crm/clue-channel")
@Validated
public class CrmClueChannelController {

    @Resource
    private CrmClueChannelService clueChannelService;

    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建线索渠道")
    @PreAuthorize("@ss.hasPermission('crm:clue-channel:create')")
    public CommonResult<Long> createClueChannel(@Valid @RequestBody CrmClueChannelSaveReqVO createReqVO) {
        return success(clueChannelService.createClueChannel(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新线索渠道")
    @PreAuthorize("@ss.hasPermission('crm:clue-channel:update')")
    public CommonResult<Boolean> updateClueChannel(@Valid @RequestBody CrmClueChannelSaveReqVO updateReqVO) {
        clueChannelService.updateClueChannel(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除线索渠道")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('crm:clue-channel:delete')")
    public CommonResult<Boolean> deleteClueChannel(@RequestParam("id") Long id) {
        clueChannelService.deleteClueChannel(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得线索渠道")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('crm:clue-channel:query')")
    public CommonResult<CrmClueChannelRespVO> getClueChannel(@RequestParam("id") Long id) {
        CrmClueChannelDO channel = clueChannelService.getClueChannel(id);
        if (channel == null) {
            return success(null);
        }
        CrmClueChannelRespVO respVO = BeanUtils.toBean(channel, CrmClueChannelRespVO.class);
        // 拼接自动分配人名称
        if (channel.getAutoAssignUserId() != null) {
            Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(Collections.singleton(channel.getAutoAssignUserId()));
            findAndThen(userMap, channel.getAutoAssignUserId(), user -> respVO.setAutoAssignUserName(user.getNickname()));
        }
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得线索渠道分页")
    @PreAuthorize("@ss.hasPermission('crm:clue-channel:query')")
    public CommonResult<PageResult<CrmClueChannelRespVO>> getClueChannelPage(@Valid CrmClueChannelPageReqVO pageVO) {
        PageResult<CrmClueChannelDO> pageResult = clueChannelService.getClueChannelPage(pageVO);
        PageResult<CrmClueChannelRespVO> result = BeanUtils.toBean(pageResult, CrmClueChannelRespVO.class);
        // 拼接自动分配人名称
        if (!result.getList().isEmpty()) {
            Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                    convertSet(pageResult.getList(), CrmClueChannelDO::getAutoAssignUserId));
            result.getList().forEach(vo -> findAndThen(userMap, vo.getAutoAssignUserId(),
                    user -> vo.setAutoAssignUserName(user.getNickname())));
        }
        return success(result);
    }

}
