package cn.iocoder.yudao.module.qms.service.capa;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.capa.vo.CAPADocumentPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.capa.vo.CAPADocumentSaveReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.capa.vo.CAPAStageTransitionReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.capa.vo.CAPAVerificationReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.capa.CAPADocumentDO;
import cn.iocoder.yudao.module.qms.dal.mysql.capa.CAPADocumentMapper;
import cn.iocoder.yudao.module.qms.enums.qms.CAPAStageEnum;
import cn.iocoder.yudao.module.qms.enums.qms.CAPAStatusEnum;
import cn.iocoder.yudao.module.qms.enums.qms.CAPAVerificationResultEnum;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.*;

/**
 * QMS CAPA 文档 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class CAPADocumentServiceImpl implements CAPADocumentService {

    @Resource
    private CAPADocumentMapper capaDocumentMapper;

    @Override
    public Long createCAPADocument(CAPADocumentSaveReqVO createReqVO) {
        // 插入
        CAPADocumentDO capaDocument = BeanUtils.toBean(createReqVO, CAPADocumentDO.class);
        // 默认状态为待处理
        if (capaDocument.getStatus() == null) {
            capaDocument.setStatus(CAPAStatusEnum.OPEN.getStatus());
        }
        // P0-4：默认阶段为 CREATED，默认优先级为 MEDIUM
        if (capaDocument.getStage() == null) {
            capaDocument.setStage(CAPAStageEnum.CREATED.getStage());
        }
        capaDocumentMapper.insert(capaDocument);
        // 返回
        return capaDocument.getId();
    }

    @Override
    public void updateCAPADocument(CAPADocumentSaveReqVO updateReqVO) {
        // 校验存在
        validateCAPADocumentExists(updateReqVO.getId());
        // 更新
        CAPADocumentDO updateObj = BeanUtils.toBean(updateReqVO, CAPADocumentDO.class);
        // 禁止通过通用更新修改状态/阶段，状态变更必须走 transitionStage/closeCAPADocument 等状态流转方法
        updateObj.setStage(null);
        updateObj.setStatus(null);
        capaDocumentMapper.updateById(updateObj);
    }

    @Override
    public void deleteCAPADocument(Long id) {
        // 校验存在
        validateCAPADocumentExists(id);
        // 删除
        capaDocumentMapper.deleteById(id);
    }

    private void validateCAPADocumentExists(Long id) {
        if (capaDocumentMapper.selectById(id) == null) {
            throw exception(CAPA_DOCUMENT_NOT_EXISTS);
        }
    }

    @Override
    public CAPADocumentDO getCAPADocument(Long id) {
        return capaDocumentMapper.selectById(id);
    }

    @Override
    public PageResult<CAPADocumentDO> getCAPADocumentPage(CAPADocumentPageReqVO pageReqVO) {
        return capaDocumentMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeCAPADocument(Long id) {
        // 1. 校验存在
        CAPADocumentDO document = capaDocumentMapper.selectById(id);
        if (document == null) {
            throw exception(CAPA_DOCUMENT_NOT_EXISTS);
        }
        // 2. P0-4：必须处于 VERIFICATION 阶段且验证通过才能关闭
        if (!CAPAStageEnum.VERIFICATION.getStage().equals(document.getStage())) {
            throw exception(CAPA_DOCUMENT_NOT_CLOSE);
        }
        if (!CAPAVerificationResultEnum.PASSED.getResult().equals(document.getVerificationResult())) {
            throw exception(CAPA_DOCUMENT_VERIFY_RESULT_REQUIRED);
        }
        // 3. 更新为已关闭：stage=CLOSED、status=CLOSED、closeDate=now
        CAPADocumentDO updateObj = new CAPADocumentDO();
        updateObj.setId(id);
        updateObj.setStage(CAPAStageEnum.CLOSED.getStage());
        updateObj.setStatus(CAPAStatusEnum.CLOSED.getStatus());
        updateObj.setCloseDate(LocalDateTime.now());
        capaDocumentMapper.updateById(updateObj);
    }

    // ==================== P0-4 CAPA 全流程状态机 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transitionStage(CAPAStageTransitionReqVO reqVO) {
        // 1. 校验存在
        CAPADocumentDO document = capaDocumentMapper.selectById(reqVO.getId());
        if (document == null) {
            throw exception(CAPA_DOCUMENT_NOT_EXISTS);
        }
        // 2. 校验流转合法性
        Integer currentStage = document.getStage() == null
                ? CAPAStageEnum.CREATED.getStage() : document.getStage();
        Integer targetStage = reqVO.getTargetStage();
        if (!CAPAStageEnum.canTransition(currentStage, targetStage)) {
            throw exception(CAPA_DOCUMENT_STAGE_TRANSITION_INVALID, currentStage, targetStage);
        }
        // 3. 前进时校验必填字段
        if (targetStage > currentStage) {
            validateForwardTransition(document, currentStage, targetStage);
        }
        // 4. 更新阶段，并同步状态
        CAPAStatusEnum newStatus = resolveStatusByStage(targetStage);
        CAPADocumentDO updateObj = new CAPADocumentDO();
        updateObj.setId(document.getId());
        updateObj.setStage(targetStage);
        if (newStatus != null) {
            updateObj.setStatus(newStatus.getStatus());
        }
        // 若流转到 CLOSED，自动设置 closeDate
        if (CAPAStageEnum.CLOSED.getStage().equals(targetStage)) {
            updateObj.setCloseDate(LocalDateTime.now());
        }
        // 若从 VERIFICATION 回退到 CORRECTIVE_ACTION（验证不通过场景），清空验证结果
        if (CAPAStageEnum.VERIFICATION.getStage().equals(currentStage)
                && CAPAStageEnum.CORRECTIVE_ACTION.getStage().equals(targetStage)) {
            updateObj.setVerificationResult(null);
            updateObj.setVerificationComment(null);
            updateObj.setVerifiedBy(null);
            updateObj.setVerifiedTime(null);
        }
        capaDocumentMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitVerification(CAPAVerificationReqVO reqVO) {
        // 1. 校验存在
        CAPADocumentDO document = capaDocumentMapper.selectById(reqVO.getId());
        if (document == null) {
            throw exception(CAPA_DOCUMENT_NOT_EXISTS);
        }
        // 2. 校验阶段：必须是 VERIFICATION
        if (!CAPAStageEnum.VERIFICATION.getStage().equals(document.getStage())) {
            throw exception(CAPA_DOCUMENT_NOT_VERIFICATION_STAGE);
        }
        // 3. 更新验证结果
        CAPADocumentDO updateObj = new CAPADocumentDO();
        updateObj.setId(document.getId());
        updateObj.setVerificationResult(reqVO.getVerificationResult());
        updateObj.setVerificationComment(reqVO.getVerificationComment());
        updateObj.setVerifiedBy(reqVO.getVerifiedBy());
        updateObj.setVerifiedTime(LocalDateTime.now());
        capaDocumentMapper.updateById(updateObj);

        // 4. 若验证不通过，自动回退到 CORRECTIVE_ACTION 阶段重新走流程
        if (CAPAVerificationResultEnum.FAILED.getResult().equals(reqVO.getVerificationResult())) {
            CAPADocumentDO rollback = new CAPADocumentDO();
            rollback.setId(document.getId());
            rollback.setStage(CAPAStageEnum.CORRECTIVE_ACTION.getStage());
            rollback.setStatus(CAPAStatusEnum.OPEN.getStatus());
            capaDocumentMapper.updateById(rollback);
        }
    }

    /**
     * 前进流转时的必填字段校验
     */
    private void validateForwardTransition(CAPADocumentDO document, Integer currentStage, Integer targetStage) {
        // ROOT_CAUSE_ANALYSIS → CORRECTIVE_ACTION：rootCauseAnalysis 必填
        if (CAPAStageEnum.ROOT_CAUSE_ANALYSIS.getStage().equals(currentStage)
                && CAPAStageEnum.CORRECTIVE_ACTION.getStage().equals(targetStage)
                && StrUtil.isBlank(document.getRootCauseAnalysis())) {
            throw exception(CAPA_DOCUMENT_ROOT_CAUSE_REQUIRED);
        }
        // CORRECTIVE_ACTION → PREVENTIVE_ACTION：correctiveAction 必填
        if (CAPAStageEnum.CORRECTIVE_ACTION.getStage().equals(currentStage)
                && CAPAStageEnum.PREVENTIVE_ACTION.getStage().equals(targetStage)
                && StrUtil.isBlank(document.getCorrectiveAction())) {
            throw exception(CAPA_DOCUMENT_CORRECTIVE_ACTION_REQUIRED);
        }
        // PREVENTIVE_ACTION → VERIFICATION：preventiveAction 必填
        if (CAPAStageEnum.PREVENTIVE_ACTION.getStage().equals(currentStage)
                && CAPAStageEnum.VERIFICATION.getStage().equals(targetStage)
                && StrUtil.isBlank(document.getPreventiveAction())) {
            throw exception(CAPA_DOCUMENT_PREVENTIVE_ACTION_REQUIRED);
        }
        // VERIFICATION → CLOSED：verificationResult 必须为 PASSED
        if (CAPAStageEnum.VERIFICATION.getStage().equals(currentStage)
                && CAPAStageEnum.CLOSED.getStage().equals(targetStage)
                && !CAPAVerificationResultEnum.PASSED.getResult().equals(document.getVerificationResult())) {
            throw exception(CAPA_DOCUMENT_VERIFY_RESULT_REQUIRED);
        }
    }

    /**
     * 根据阶段映射 status 字段
     *
     * <p>状态映射规则：
     * <ul>
     *   <li>CREATED / ROOT_CAUSE_ANALYSIS / CORRECTIVE_ACTION / PREVENTIVE_ACTION → OPEN</li>
     *   <li>VERIFICATION → IN_PROGRESS</li>
     *   <li>CLOSED → CLOSED</li>
     * </ul>
     */
    private CAPAStatusEnum resolveStatusByStage(Integer targetStage) {
        if (CAPAStageEnum.VERIFICATION.getStage().equals(targetStage)) {
            return CAPAStatusEnum.IN_PROGRESS;
        }
        if (CAPAStageEnum.CLOSED.getStage().equals(targetStage)) {
            return CAPAStatusEnum.CLOSED;
        }
        // 其余阶段保持 OPEN
        return CAPAStatusEnum.OPEN;
    }

}
