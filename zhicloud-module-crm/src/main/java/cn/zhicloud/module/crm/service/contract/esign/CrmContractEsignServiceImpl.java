package cn.zhicloud.module.crm.service.contract.esign;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.zhicloud.module.crm.controller.admin.contract.vo.esign.CrmContractEsignRespVO;
import cn.zhicloud.module.crm.controller.admin.contract.vo.esign.CrmEsignCallbackReqVO;
import cn.zhicloud.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.zhicloud.module.crm.dal.dataobject.contract.CrmContractEsignTaskDO;
import cn.zhicloud.module.crm.dal.mysql.contract.CrmContractEsignTaskMapper;
import cn.zhicloud.module.crm.dal.mysql.contract.CrmContractMapper;
import cn.zhicloud.module.crm.enums.common.CrmAuditStatusEnum;
import cn.zhicloud.module.crm.enums.contract.CrmContractEsignStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.CONTRACT_ESIGN_ALREADY_SIGNED;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.CONTRACT_ESIGN_INIT_FAIL;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.CONTRACT_ESIGN_NOT_SIGNED;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.CONTRACT_ESIGN_STATUS_ILLEGAL;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.CONTRACT_ESIGN_TASK_ALREADY_EXISTS;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.CONTRACT_ESIGN_TASK_NOT_EXISTS;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.CONTRACT_NOT_EXISTS;

/**
 * CRM 合同电子签 Service 实现类（任务真状态机，ADR 271 D3）
 *
 * 真值载体＝crm_contract_esign_task；crm_contract.esign_task_id 仅为「最新任务号」缓存指针。
 * 第三方平台调用（法大大/签章 API）与回调验签/公网网关属运维接线面（ADR 271 D5，不扩面）。
 *
 * @author dhb52
 */
@Service
@Validated
@Slf4j
public class CrmContractEsignServiceImpl implements CrmContractEsignService {

    /**
     * 任务号时间片格式：yyyyMMddHHmmssSSS（ADR 271 D3：ESIGN+时间片+3 位随机数）
     */
    private static final DateTimeFormatter TASK_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Resource
    private CrmContractMapper contractMapper;

