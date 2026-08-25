package cn.zhicloud.module.qms.service.capa;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.controller.admin.capa.vo.CAPAStageTransitionReqVO;
import cn.zhicloud.module.qms.controller.admin.capa.vo.CAPAVerificationReqVO;
import cn.zhicloud.module.qms.dal.dataobject.capa.CAPADocumentDO;
import cn.zhicloud.module.qms.dal.mysql.capa.CAPADocumentMapper;
import cn.zhicloud.module.qms.enums.qms.CAPAStageEnum;
import cn.zhicloud.module.qms.enums.qms.CAPAStatusEnum;
import cn.zhicloud.module.qms.enums.qms.CAPAVerificationResultEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CAPADocumentServiceImpl} 的单元测试
 *
 * <p>覆盖 CAPA 全流程状态机：CREATED → ROOT_CAUSE_ANALYSIS → CORRECTIVE_ACTION
 * → PREVENTIVE_ACTION → VERIFICATION → CLOSED，含前进校验、回退、验证不通过回退。
 *
 * @author 智云
 */
@Import(CAPADocumentServiceImpl.class)
public class CAPADocumentServiceImplTest extends BaseDbUnitTest {

    @Resource
    private CAPADocumentServiceImpl capaService;

    @Resource
    private CAPADocumentMapper capaDocumentMapper;

    // ==================== 状态机前进测试 ====================

    @Test
    public void test_transitionStage_forward_createdToRootCause() {
        // 准备数据：CREATED 阶段
        CAPADocumentDO doc = randomPojo(CAPADocumentDO.class, o -> {
            o.setStage(CAPAStageEnum.CREATED.getStage());
            o.setStatus(CAPAStatusEnum.OPEN.getStatus());
        });
        capaDocumentMapper.insert(doc);

        // 调用：前进到 ROOT_CAUSE_ANALYSIS
        CAPAStageTransitionReqVO reqVO = new CAPAStageTransitionReqVO();
        reqVO.setId(doc.getId());
        reqVO.setTargetStage(CAPAStageEnum.ROOT_CAUSE_ANALYSIS.getStage());
        capaService.transitionStage(reqVO);

        // 断言
        CAPADocumentDO updated = capaDocumentMapper.selectById(doc.getId());
        assertEquals(CAPAStageEnum.ROOT_CAUSE_ANALYSIS.getStage(), updated.getStage());
        assertEquals(CAPAStatusEnum.OPEN.getStatus(), updated.getStatus());
    }

    @Test
    public void test_transitionStage_forward_rootCauseToCorrective() {
        // 准备数据：ROOT_CAUSE_ANALYSIS 阶段，已填 rootCauseAnalysis
        CAPADocumentDO doc = randomPojo(CAPADocumentDO.class, o -> {
            o.setStage(CAPAStageEnum.ROOT_CAUSE_ANALYSIS.getStage());
            o.setStatus(CAPAStatusEnum.OPEN.getStatus());
            o.setRootCauseAnalysis("根本原因是操作员未按SOP执行");
        });
        capaDocumentMapper.insert(doc);

        // 调用
        CAPAStageTransitionReqVO reqVO = new CAPAStageTransitionReqVO();
        reqVO.setId(doc.getId());
        reqVO.setTargetStage(CAPAStageEnum.CORRECTIVE_ACTION.getStage());
        capaService.transitionStage(reqVO);

        // 断言
        CAPADocumentDO updated = capaDocumentMapper.selectById(doc.getId());
        assertEquals(CAPAStageEnum.CORRECTIVE_ACTION.getStage(), updated.getStage());
    }

