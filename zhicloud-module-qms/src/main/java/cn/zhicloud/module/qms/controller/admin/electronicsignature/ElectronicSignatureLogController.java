package cn.zhicloud.module.qms.controller.admin.electronicsignature;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.electronicsignature.vo.ElectronicSignatureLogPageReqVO;
import cn.zhicloud.module.qms.controller.admin.electronicsignature.vo.ElectronicSignatureLogRespVO;
import cn.zhicloud.module.qms.dal.dataobject.electronicsignature.ElectronicSignatureLogDO;
import cn.zhicloud.module.qms.service.electronicsignature.ElectronicSignatureLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * QMS 电子签名记录 Controller
 *
 * @author 智云
 */
@Tag(name = "管理后台 - QMS 电子签名记录")
@RestController
@RequestMapping("/qms/electronic-signature")
@Validated
public class ElectronicSignatureLogController {

    @Resource
    private ElectronicSignatureLogService electronicSignatureLogService;

    @GetMapping("/page")
    @Operation(summary = "获得电子签名记录分页")
    @PreAuthorize("@ss.hasPermission('qms:electronic-signature:query')")
    public CommonResult<PageResult<ElectronicSignatureLogRespVO>> getElectronicSignatureLogPage(
            @Valid ElectronicSignatureLogPageReqVO pageReqVO) {
        PageResult<ElectronicSignatureLogDO> pageResult = electronicSignatureLogService.getElectronicSignatureLogPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ElectronicSignatureLogRespVO.class));
    }

}
