package cn.zhicloud.module.mes.controller.admin.pro.pda;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.mes.controller.admin.pro.feedback.vo.MesProFeedbackRespVO;
import cn.zhicloud.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.zhicloud.module.mes.service.pro.feedback.MesProFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;
import static cn.zhicloud.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 管理后台 - MES PDA 报工记录
 *
 * <p>只暴露当前登录用户的报工记录查询，供管理后台「MES PDA 报工」页面使用。
 * 写入、扫码等 PDA 现场操作仍只保留在 {@code controller.app} 的 APP API 下，
 * 避免把现场操作接口额外暴露到管理后台。
 */
@Tag(name = "管理后台 - MES PDA 报工记录")
@RestController
@RequestMapping("/mes-api/pro/feedback")
@Validated
public class MesProPdaFeedbackAdminController {

    @Resource
    private MesProFeedbackService feedbackService;

    @GetMapping("/my-feedback-list")
    @Operation(summary = "查询当前用户的报工记录")
    @PreAuthorize("@ss.hasPermission('mes:pda:feedback')")
    public CommonResult<List<MesProFeedbackRespVO>> myFeedbackList() {
        List<MesProFeedbackDO> list = feedbackService.getFeedbackListByUser(getLoginUserId());
        return success(BeanUtils.toBean(list, MesProFeedbackRespVO.class));
    }

}