    @Test
    public void test_transitionStage_forward_rootCauseToCorrective_missingRootCause() {
        // 准备数据：ROOT_CAUSE_ANALYSIS 阶段，但 rootCauseAnalysis 为空
        CAPADocumentDO doc = randomPojo(CAPADocumentDO.class, o -> {
            o.setStage(CAPAStageEnum.ROOT_CAUSE_ANALYSIS.getStage());
            o.setStatus(CAPAStatusEnum.OPEN.getStatus());
            o.setRootCauseAnalysis(null);
        });
        capaDocumentMapper.insert(doc);

        // 调用：应抛异常（rootCauseAnalysis 必填）
        CAPAStageTransitionReqVO reqVO = new CAPAStageTransitionReqVO();
        reqVO.setId(doc.getId());
        reqVO.setTargetStage(CAPAStageEnum.CORRECTIVE_ACTION.getStage());
        assertThrows(Exception.class, () -> capaService.transitionStage(reqVO));
    }

    @Test
    public void test_transitionStage_forward_correctiveToPreventive_missingAction() {
        // 准备数据：CORRECTIVE_ACTION 阶段，但 correctiveAction 为空
        CAPADocumentDO doc = randomPojo(CAPADocumentDO.class, o -> {
            o.setStage(CAPAStageEnum.CORRECTIVE_ACTION.getStage());
            o.setStatus(CAPAStatusEnum.OPEN.getStatus());
            o.setCorrectiveAction(null);
        });
        capaDocumentMapper.insert(doc);

        // 调用：应抛异常
        CAPAStageTransitionReqVO reqVO = new CAPAStageTransitionReqVO();
        reqVO.setId(doc.getId());
        reqVO.setTargetStage(CAPAStageEnum.PREVENTIVE_ACTION.getStage());
        assertThrows(Exception.class, () -> capaService.transitionStage(reqVO));
    }

    @Test
    public void test_transitionStage_forward_preventiveToVerification() {
        // 准备数据：PREVENTIVE_ACTION 阶段，已填 preventiveAction
        CAPADocumentDO doc = randomPojo(CAPADocumentDO.class, o -> {
            o.setStage(CAPAStageEnum.PREVENTIVE_ACTION.getStage());
            o.setStatus(CAPAStatusEnum.OPEN.getStatus());
            o.setPreventiveAction("更新SOP并增加培训");
        });
        capaDocumentMapper.insert(doc);

        // 调用
        CAPAStageTransitionReqVO reqVO = new CAPAStageTransitionReqVO();
        reqVO.setId(doc.getId());
        reqVO.setTargetStage(CAPAStageEnum.VERIFICATION.getStage());
        capaService.transitionStage(reqVO);

        // 断言：阶段=VERIFICATION，状态=IN_PROGRESS
        CAPADocumentDO updated = capaDocumentMapper.selectById(doc.getId());
        assertEquals(CAPAStageEnum.VERIFICATION.getStage(), updated.getStage());
        assertEquals(CAPAStatusEnum.IN_PROGRESS.getStatus(), updated.getStatus());
    }

    @Test
    public void test_transitionStage_invalidJump() {
        // 准备数据：CREATED 阶段
        CAPADocumentDO doc = randomPojo(CAPADocumentDO.class, o -> {
            o.setStage(CAPAStageEnum.CREATED.getStage());
            o.setStatus(CAPAStatusEnum.OPEN.getStatus());
        });
        capaDocumentMapper.insert(doc);

        // 调用：跳2步（CREATED → CORRECTIVE_ACTION），应抛异常
        CAPAStageTransitionReqVO reqVO = new CAPAStageTransitionReqVO();
        reqVO.setId(doc.getId());
        reqVO.setTargetStage(CAPAStageEnum.CORRECTIVE_ACTION.getStage());
        assertThrows(Exception.class, () -> capaService.transitionStage(reqVO));
    }

    // ==================== 状态机回退测试 ====================

