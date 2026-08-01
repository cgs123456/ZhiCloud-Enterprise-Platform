package cn.iocoder.yudao.module.oa.service.document;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.oa.controller.admin.document.vo.OaDocumentAttachmentVO;
import cn.iocoder.yudao.module.oa.controller.admin.document.vo.OaDocumentPageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.document.vo.OaDocumentSaveReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.document.OaDocumentAttachmentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.document.OaDocumentDO;
import cn.iocoder.yudao.module.oa.dal.mysql.document.OaDocumentAttachmentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.document.OaDocumentMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.oa.enums.ErrorCodeConstants.OA_DOCUMENT_ARCHIVE_NO_REQUIRED;
import static cn.iocoder.yudao.module.oa.enums.ErrorCodeConstants.OA_DOCUMENT_NO_DUPLICATE;
import static cn.iocoder.yudao.module.oa.enums.ErrorCodeConstants.OA_DOCUMENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.oa.enums.ErrorCodeConstants.OA_DOCUMENT_REVIEW_OPINION_REQUIRED;
import static cn.iocoder.yudao.module.oa.enums.ErrorCodeConstants.OA_DOCUMENT_SIGN_OPINION_REQUIRED;
import static cn.iocoder.yudao.module.oa.enums.ErrorCodeConstants.OA_DOCUMENT_STATUS_INVALID;

/**
 * OA 公文 Service 实现类
 *
 * @author yudao
 */
@Service
@Validated
public class OaDocumentServiceImpl implements OaDocumentService {

    /**
     * 草稿状态
     */
    private static final int STATUS_DRAFT = 10;
    /**
     * 审核中状态
     */
    private static final int STATUS_REVIEWING = 20;
    /**
     * 已发布状态
     */
    private static final int STATUS_PUBLISHED = 30;
    /**
     * 已废止状态
     */
    private static final int STATUS_VOID = 40;
    /**
     * 待签发状态
     */
    private static final int STATUS_PENDING_SIGN = 25;
    /**
     * 已归档状态
     */
    private static final int STATUS_ARCHIVED = 50;

    /**
     * 公文审批对应的流程定义 KEY
     */
    public static final String PROCESS_KEY = "oa_document";

