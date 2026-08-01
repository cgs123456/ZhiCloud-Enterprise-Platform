package cn.iocoder.yudao.module.qms.controller.admin.electronicsignature;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.electronicsignature.vo.ElectronicSignatureLogPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.electronicsignature.vo.ElectronicSignatureLogRespVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.electronicsignature.ElectronicSignatureLogDO;
import cn.iocoder.yudao.module.qms.service.electronicsignature.ElectronicSignatureLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * QMS 电子签名记录 Controller
 *
 * @author 芋道源码
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
