package cn.zhicloud.module.crm.service.contract.esign;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.crm.controller.admin.contract.vo.esign.CrmContractEsignRespVO;
import cn.zhicloud.module.crm.controller.admin.contract.vo.esign.CrmEsignCallbackReqVO;
import cn.zhicloud.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.zhicloud.module.crm.dal.dataobject.contract.CrmContractEsignTaskDO;
import cn.zhicloud.module.crm.dal.mysql.contract.CrmContractEsignTaskMapper;
import cn.zhicloud.module.crm.dal.mysql.contract.CrmContractMapper;
import cn.zhicloud.module.crm.enums.common.CrmAuditStatusEnum;
import cn.zhicloud.module.crm.enums.contract.CrmContractEsignStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import java.time.LocalDateTime;
import java.util.List;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.CONTRACT_ESIGN_ALREADY_SIGNED;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.CONTRACT_ESIGN_INIT_FAIL;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.CONTRACT_ESIGN_NOT_SIGNED;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.CONTRACT_ESIGN_STATUS_ILLEGAL;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.CONTRACT_ESIGN_TASK_ALREADY_EXISTS;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.CONTRACT_ESIGN_TASK_NOT_EXISTS;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.CONTRACT_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CrmContractEsignServiceImpl} 的单元测试（ADR 271 D8：任务真状态机）
 *
 * @author 智云
 */
@Import(CrmContractEsignServiceImpl.class)
public class CrmContractEsignServiceImplTest extends BaseDbUnitTest {

    @Resource
    private CrmContractEsignServiceImpl esignService;

    @Resource
    private CrmContractMapper contractMapper;

    @Resource
    private CrmContractEsignTaskMapper esignTaskMapper;

    // ========== initEsign ==========

    @Test
    public void testInitEsign_contractNotExists() {
        assertServiceException(() -> esignService.initEsign(randomLongId()), CONTRACT_ESIGN_INIT_FAIL);
    }

    @Test
    public void testInitEsign_success() {
        CrmContractDO contract = createContract();
        // 调用
        String taskNo = esignService.initEsign(contract.getId());
        // 任务号格式：ESIGN + 17 位时间片 + 3 位随机数
        assertTrue(taskNo.startsWith("ESIGN"));
        assertEquals(25, taskNo.length());
        // 任务行真落库（10 待签署）
        CrmContractEsignTaskDO task = esignTaskMapper.selectByTaskNo(taskNo);
        assertNotNull(task);
        assertEquals(contract.getId(), task.getContractId());
        assertEquals(CrmContractEsignStatusEnum.PENDING.getStatus(), task.getStatus());
        assertNull(task.getSignTime());
        // 合同指针回写为最新任务号
        assertEquals(taskNo, contractMapper.selectById(contract.getId()).getEsignTaskId());
    }

    @Test
    public void testInitEsign_pendingExists() {
        CrmContractDO contract = createContract();
        esignService.initEsign(contract.getId());
        assertServiceException(() -> esignService.initEsign(contract.getId()), CONTRACT_ESIGN_TASK_ALREADY_EXISTS);
    }

    @Test
    public void testInitEsign_signedExists() {
        CrmContractDO contract = createContract();
        createTask(contract.getId(), CrmContractEsignStatusEnum.SIGNED.getStatus(), "ESIGN-9901-SIGNED", null, null);
        assertServiceException(() -> esignService.initEsign(contract.getId()), CONTRACT_ESIGN_ALREADY_SIGNED);
    }

