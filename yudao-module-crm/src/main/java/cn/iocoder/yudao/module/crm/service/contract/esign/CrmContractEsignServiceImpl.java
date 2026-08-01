package cn.iocoder.yudao.module.crm.service.contract.esign;

import cn.iocoder.yudao.module.crm.controller.admin.contract.vo.esign.CrmContractEsignRespVO;
import cn.iocoder.yudao.module.crm.controller.admin.contract.vo.esign.CrmEsignCallbackReqVO;
import cn.iocoder.yudao.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.iocoder.yudao.module.crm.dal.mysql.contract.CrmContractMapper;
import cn.iocoder.yudao.module.crm.enums.common.CrmAuditStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.crm.enums.ErrorCodeConstants.CONTRACT_ESIGN_ALREADY_SIGNED;
import static cn.iocoder.yudao.module.crm.enums.ErrorCodeConstants.CONTRACT_ESIGN_INIT_FAIL;
import static cn.iocoder.yudao.module.crm.enums.ErrorCodeConstants.CONTRACT_ESIGN_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.crm.enums.ErrorCodeConstants.CONTRACT_NOT_EXISTS;

/**
 * CRM 合同电子签 Service 实现类（Stub 模式，不实际调用法大大 API）
 *
 * @author dhb52
 */
@Service
@Validated
@Slf4j
public class CrmContractEsignServiceImpl implements CrmContractEsignService {

    /**
     * 电子签状态 - 已签署
     */
    private static final Integer ESIGN_STATUS_SIGNED = 20;
    private static final String ESIGN_STATUS_SIGNED_NAME = "已签署";

    @Resource
    private CrmContractMapper contractMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String initEsign(Long contractId) {
        // 1. 校验合同存在
        CrmContractDO contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw exception(CONTRACT_ESIGN_INIT_FAIL);
        }
        // 2. 如果已有 esignTaskId，提示已签署
        if (contract.getEsignTaskId() != null) {
            throw exception(CONTRACT_ESIGN_ALREADY_SIGNED);
        }
        // 3. 生成模拟的 esignTaskId（"ESIGN" + timestamp）
        String esignTaskId = "ESIGN" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        // 4. 更新 CrmContractDO 的 esignTaskId 字段
        contractMapper.updateById(new CrmContractDO().setId(contractId).setEsignTaskId(esignTaskId));
        log.info("[initEsign][合同({}) 发起电子签，esignTaskId({})]", contractId, esignTaskId);
        return esignTaskId;
    }

    @Override
    public CrmContractEsignRespVO getEsignStatus(String esignTaskId) {
        // 1. 校验 esignTaskId 非空
        if (esignTaskId == null || esignTaskId.isEmpty()) {
            throw exception(CONTRACT_ESIGN_TASK_NOT_EXISTS);
        }
        // 2. Stub 模式：直接返回"已签署"
        CrmContractEsignRespVO respVO = new CrmContractEsignRespVO();
        respVO.setEsignTaskId(esignTaskId);
        respVO.setStatus(ESIGN_STATUS_SIGNED);
        respVO.setStatusName(ESIGN_STATUS_SIGNED_NAME);
        respVO.setSignTime(LocalDateTime.now());
        return respVO;
    }

    @Override
    public CrmContractEsignRespVO downloadSignedContract(Long contractId) {
        // 1. 校验合同存在
        CrmContractDO contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
        // 2. Stub 模式：返回原始 fileUrls
        CrmContractEsignRespVO respVO = new CrmContractEsignRespVO();
        respVO.setContractId(contractId);
        respVO.setEsignTaskId(contract.getEsignTaskId());
        respVO.setStatus(ESIGN_STATUS_SIGNED);
        respVO.setStatusName(ESIGN_STATUS_SIGNED_NAME);
        respVO.setFileUrls(contract.getFileUrls() != null ? contract.getFileUrls() : Collections.emptyList());
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleEsignCallback(CrmEsignCallbackReqVO req) {
        // 1. 校验合同存在
        CrmContractDO contract = contractMapper.selectById(req.getContractId());
        if (contract == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
        // 2. 更新合同状态（回调中状态为已签署时，更新审批状态为审核通过）
        if (ESIGN_STATUS_SIGNED.equals(req.getStatus())) {
            contractMapper.updateById(new CrmContractDO().setId(req.getContractId())
                    .setEsignTaskId(req.getEsignTaskId())
                    .setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus()));
            log.info("[handleEsignCallback][合同({}) 电子签回调，esignTaskId({}) 已签署]", req.getContractId(), req.getEsignTaskId());
        } else {
            log.info("[handleEsignCallback][合同({}) 电子签回调，status({})]", req.getContractId(), req.getStatus());
        }
    }

}
