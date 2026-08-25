package cn.zhicloud.module.qms.service.document;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.qms.controller.admin.document.vo.QmsDocumentPageReqVO;
import cn.zhicloud.module.qms.controller.admin.document.vo.QmsDocumentSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.document.QmsDocumentDO;
import jakarta.validation.Valid;

/**
 * QMS 受控文档 Service 接口
 *
 * @author 智云
 */
public interface QmsDocumentService {

    /**
     * 创建受控文档（草稿）
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createDocument(@Valid QmsDocumentSaveReqVO createReqVO);

    /**
     * 更新受控文档
     *
     * @param updateReqVO 更新信息
     */
    void updateDocument(@Valid QmsDocumentSaveReqVO updateReqVO);

    /**
     * 删除受控文档
     *
     * @param id 编号
     */
    void deleteDocument(Long id);

    /**
     * 获得受控文档
     *
     * @param id 编号
     * @return 受控文档
     */
    QmsDocumentDO getDocument(Long id);

    /**
     * 获得受控文档分页
     *
     * @param pageReqVO 分页查询
     * @return 受控文档分页
     */
    PageResult<QmsDocumentDO> getDocumentPage(QmsDocumentPageReqVO pageReqVO);

    /**
     * 提交审核，状态 10 草稿 -> 20 待审
     *
     * @param id 编号
     */
    void submitDocument(Long id);

    /**
     * 审核通过并发布，状态 20 待审 -> 30 已发布，version + 1
     *
     * @param id      编号
     * @param fileUrl 文件 URL
     */
    void approveDocument(Long id, String fileUrl);

    /**
     * 审核驳回，状态 20 待审 -> 10 草稿
     *
     * @param id     编号
     * @param reason 驳回原因
     */
    void rejectDocument(Long id, String reason);

    /**
     * 作废文档，状态 30 已发布 -> 40 已作废
     *
     * @param id 编号
     */
    void revokeDocument(Long id);

}
