package cn.iocoder.yudao.module.qms.service.document;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.qms.controller.admin.document.vo.QmsDocumentChangeRequestPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.document.vo.QmsDocumentChangeRequestSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.document.QmsDocumentChangeRequestDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * QMS 文件变更申请 Service 接口
 *
 * @author 芋道源码
 */
public interface QmsDocumentChangeRequestService {

    /**
     * 创建变更申请
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createChangeRequest(@Valid QmsDocumentChangeRequestSaveReqVO createReqVO);

    /**
     * 更新变更申请
     *
     * @param updateReqVO 更新信息
     */
    void updateChangeRequest(@Valid QmsDocumentChangeRequestSaveReqVO updateReqVO);

    /**
     * 删除变更申请
     *
     * @param id 编号
     */
    void deleteChangeRequest(Long id);

    /**
     * 获得变更申请
     *
     * @param id 编号
     * @return 变更申请
     */
    QmsDocumentChangeRequestDO getChangeRequest(Long id);

    /**
     * 获得变更申请分页
     *
     * @param pageReqVO 分页查询
     * @return 变更申请分页
     */
    PageResult<QmsDocumentChangeRequestDO> getChangeRequestPage(QmsDocumentChangeRequestPageReqVO pageReqVO);

    /**
     * 审核通过，自动创建新版本文档
     *
     * @param id 编号
     */
    void approveChangeRequest(Long id);

    /**
     * 审核驳回
     *
     * @param id     编号
     * @param reason 驳回原因
     */
    void rejectChangeRequest(Long id, String reason);

    /**
     * 获得文档关联的变更申请列表
     *
     * @param documentId 受控文档 ID
     * @return 变更申请列表
     */
    List<QmsDocumentChangeRequestDO> getChangeRequestListByDocumentId(Long documentId);

}
