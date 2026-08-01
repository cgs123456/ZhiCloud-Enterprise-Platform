package cn.iocoder.yudao.module.qms.service.document;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.document.vo.QmsDocumentChangeRequestPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.document.vo.QmsDocumentChangeRequestSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.document.QmsDocumentChangeRequestDO;
import cn.iocoder.yudao.module.qms.dal.dataobject.document.QmsDocumentDO;
import cn.iocoder.yudao.module.qms.dal.mysql.document.QmsDocumentChangeRequestMapper;
import cn.iocoder.yudao.module.qms.dal.mysql.document.QmsDocumentMapper;
import cn.iocoder.yudao.module.qms.enums.document.QmsChangeRequestStatusEnum;
import cn.iocoder.yudao.module.qms.enums.document.QmsDocChangeTypeEnum;
import cn.iocoder.yudao.module.qms.enums.document.QmsDocStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.DOCUMENT_CHANGE_REQUEST_NOT_EXISTS;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.DOCUMENT_CHANGE_REQUEST_STATUS_INVALID;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.DOCUMENT_NOT_EXISTS;

/**
 * QMS 文件变更申请 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class QmsDocumentChangeRequestServiceImpl implements QmsDocumentChangeRequestService {

    @Resource
    private QmsDocumentChangeRequestMapper changeRequestMapper;

    @Resource
    private QmsDocumentMapper documentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createChangeRequest(QmsDocumentChangeRequestSaveReqVO createReqVO) {
        // 校验文档存在
        QmsDocumentDO document = documentMapper.selectById(createReqVO.getDocumentId());
        if (document == null) {
            throw exception(DOCUMENT_NOT_EXISTS);
        }
        // 插入变更申请
        QmsDocumentChangeRequestDO changeRequest = BeanUtils.toBean(createReqVO, QmsDocumentChangeRequestDO.class);
        if (changeRequest.getStatus() == null) {
            changeRequest.setStatus(QmsChangeRequestStatusEnum.PENDING.getStatus());
        }
        if (changeRequest.getApplyDate() == null) {
            changeRequest.setApplyDate(LocalDate.now());
        }
        changeRequestMapper.insert(changeRequest);
        return changeRequest.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChangeRequest(QmsDocumentChangeRequestSaveReqVO updateReqVO) {
        // 校验存在
        validateChangeRequestExists(updateReqVO.getId());
        // 更新
        QmsDocumentChangeRequestDO updateObj = BeanUtils.toBean(updateReqVO, QmsDocumentChangeRequestDO.class);
        changeRequestMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChangeRequest(Long id) {
        // 校验存在
        validateChangeRequestExists(id);
        // 删除
        changeRequestMapper.deleteById(id);
    }

    private void validateChangeRequestExists(Long id) {
        if (changeRequestMapper.selectById(id) == null) {
            throw exception(DOCUMENT_CHANGE_REQUEST_NOT_EXISTS);
        }
    }

    @Override
    public QmsDocumentChangeRequestDO getChangeRequest(Long id) {
        return changeRequestMapper.selectById(id);
    }

    @Override
    public PageResult<QmsDocumentChangeRequestDO> getChangeRequestPage(QmsDocumentChangeRequestPageReqVO pageReqVO) {
        return changeRequestMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveChangeRequest(Long id) {
        // 1. 校验存在
        QmsDocumentChangeRequestDO changeRequest = changeRequestMapper.selectById(id);
        if (changeRequest == null) {
            throw exception(DOCUMENT_CHANGE_REQUEST_NOT_EXISTS);
        }
        // 2. 校验状态：必须是待审
        if (!QmsChangeRequestStatusEnum.PENDING.getStatus().equals(changeRequest.getStatus())) {
            throw exception(DOCUMENT_CHANGE_REQUEST_STATUS_INVALID);
        }
        // 3. 流转变更申请状态为已审
        QmsDocumentChangeRequestDO updateReq = new QmsDocumentChangeRequestDO();
        updateReq.setId(id);
        updateReq.setStatus(QmsChangeRequestStatusEnum.APPROVED.getStatus());
        updateReq.setApproveDate(LocalDateTime.now());
        changeRequestMapper.updateById(updateReq);
        // 4. 根据变更类型处理文档：修订->发布新版本文档；作废->将原文档置为已作废
        Integer changeType = changeRequest.getChangeType();
        if (QmsDocChangeTypeEnum.OBSOLETE.getChangeType().equals(changeType)) {
            // 作废原文档
            QmsDocumentDO updateDoc = new QmsDocumentDO();
            updateDoc.setId(changeRequest.getDocumentId());
            updateDoc.setStatus(QmsDocStatusEnum.OBSOLETE.getStatus());
            documentMapper.updateById(updateDoc);
        } else if (QmsDocChangeTypeEnum.REVISE.getChangeType().equals(changeType)
                || QmsDocChangeTypeEnum.CREATE.getChangeType().equals(changeType)) {
            // 修订/新增 -> 自动创建新版本文档（草稿），version 自增
            QmsDocumentDO origin = documentMapper.selectById(changeRequest.getDocumentId());
            if (origin != null) {
                QmsDocumentDO newDoc = new QmsDocumentDO();
                newDoc.setDocNo(origin.getDocNo());
                newDoc.setTitle(origin.getTitle());
                newDoc.setDocType(origin.getDocType());
                newDoc.setVersion(incrementVersion(origin.getVersion()));
                newDoc.setStatus(QmsDocStatusEnum.DRAFT.getStatus());
                newDoc.setOwnerDeptId(origin.getOwnerDeptId());
                newDoc.setFileUrl(origin.getFileUrl());
                newDoc.setSort(origin.getSort());
                documentMapper.insert(newDoc);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectChangeRequest(Long id, String reason) {
        // 1. 校验存在
        QmsDocumentChangeRequestDO changeRequest = changeRequestMapper.selectById(id);
        if (changeRequest == null) {
            throw exception(DOCUMENT_CHANGE_REQUEST_NOT_EXISTS);
        }
        // 2. 校验状态：必须是待审
        if (!QmsChangeRequestStatusEnum.PENDING.getStatus().equals(changeRequest.getStatus())) {
            throw exception(DOCUMENT_CHANGE_REQUEST_STATUS_INVALID);
        }
        // 3. 流转状态为已驳回
        QmsDocumentChangeRequestDO updateObj = new QmsDocumentChangeRequestDO();
        updateObj.setId(id);
        updateObj.setStatus(QmsChangeRequestStatusEnum.REJECTED.getStatus());
        updateObj.setApproveDate(LocalDateTime.now());
        if (reason != null) {
            updateObj.setRemark(reason);
        }
        changeRequestMapper.updateById(updateObj);
    }

    @Override
    public List<QmsDocumentChangeRequestDO> getChangeRequestListByDocumentId(Long documentId) {
        return changeRequestMapper.selectListByDocumentId(documentId);
    }

    /**
     * 版本号自增
     */
    private String incrementVersion(String version) {
        if (version == null || version.isEmpty()) {
            return "1.1";
        }
        int dotIdx = version.lastIndexOf('.');
        if (dotIdx < 0 || dotIdx == version.length() - 1) {
            return version + ".1";
        }
        String prefix = version.substring(0, dotIdx + 1);
        String suffix = version.substring(dotIdx + 1);
        try {
            int minor = Integer.parseInt(suffix);
            return prefix + (minor + 1);
        } catch (NumberFormatException e) {
            return version + ".1";
        }
    }

}