    @Test
    public void testInitEsign_rejectedAllowsReInit() {
        CrmContractDO contract = createContract();
        CrmContractEsignTaskDO oldTask = createTask(contract.getId(), CrmContractEsignStatusEnum.REJECTED.getStatus(),
                "ESIGN-9902-REJECTED", null, null);
        // 调用
        String taskNo = esignService.initEsign(contract.getId());
        // 新任务落库、旧任务保持已拒签
        assertNotEquals(oldTask.getTaskNo(), taskNo);
        assertEquals(CrmContractEsignStatusEnum.REJECTED.getStatus(),
                esignTaskMapper.selectById(oldTask.getId()).getStatus());
        assertEquals(CrmContractEsignStatusEnum.PENDING.getStatus(),
                esignTaskMapper.selectByTaskNo(taskNo).getStatus());
        // 指针指向新任务
        assertEquals(taskNo, contractMapper.selectById(contract.getId()).getEsignTaskId());
    }

    // ========== getEsignStatus ==========

    @Test
    public void testGetEsignStatus_taskNotExists() {
        assertServiceException(() -> esignService.getEsignStatus(""), CONTRACT_ESIGN_TASK_NOT_EXISTS);
        assertServiceException(() -> esignService.getEsignStatus("ESIGN-NOT-EXISTS-271"),
                CONTRACT_ESIGN_TASK_NOT_EXISTS);
    }

    @Test
    public void testGetEsignStatus_pending() {
        CrmContractDO contract = createContract();
        createTask(contract.getId(), CrmContractEsignStatusEnum.PENDING.getStatus(), "ESIGN-9903-PENDING", null, null);
        // 调用
        CrmContractEsignRespVO respVO = esignService.getEsignStatus("ESIGN-9903-PENDING");
        assertEquals("ESIGN-9903-PENDING", respVO.getEsignTaskId());
        assertEquals(contract.getId(), respVO.getContractId());
        assertEquals(CrmContractEsignStatusEnum.PENDING.getStatus(), respVO.getStatus());
        assertEquals("待签署", respVO.getStatusName());
        assertNull(respVO.getSignTime());
        assertTrue(respVO.getFileUrls().isEmpty());
    }

    @Test
    public void testGetEsignStatus_signed() {
        LocalDateTime signTime = LocalDateTime.of(2026, 10, 18, 12, 0, 0);
        CrmContractDO contract = createContract();
        createTask(contract.getId(), CrmContractEsignStatusEnum.SIGNED.getStatus(), "ESIGN-9904-SIGNED",
                signTime, List.of("https://example.com/signed-9904.pdf"));
        // 调用
        CrmContractEsignRespVO respVO = esignService.getEsignStatus("ESIGN-9904-SIGNED");
        assertEquals(CrmContractEsignStatusEnum.SIGNED.getStatus(), respVO.getStatus());
        assertEquals("已签署", respVO.getStatusName());
        assertEquals(signTime, respVO.getSignTime());
        assertEquals(List.of("https://example.com/signed-9904.pdf"), respVO.getFileUrls());
    }

    // ========== downloadSignedContract ==========

    @Test
    public void testDownload_contractNotExists() {
        assertServiceException(() -> esignService.downloadSignedContract(randomLongId()), CONTRACT_NOT_EXISTS);
    }

    @Test
    public void testDownload_taskNotExists() {
        CrmContractDO contract = createContract();
        assertServiceException(() -> esignService.downloadSignedContract(contract.getId()),
                CONTRACT_ESIGN_TASK_NOT_EXISTS);
    }

    @Test
    public void testDownload_notSigned() {
        CrmContractDO contract = createContract();
        createTask(contract.getId(), CrmContractEsignStatusEnum.PENDING.getStatus(), "ESIGN-9905-PENDING", null, null);
        assertServiceException(() -> esignService.downloadSignedContract(contract.getId()), CONTRACT_ESIGN_NOT_SIGNED);
    }

