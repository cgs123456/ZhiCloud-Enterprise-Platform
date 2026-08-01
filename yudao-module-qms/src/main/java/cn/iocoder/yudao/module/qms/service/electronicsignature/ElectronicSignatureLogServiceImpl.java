package cn.iocoder.yudao.module.qms.service.electronicsignature;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.qms.controller.admin.electronicsignature.vo.ElectronicSignatureLogPageReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.electronicsignature.ElectronicSignatureLogDO;
import cn.iocoder.yudao.module.qms.dal.mysql.electronicsignature.ElectronicSignatureLogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

/**
 * QMS 电子签名记录 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ElectronicSignatureLogServiceImpl implements ElectronicSignatureLogService {

    @Resource
    private ElectronicSignatureLogMapper electronicSignatureLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordLog(Long userId, String signatureMeaning, String operationType,
                          String operationContent, String ipAddress, String remark) {
        ElectronicSignatureLogDO logDO = ElectronicSignatureLogDO.builder()
                .userId(userId)
                .signatureMeaning(signatureMeaning)
                .operationType(operationType)
                .operationContent(operationContent)
                .signatureTime(LocalDateTime.now())
                .ipAddress(ipAddress)
                .remark(remark)
                .build();
        electronicSignatureLogMapper.insert(logDO);
    }

    @Override
    public PageResult<ElectronicSignatureLogDO> getElectronicSignatureLogPage(ElectronicSignatureLogPageReqVO pageReqVO) {
        return electronicSignatureLogMapper.selectPage(pageReqVO);
    }

}