    @Test
    public void test_transitionStage_backward_verificationToCorrective() {
        // 准备数据：VERIFICATION 阶段
        CAPADocumentDO doc = randomPojo(CAPADocumentDO.class, o -> {
            o.setStage(CAPAStageEnum.VERIFICATION.getStage());
            o.setStatus(CAPAStatusEnum.IN_PROGRESS.getStatus());
            o.setVerificationResult(CAPAVerificationResultEnum.PENDING.getResult());
            o.setVerificationComment("待验证");
            o.setVerifiedBy("审查员A");
        });
        capaDocumentMapper.insert(doc);

        // 调用：回退到 CORRECTIVE_ACTION
        CAPAStageTransitionReqVO reqVO = new CAPAStageTransitionReqVO();
        reqVO.setId(doc.getId());
        reqVO.setTargetStage(CAPAStageEnum.CORRECTIVE_ACTION.getStage());
        capaService.transitionStage(reqVO);

        // 断言：阶段回退、状态=OPEN、验证结果清空
        CAPADocumentDO updated = capaDocumentMapper.selectById(doc.getId());
        assertEquals(CAPAStageEnum.CORRECTIVE_ACTION.getStage(), updated.getStage());
        assertEquals(CAPAStatusEnum.OPEN.getStatus(), updated.getStatus());
        assertNull(updated.getVerificationResult());
        assertNull(updated.getVerificationComment());
        assertNull(updated.getVerifiedBy());
    }

    // ==================== 验证提交测试 ====================

    @Test
    public void test_submitVerification_passed() {
        // 准备数据：VERIFICATION 阶段
        CAPADocumentDO doc = randomPojo(CAPADocumentDO.class, o -> {
            o.setStage(CAPAStageEnum.VERIFICATION.getStage());
            o.setStatus(CAPAStatusEnum.IN_PROGRESS.getStatus());
        });
        capaDocumentMapper.insert(doc);

        // 调用：提交验证通过
        CAPAVerificationReqVO reqVO = new CAPAVerificationReqVO();
        reqVO.setId(doc.getId());
        reqVO.setVerificationResult(CAPAVerificationResultEnum.PASSED.getResult());
        reqVO.setVerificationComment("措施有效，未再发生");
        reqVO.setVerifiedBy("验证员B");
        capaService.submitVerification(reqVO);

        // 断言：验证结果已记录，阶段不变
        CAPADocumentDO updated = capaDocumentMapper.selectById(doc.getId());
        assertEquals(CAPAVerificationResultEnum.PASSED.getResult(), updated.getVerificationResult());
        assertEquals("验证员B", updated.getVerifiedBy());
        assertNotNull(updated.getVerifiedTime());
    }

    @Test
    public void test_submitVerification_failed_autoRollback() {
        // 准备数据：VERIFICATION 阶段
        CAPADocumentDO doc = randomPojo(CAPADocumentDO.class, o -> {
            o.setStage(CAPAStageEnum.VERIFICATION.getStage());
            o.setStatus(CAPAStatusEnum.IN_PROGRESS.getStatus());
        });
        capaDocumentMapper.insert(doc);

        // 调用：提交验证不通过
        CAPAVerificationReqVO reqVO = new CAPAVerificationReqVO();
        reqVO.setId(doc.getId());
        reqVO.setVerificationResult(CAPAVerificationResultEnum.FAILED.getResult());
        reqVO.setVerificationComment("措施无效，问题复发");
        reqVO.setVerifiedBy("验证员B");
        capaService.submitVerification(reqVO);

        // 断言：自动回退到 CORRECTIVE_ACTION，状态=OPEN
        CAPADocumentDO updated = capaDocumentMapper.selectById(doc.getId());
        assertEquals(CAPAStageEnum.CORRECTIVE_ACTION.getStage(), updated.getStage());
        assertEquals(CAPAStatusEnum.OPEN.getStatus(), updated.getStatus());
    }

    @Test
    public void test_submitVerification_wrongStage() {
        // 准备数据：CREATED 阶段（非 VERIFICATION）
        CAPADocumentDO doc = randomPojo(CAPADocumentDO.class, o -> {
            o.setStage(CAPAStageEnum.CREATED.getStage());
            o.setStatus(CAPAStatusEnum.OPEN.getStatus());
        });
        capaDocumentMapper.insert(doc);

        // 调用：应抛异常
        CAPAVerificationReqVO reqVO = new CAPAVerificationReqVO();
        reqVO.setId(doc.getId());
        reqVO.setVerificationResult(CAPAVerificationResultEnum.PASSED.getResult());
        reqVO.setVerificationComment("验证");
        assertThrows(Exception.class, () -> capaService.submitVerification(reqVO));
    }