    @Test
    public void testDownload_signed() {
        CrmContractDO contract = createContract();
        createTask(contract.getId(), CrmContractEsignStatusEnum.SIGNED.getStatus(), "ESIGN-9906-SIGNED",
                LocalDateTime.of(2026, 10, 18, 12, 15, 0), List.of("https://example.com/signed-9906.pdf"));
        // 调用
        CrmContractEsignRespVO respVO = esignService.downloadSignedContract(contract.getId());
        assertEquals("ESIGN-9906-SIGNED", respVO.getEsignTaskId());
        assertEquals(CrmContractEsignStatusEnum.SIGNED.getStatus(), respVO.getStatus());
        assertEquals(List.of("https://example.com/signed-9906.pdf"), respVO.getFileUrls());
    }

    // ========== handleEsignCallback ==========

    @Test
    public void testCallback_taskNotExists() {
        CrmEsignCallbackReqVO req = buildCallback(randomLongId(), "ESIGN-NOT-EXISTS-271",
                CrmContractEsignStatusEnum.SIGNED.getStatus());
        assertServiceException(() -> esignService.handleEsignCallback(req), CONTRACT_ESIGN_TASK_NOT_EXISTS);
    }

    @Test
    public void testCallback_contractMismatch() {
        CrmContractDO contract = createContract();
        createTask(contract.getId(), CrmContractEsignStatusEnum.PENDING.getStatus(), "ESIGN-9907-PENDING", null, null);
        // 任务存在但归属另一合同：拒绝（防串用）
        CrmEsignCallbackReqVO req = buildCallback(randomLongId(), "ESIGN-9907-PENDING",
                CrmContractEsignStatusEnum.SIGNED.getStatus());
        assertServiceException(() -> esignService.handleEsignCallback(req), CONTRACT_ESIGN_TASK_NOT_EXISTS);
    }

    @Test
    public void testCallback_unknownStatus() {
        CrmContractDO contract = createContract();
        createTask(contract.getId(), CrmContractEsignStatusEnum.PENDING.getStatus(), "ESIGN-9908-PENDING", null, null);
        CrmEsignCallbackReqVO req = buildCallback(contract.getId(), "ESIGN-9908-PENDING", 99);
        assertServiceException(() -> esignService.handleEsignCallback(req), CONTRACT_ESIGN_STATUS_ILLEGAL);
    }

    @Test
    public void testCallback_signed_success() {
        LocalDateTime signTime = LocalDateTime.of(2026, 10, 18, 12, 30, 0);
        CrmContractDO contract = createContract();
        esignService.initEsign(contract.getId());
        String taskNo = contractMapper.selectById(contract.getId()).getEsignTaskId();
        // 调用：签署完成回调（带签署时间与签署后文件）
        CrmEsignCallbackReqVO req = buildCallback(contract.getId(), taskNo,
                CrmContractEsignStatusEnum.SIGNED.getStatus());
        req.setSignTime(signTime);
        req.setFileUrls(List.of("https://example.com/signed-9910.pdf"));
        esignService.handleEsignCallback(req);
        // 任务真迁移 10 -> 20
        CrmContractEsignTaskDO task = esignTaskMapper.selectByTaskNo(taskNo);
        assertEquals(CrmContractEsignStatusEnum.SIGNED.getStatus(), task.getStatus());
        assertEquals(signTime, task.getSignTime());
        assertEquals(List.of("https://example.com/signed-9910.pdf"), task.getSignedFileUrls());
        // 合同写回：§12.207 审批回写 + 指针恒为任务行 task_no
        CrmContractDO updatedContract = contractMapper.selectById(contract.getId());
        assertEquals(CrmAuditStatusEnum.APPROVE.getStatus(), updatedContract.getAuditStatus());
        assertEquals(taskNo, updatedContract.getEsignTaskId());
    }

