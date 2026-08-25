package cn.zhicloud.module.qms.service.electronicsignature;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.qms.controller.admin.electronicsignature.vo.ElectronicSignatureLogPageReqVO;
import cn.zhicloud.module.qms.dal.dataobject.electronicsignature.ElectronicSignatureLogDO;
import cn.zhicloud.module.qms.dal.mysql.electronicsignature.ElectronicSignatureLogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

/**
 * QMS 电子签名记录 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class ElectronicSignatureLogServiceImpl implements ElectronicSignatureLogService {

    @Resource
    private ElectronicSignatureLogMapper electronicSignatureLogMapper;

    /**
     * {@inheritDoc}
     *
     * <p><b>传播行为必须是 REQUIRES_NEW</b>：签名日志由 {@code ElectronicSignatureAspect} 在放行业务方法
     * <i>之前</i>写入。若沿用默认的 REQUIRED，日志就会加入业务事务——一旦业务方法随后抛异常回滚，
     * 这条签名记录会被一并回滚，审计轨迹里将完全看不到「谁在何时尝试签署了什么」。
     * 而 21 CFR Part 11 §11.10(e) 要求审计追踪记录全部操作尝试（含失败），不得因业务失败而消失。
     * 故此处开独立事务，使签名事实与业务结果解耦。
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
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