    // ==================== 关闭测试 ====================

    @Test
    public void test_closeCAPADocument_success() {
        // 准备数据：VERIFICATION 阶段且验证通过
        CAPADocumentDO doc = randomPojo(CAPADocumentDO.class, o -> {
            o.setStage(CAPAStageEnum.VERIFICATION.getStage());
            o.setStatus(CAPAStatusEnum.IN_PROGRESS.getStatus());
            o.setVerificationResult(CAPAVerificationResultEnum.PASSED.getResult());
        });
        capaDocumentMapper.insert(doc);

        // 调用
        capaService.closeCAPADocument(doc.getId());

        // 断言
        CAPADocumentDO updated = capaDocumentMapper.selectById(doc.getId());
        assertEquals(CAPAStageEnum.CLOSED.getStage(), updated.getStage());
        assertEquals(CAPAStatusEnum.CLOSED.getStatus(), updated.getStatus());
        assertNotNull(updated.getCloseDate());
    }

    @Test
    public void test_closeCAPADocument_notVerificationStage() {
        // 准备数据：CREATED 阶段（非 VERIFICATION）
        CAPADocumentDO doc = randomPojo(CAPADocumentDO.class, o -> {
            o.setStage(CAPAStageEnum.CREATED.getStage());
            o.setStatus(CAPAStatusEnum.OPEN.getStatus());
        });
        capaDocumentMapper.insert(doc);

        // 调用：应抛异常
        assertThrows(Exception.class, () -> capaService.closeCAPADocument(doc.getId()));
    }

    @Test
    public void test_closeCAPADocument_verificationNotPassed() {
        // 准备数据：VERIFICATION 阶段但验证结果为 FAILED
        CAPADocumentDO doc = randomPojo(CAPADocumentDO.class, o -> {
            o.setStage(CAPAStageEnum.VERIFICATION.getStage());
            o.setStatus(CAPAStatusEnum.IN_PROGRESS.getStatus());
            o.setVerificationResult(CAPAVerificationResultEnum.FAILED.getResult());
        });
        capaDocumentMapper.insert(doc);

        // 调用：应抛异常（验证不通过不能关闭）
        assertThrows(Exception.class, () -> capaService.closeCAPADocument(doc.getId()));
    }

    @Test
    public void test_closeCAPADocument_notExists() {
        assertThrows(Exception.class, () -> capaService.closeCAPADocument(randomLongId()));
    }

    // ==================== 创建默认值测试 ====================

    @Test
    public void test_createCAPADocument_defaultStageAndStatus() {
        // 准备数据
        cn.zhicloud.module.qms.controller.admin.capa.vo.CAPADocumentSaveReqVO createReqVO =
                new cn.zhicloud.module.qms.controller.admin.capa.vo.CAPADocumentSaveReqVO();
        createReqVO.setCapaNo("CAPA20240101001");
        // source 在 VO 上标注了 @NotNull，DB 列亦为 NOT NULL，必须显式赋值
        createReqVO.setSource(cn.zhicloud.module.qms.enums.qms.CAPASourceEnum.INTERNAL.getSource());
        createReqVO.setProblem("产品尺寸超差");

        // 调用
        Long id = capaService.createCAPADocument(createReqVO);

        // 断言：默认 stage=CREATED, status=OPEN
        CAPADocumentDO doc = capaDocumentMapper.selectById(id);
        assertNotNull(doc);
        assertEquals(CAPAStageEnum.CREATED.getStage(), doc.getStage());
        assertEquals(CAPAStatusEnum.OPEN.getStatus(), doc.getStatus());
    }

}