    @Test
    public void testCallback_signed_idempotent() {
        CrmContractDO contract = createContract();
        esignService.initEsign(contract.getId());
        String taskNo = contractMapper.selectById(contract.getId()).getEsignTaskId();
        LocalDateTime firstSignTime = LocalDateTime.of(2026, 10, 18, 13, 0, 0);
        CrmEsignCallbackReqVO first = buildCallback(contract.getId(), taskNo,
                CrmContractEsignStatusEnum.SIGNED.getStatus());
        first.setSignTime(firstSignTime);
        esignService.handleEsignCallback(first);
        // 重复回调（携不同 signTime）：幂等 no-op，不改任何字段
        CrmEsignCallbackReqVO second = buildCallback(contract.getId(), taskNo,
                CrmContractEsignStatusEnum.SIGNED.getStatus());
        second.setSignTime(LocalDateTime.of(2026, 10, 18, 23, 29, 0));
        esignService.handleEsignCallback(second);
        assertEquals(firstSignTime, esignTaskMapper.selectByTaskNo(taskNo).getSignTime());
    }

    @Test
    public void testCallback_rejected_successAndIdempotent() {
        CrmContractDO contract = createContract();
        esignService.initEsign(contract.getId());
        String taskNo = contractMapper.selectById(contract.getId()).getEsignTaskId();
        Integer auditStatusBefore = contractMapper.selectById(contract.getId()).getAuditStatus();
        // 调用：拒签回调 10 -> 30
        esignService.handleEsignCallback(buildCallback(contract.getId(), taskNo,
                CrmContractEsignStatusEnum.REJECTED.getStatus()));
        assertEquals(CrmContractEsignStatusEnum.REJECTED.getStatus(),
                esignTaskMapper.selectByTaskNo(taskNo).getStatus());
        // 拒签不动合同审批状态
        assertEquals(auditStatusBefore, contractMapper.selectById(contract.getId()).getAuditStatus());
        // 同态重复幂等
        esignService.handleEsignCallback(buildCallback(contract.getId(), taskNo,
                CrmContractEsignStatusEnum.REJECTED.getStatus()));
        assertEquals(CrmContractEsignStatusEnum.REJECTED.getStatus(),
                esignTaskMapper.selectByTaskNo(taskNo).getStatus());
        // 已拒签不可再签（30 -> 20 非法）
        assertServiceException(() -> esignService.handleEsignCallback(
                buildCallback(contract.getId(), taskNo, CrmContractEsignStatusEnum.SIGNED.getStatus())),
                CONTRACT_ESIGN_STATUS_ILLEGAL);
    }

    @Test
    public void testCallback_signedThenRejected_illegal() {
        CrmContractDO contract = createContract();
        esignService.initEsign(contract.getId());
        String taskNo = contractMapper.selectById(contract.getId()).getEsignTaskId();
        esignService.handleEsignCallback(buildCallback(contract.getId(), taskNo,
                CrmContractEsignStatusEnum.SIGNED.getStatus()));
        // 已签署不可再拒签（20 -> 30 非法）
        assertServiceException(() -> esignService.handleEsignCallback(
                buildCallback(contract.getId(), taskNo, CrmContractEsignStatusEnum.REJECTED.getStatus())),
                CONTRACT_ESIGN_STATUS_ILLEGAL);
    }

    // ========== 辅助 ==========

    private CrmContractDO createContract() {
        CrmContractDO contract = randomPojo(CrmContractDO.class, o -> o.setEsignTaskId(null));
        contractMapper.insert(contract);
        return contract;
    }

    private CrmContractEsignTaskDO createTask(Long contractId, Integer status, String taskNo,
                                              LocalDateTime signTime, List<String> signedFileUrls) {
        CrmContractEsignTaskDO task = CrmContractEsignTaskDO.builder().contractId(contractId).taskNo(taskNo)
                .status(status).signTime(signTime).signedFileUrls(signedFileUrls).build();
        esignTaskMapper.insert(task);
        return task;
    }

    private CrmEsignCallbackReqVO buildCallback(Long contractId, String taskNo, Integer status) {
        CrmEsignCallbackReqVO req = new CrmEsignCallbackReqVO();
        req.setContractId(contractId);
        req.setEsignTaskId(taskNo);
        req.setStatus(status);
        return req;
    }

}