    @Resource
    private CrmContractEsignTaskMapper esignTaskMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String initEsign(Long contractId) {
        // 1. 校验合同存在
        CrmContractDO contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw exception(CONTRACT_ESIGN_INIT_FAIL);
        }
        // 2. 按最新任务状态判定可否发起：进行中拒（003）/ 已签署拒（002）/ 已拒签可重新发起（ADR 271 D3）
        CrmContractEsignTaskDO latestTask = esignTaskMapper.selectLatestByContractId(contractId);
        if (latestTask != null) {
            if (CrmContractEsignStatusEnum.PENDING.getStatus().equals(latestTask.getStatus())) {
                throw exception(CONTRACT_ESIGN_TASK_ALREADY_EXISTS);
            }
            if (CrmContractEsignStatusEnum.SIGNED.getStatus().equals(latestTask.getStatus())) {
                throw exception(CONTRACT_ESIGN_ALREADY_SIGNED);
            }
        }
        // 3. 新建任务（10 待签署）并回写合同「最新任务号」指针
        String taskNo = "ESIGN" + LocalDateTime.now().format(TASK_NO_FORMATTER) + RandomUtil.randomNumbers(3);
        esignTaskMapper.insert(new CrmContractEsignTaskDO().setContractId(contractId).setTaskNo(taskNo)
                .setStatus(CrmContractEsignStatusEnum.PENDING.getStatus()));
        contractMapper.updateById(new CrmContractDO().setId(contractId).setEsignTaskId(taskNo));
        log.info("[initEsign][合同({}) 发起电子签，taskNo({})]", contractId, taskNo);
        return taskNo;
    }

    @Override
    public CrmContractEsignRespVO getEsignStatus(String esignTaskId) {
        // 1. 任务必须真实存在（空/未知一律 001；不再返回「假已签署」——ADR 271 缺陷 B 修复）
        if (StrUtil.isEmpty(esignTaskId)) {
            throw exception(CONTRACT_ESIGN_TASK_NOT_EXISTS);
        }
        CrmContractEsignTaskDO task = esignTaskMapper.selectByTaskNo(esignTaskId);
        if (task == null) {
            throw exception(CONTRACT_ESIGN_TASK_NOT_EXISTS);
        }
        return buildResp(task);
    }

    @Override
    public CrmContractEsignRespVO downloadSignedContract(Long contractId) {
        // 1. 校验合同存在
        CrmContractDO contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
        // 2. 最新任务必须存在且已签署（未签署不给「假签署文件」——ADR 271 缺陷 C 修复）
        CrmContractEsignTaskDO task = esignTaskMapper.selectLatestByContractId(contractId);
        if (task == null) {
            throw exception(CONTRACT_ESIGN_TASK_NOT_EXISTS);
        }
        if (!CrmContractEsignStatusEnum.SIGNED.getStatus().equals(task.getStatus())) {
            throw exception(CONTRACT_ESIGN_NOT_SIGNED);
        }
        return buildResp(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleEsignCallback(CrmEsignCallbackReqVO req) {
        // 1. 任务必须存在 + 归属一致（防 (taskId, contractId) 串用——ADR 271 缺陷 D 修复）
        CrmContractEsignTaskDO task = esignTaskMapper.selectByTaskNo(req.getEsignTaskId());
        if (task == null || !task.getContractId().equals(req.getContractId())) {
            throw exception(CONTRACT_ESIGN_TASK_NOT_EXISTS);
        }
        Integer signedStatus = CrmContractEsignStatusEnum.SIGNED.getStatus();
        Integer rejectedStatus = CrmContractEsignStatusEnum.REJECTED.getStatus();
        // 2. 终态幂等：同态重复回调成功 no-op；跨终态非法（ADR 271 D3）
        if (signedStatus.equals(task.getStatus()) || rejectedStatus.equals(task.getStatus())) {
            if (task.getStatus().equals(req.getStatus())) {
                log.info("[handleEsignCallback][任务({}) 已处于状态({})，重复回调幂等跳过]", task.getTaskNo(), task.getStatus());
                return;
            }
            throw exception(CONTRACT_ESIGN_STATUS_ILLEGAL);
        }
        // 3. 状态机（仅 10 待签署可迁移）
        if (signedStatus.equals(req.getStatus())) { // 10 -> 20
            LocalDateTime signTime = req.getSignTime() != null ? req.getSignTime() : LocalDateTime.now();
            esignTaskMapper.updateById(new CrmContractEsignTaskDO().setId(task.getId())
                    .setStatus(signedStatus).setSignTime(signTime).setSignedFileUrls(req.getFileUrls()));
            // §12.207 审批回写语义保留：签署完成 → 合同审批状态置为审核通过；指针回写恒为任务行 task_no（不可被回调入参篡改）
            contractMapper.updateById(new CrmContractDO().setId(task.getContractId())
                    .setEsignTaskId(task.getTaskNo()).setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus()));
            log.info("[handleEsignCallback][合同({}) 电子签签署完成，taskNo({})]", task.getContractId(), task.getTaskNo());
        } else if (rejectedStatus.equals(req.getStatus())) { // 10 -> 30
            esignTaskMapper.updateById(new CrmContractEsignTaskDO().setId(task.getId()).setStatus(rejectedStatus));
            log.info("[handleEsignCallback][合同({}) 电子签已拒签，taskNo({})]", task.getContractId(), task.getTaskNo());
        } else {
            // 未知状态值：不再静默丢弃（ADR 271 缺陷 D 修复）
            throw exception(CONTRACT_ESIGN_STATUS_ILLEGAL);
        }
    }

    /**
     * 任务行 → 响应 VO（状态名走枚举；已签文件缺省空列表）
     */
    private CrmContractEsignRespVO buildResp(CrmContractEsignTaskDO task) {
        CrmContractEsignRespVO respVO = new CrmContractEsignRespVO();
        respVO.setEsignTaskId(task.getTaskNo());
        respVO.setContractId(task.getContractId());
        respVO.setStatus(task.getStatus());
        respVO.setStatusName(CrmContractEsignStatusEnum.getName(task.getStatus()));
        respVO.setSignTime(task.getSignTime());
        respVO.setFileUrls(task.getSignedFileUrls() != null ? task.getSignedFileUrls() : Collections.emptyList());
        return respVO;
    }

}