    @Resource
    private OaDocumentMapper documentMapper;
    @Resource
    private OaDocumentAttachmentMapper documentAttachmentMapper;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDocument(OaDocumentSaveReqVO createReqVO) {
        // 校验公文编号唯一
        validateNoUnique(null, createReqVO.getNo());
        // 插入公文（默认草稿状态）
        OaDocumentDO document = BeanUtils.toBean(createReqVO, OaDocumentDO.class);
        if (document.getStatus() == null) {
            document.setStatus(STATUS_DRAFT);
        }
        documentMapper.insert(document);
        // 插入公文附件
        saveAttachments(document.getId(), createReqVO.getAttachments());
        return document.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDocument(OaDocumentSaveReqVO updateReqVO) {
        // 校验存在 & 状态（仅草稿可修改）
        OaDocumentDO document = validateDocumentExists(updateReqVO.getId());
        if (!Integer.valueOf(STATUS_DRAFT).equals(document.getStatus())) {
            throw exception(OA_DOCUMENT_STATUS_INVALID);
        }
        // 校验公文编号唯一
        validateNoUnique(updateReqVO.getId(), updateReqVO.getNo());
        // 更新公文（P1 修复：屏蔽 status 字段，状态变更必须走 submit/reviewPass/sign/publish/archive 专门方法）
        OaDocumentDO updateObj = BeanUtils.toBean(updateReqVO, OaDocumentDO.class);
        updateObj.setStatus(null);
        documentMapper.updateById(updateObj);
        // 重建附件
        documentAttachmentMapper.deleteByDocumentId(updateReqVO.getId());
        saveAttachments(updateReqVO.getId(), updateReqVO.getAttachments());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long id) {
        validateDocumentExists(id);
        documentMapper.deleteById(id);
        documentAttachmentMapper.deleteByDocumentId(id);
    }

    @Override
    public OaDocumentDO getDocument(Long id) {
        return documentMapper.selectById(id);
    }

    @Override
    public PageResult<OaDocumentDO> getDocumentPage(OaDocumentPageReqVO pageReqVO) {
        return documentMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitDocument(Long id) {
        // 校验存在 & 状态（仅草稿可提交）
        OaDocumentDO document = validateDocumentExists(id);
        if (!Integer.valueOf(STATUS_DRAFT).equals(document.getStatus())) {
            throw exception(OA_DOCUMENT_STATUS_INVALID);
        }
        // 1. 先更新状态为审核中（BPM 调用失败时本地事务自动回滚，避免孤儿状态）
        OaDocumentDO preUpdate = new OaDocumentDO();
        preUpdate.setId(id);
        preUpdate.setStatus(STATUS_REVIEWING);
        documentMapper.updateById(preUpdate);
        // 2. 发起 BPM 流程
        Long userId = document.getIssuerUserId() != null
                ? document.getIssuerUserId()
                : SecurityFrameworkUtils.getLoginUserId();
        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put("documentId", id);
        if (document.getDocumentType() != null) {
            processInstanceVariables.put("documentType", document.getDocumentType());
        }
        if (document.getUrgency() != null) {
            processInstanceVariables.put("urgency", document.getUrgency());
        }
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(id)));
        // 3. 回填工作流编号（BPM 已成功创建，此处失败仅影响追溯，可通过 businessKey 反查）
        OaDocumentDO postUpdate = new OaDocumentDO();
        postUpdate.setId(id);
        postUpdate.setProcessInstanceId(processInstanceId);
        documentMapper.updateById(postUpdate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishDocument(Long id) {
        // 校验存在 & 状态（仅待签发可发布，强制走 20核稿→25签发→30发布 的状态机，禁止跳过签发）
        OaDocumentDO document = validateDocumentExists(id);
        if (!Integer.valueOf(STATUS_PENDING_SIGN).equals(document.getStatus())) {
            throw exception(OA_DOCUMENT_STATUS_INVALID);
        }
        updateStatus(id, STATUS_PUBLISHED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidDocument(Long id) {
        // 校验存在 & 状态（已发布或已归档均可废止，符合 30→40 / 50→40 状态机）
        OaDocumentDO document = validateDocumentExists(id);
        Integer status = document.getStatus();
        if (!Integer.valueOf(STATUS_PUBLISHED).equals(status) && !Integer.valueOf(STATUS_ARCHIVED).equals(status)) {
            throw exception(OA_DOCUMENT_STATUS_INVALID);
        }
        updateStatus(id, STATUS_VOID);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewPassDocument(Long id, String opinion) {
        // 校验存在 & 状态（仅核稿中可核稿通过）
        OaDocumentDO document = validateDocumentExists(id);
        if (!Integer.valueOf(STATUS_REVIEWING).equals(document.getStatus())) {
            throw exception(OA_DOCUMENT_STATUS_INVALID);
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        OaDocumentDO updateObj = new OaDocumentDO();
        updateObj.setId(id);
        updateObj.setReviewerUserId(userId);
        updateObj.setReviewerName(userId != null ? userId.toString() : null);
        updateObj.setReviewTime(LocalDateTime.now());
        updateObj.setReviewOpinion(opinion);
        updateObj.setStatus(STATUS_PENDING_SIGN);
        documentMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewRejectDocument(Long id, String opinion) {
        // 校验存在 & 状态（仅核稿中可核稿驳回）
        OaDocumentDO document = validateDocumentExists(id);
        if (!Integer.valueOf(STATUS_REVIEWING).equals(document.getStatus())) {
            throw exception(OA_DOCUMENT_STATUS_INVALID);
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        OaDocumentDO updateObj = new OaDocumentDO();
        updateObj.setId(id);
        updateObj.setReviewerUserId(userId);
        updateObj.setReviewerName(userId != null ? userId.toString() : null);
        updateObj.setReviewTime(LocalDateTime.now());
        updateObj.setReviewOpinion(opinion);
        updateObj.setStatus(STATUS_DRAFT);
        documentMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void signDocument(Long id, String opinion) {
        // 校验存在 & 状态（仅待签发可签发）
        OaDocumentDO document = validateDocumentExists(id);
        if (!Integer.valueOf(STATUS_PENDING_SIGN).equals(document.getStatus())) {
            throw exception(OA_DOCUMENT_STATUS_INVALID);
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        OaDocumentDO updateObj = new OaDocumentDO();
        updateObj.setId(id);
        updateObj.setSignerUserId(userId);
        updateObj.setSignerName(userId != null ? userId.toString() : null);
        updateObj.setSignTime(LocalDateTime.now());
        updateObj.setSignOpinion(opinion);
        updateObj.setStatus(STATUS_PUBLISHED);
        documentMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archiveDocument(Long id, String archiveNo) {
        // 校验归档编号非空
        if (StrUtil.isBlank(archiveNo)) {
            throw exception(OA_DOCUMENT_ARCHIVE_NO_REQUIRED);
        }
        // 校验存在 & 状态（仅已发布可归档）
        OaDocumentDO document = validateDocumentExists(id);
        if (!Integer.valueOf(STATUS_PUBLISHED).equals(document.getStatus())) {
            throw exception(OA_DOCUMENT_STATUS_INVALID);
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        OaDocumentDO updateObj = new OaDocumentDO();
        updateObj.setId(id);
        updateObj.setArchiverUserId(userId);
        updateObj.setArchiverName(userId != null ? userId.toString() : null);
        updateObj.setArchiveTime(LocalDateTime.now());
        updateObj.setArchiveNo(archiveNo);
        updateObj.setStatus(STATUS_ARCHIVED);
        documentMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementReadCount(Long id) {
        validateDocumentExists(id);
        // 原子更新，避免并发计数丢失
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<OaDocumentDO> updateWrapper =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<OaDocumentDO>()
                        .eq(OaDocumentDO::getId, id)
                        .setSql("read_count = COALESCE(read_count, 0) + 1");
        documentMapper.update(null, updateWrapper);
    }

    private void updateStatus(Long id, Integer status) {
        OaDocumentDO updateObj = new OaDocumentDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        documentMapper.updateById(updateObj);
    }

    private void validateNoUnique(Long id, String no) {
        if (no == null) {
            return;
        }
        OaDocumentDO document = documentMapper.selectByNo(no);
        if (document == null) {
            return;
        }
        if (id == null || !document.getId().equals(id)) {
            throw exception(OA_DOCUMENT_NO_DUPLICATE);
        }
    }

    private OaDocumentDO validateDocumentExists(Long id) {
        OaDocumentDO document = documentMapper.selectById(id);
        if (document == null) {
            throw exception(OA_DOCUMENT_NOT_EXISTS);
        }
        return document;
    }

    /**
     * 保存公文附件
     */
    private void saveAttachments(Long documentId, List<OaDocumentAttachmentVO> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        for (OaDocumentAttachmentVO attachment : attachments) {
            OaDocumentAttachmentDO attachmentDO = BeanUtils.toBean(attachment, OaDocumentAttachmentDO.class);
            attachmentDO.setId(null);
            attachmentDO.setDocumentId(documentId);
            documentAttachmentMapper.insert(attachmentDO);
        }
    }

}
