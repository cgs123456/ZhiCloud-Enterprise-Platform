package cn.iocoder.yudao.module.qms.service.document;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.document.vo.QmsDocumentPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.document.vo.QmsDocumentSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.document.QmsDocumentDO;
import cn.iocoder.yudao.module.qms.dal.mysql.document.QmsDocumentMapper;
import cn.iocoder.yudao.module.qms.enums.document.QmsDocStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.DOCUMENT_NO_DUPLICATE;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.DOCUMENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.DOCUMENT_STATUS_INVALID;

/**
 * QMS 受控文档 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class QmsDocumentServiceImpl implements QmsDocumentService {

    @Resource
    private QmsDocumentMapper documentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDocument(QmsDocumentSaveReqVO createReqVO) {
        // 校验文件编号唯一
        validateDocNoUnique(null, createReqVO.getDocNo());
        // 插入
        QmsDocumentDO document = BeanUtils.toBean(createReqVO, QmsDocumentDO.class);
        // 默认状态为草稿
        if (document.getStatus() == null) {
            document.setStatus(QmsDocStatusEnum.DRAFT.getStatus());
        }
        // 默认版本号 1.0
        if (document.getVersion() == null) {
            document.setVersion("1.0");
        }
        documentMapper.insert(document);
        return document.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDocument(QmsDocumentSaveReqVO updateReqVO) {
        // 校验存在
        validateDocumentExists(updateReqVO.getId());
        // 校验文件编号唯一
        validateDocNoUnique(updateReqVO.getId(), updateReqVO.getDocNo());
        // 更新
        QmsDocumentDO updateObj = BeanUtils.toBean(updateReqVO, QmsDocumentDO.class);
        // 禁止通过通用更新修改状态，状态变更必须走 submitDocument/approveDocument/rejectDocument/revokeDocument 等状态流转方法
        updateObj.setStatus(null);
        documentMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long id) {
        // 校验存在
        validateDocumentExists(id);
        // 删除
        documentMapper.deleteById(id);
    }

    private void validateDocumentExists(Long id) {
        if (documentMapper.selectById(id) == null) {
            throw exception(DOCUMENT_NOT_EXISTS);
        }
    }

    private void validateDocNoUnique(Long id, String docNo) {
        QmsDocumentDO existing = documentMapper.selectByDocNo(docNo);
        if (existing == null) {
            return;
        }
        if (id == null || !id.equals(existing.getId())) {
            throw exception(DOCUMENT_NO_DUPLICATE);
        }
    }

    @Override
    public QmsDocumentDO getDocument(Long id) {
        return documentMapper.selectById(id);
    }

    @Override
    public PageResult<QmsDocumentDO> getDocumentPage(QmsDocumentPageReqVO pageReqVO) {
        return documentMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitDocument(Long id) {
        // 1. 校验存在
        QmsDocumentDO document = documentMapper.selectById(id);
        if (document == null) {
            throw exception(DOCUMENT_NOT_EXISTS);
        }
        // 2. 校验状态：必须是草稿
        if (!QmsDocStatusEnum.DRAFT.getStatus().equals(document.getStatus())) {
            throw exception(DOCUMENT_STATUS_INVALID);
        }
        // 3. 流转状态为待审
        QmsDocumentDO updateObj = new QmsDocumentDO();
        updateObj.setId(id);
        updateObj.setStatus(QmsDocStatusEnum.PENDING_APPROVAL.getStatus());
        documentMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveDocument(Long id, String fileUrl) {
        // 1. 校验存在
        QmsDocumentDO document = documentMapper.selectById(id);
        if (document == null) {
            throw exception(DOCUMENT_NOT_EXISTS);
        }
        // 2. 校验状态：必须是待审
        if (!QmsDocStatusEnum.PENDING_APPROVAL.getStatus().equals(document.getStatus())) {
            throw exception(DOCUMENT_STATUS_INVALID);
        }
        // 3. 流转状态为已发布，version + 1
        QmsDocumentDO updateObj = new QmsDocumentDO();
        updateObj.setId(id);
        updateObj.setStatus(QmsDocStatusEnum.PUBLISHED.getStatus());
        updateObj.setApproveDate(LocalDateTime.now());
        if (fileUrl != null) {
            updateObj.setFileUrl(fileUrl);
        }
        updateObj.setVersion(incrementVersion(document.getVersion()));
        documentMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectDocument(Long id, String reason) {
        // 1. 校验存在
        QmsDocumentDO document = documentMapper.selectById(id);
        if (document == null) {
            throw exception(DOCUMENT_NOT_EXISTS);
        }
        // 2. 校验状态：必须是待审
        if (!QmsDocStatusEnum.PENDING_APPROVAL.getStatus().equals(document.getStatus())) {
            throw exception(DOCUMENT_STATUS_INVALID);
        }
        // 3. 流转状态回草稿
        QmsDocumentDO updateObj = new QmsDocumentDO();
        updateObj.setId(id);
        updateObj.setStatus(QmsDocStatusEnum.DRAFT.getStatus());
        if (reason != null) {
            updateObj.setRemark(reason);
        }
        documentMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeDocument(Long id) {
        // 1. 校验存在
        QmsDocumentDO document = documentMapper.selectById(id);
        if (document == null) {
            throw exception(DOCUMENT_NOT_EXISTS);
        }
        // 2. 校验状态：必须是已发布
        if (!QmsDocStatusEnum.PUBLISHED.getStatus().equals(document.getStatus())) {
            throw exception(DOCUMENT_STATUS_INVALID);
        }
        // 3. 流转状态为已作废
        QmsDocumentDO updateObj = new QmsDocumentDO();
        updateObj.setId(id);
        updateObj.setStatus(QmsDocStatusEnum.OBSOLETE.getStatus());
        documentMapper.updateById(updateObj);
    }

    /**
     * 版本号自增：将 "1.0" -> "1.1"，"1.9" -> "1.10"，"2.3" -> "2.4"
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
