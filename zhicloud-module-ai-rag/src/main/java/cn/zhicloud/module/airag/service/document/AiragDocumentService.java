package cn.zhicloud.module.airag.service.document;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.airag.controller.admin.document.vo.AiragDocumentPageReqVO;
import cn.zhicloud.module.airag.controller.admin.document.vo.AiragDocumentUploadReqVO;
import cn.zhicloud.module.airag.dal.dataobject.AiragDocumentDO;

import java.util.List;

/**
 * AI RAG 文档 Service 接口
 *
 * @author zhicloud
 */
public interface AiragDocumentService {

    /**
     * 上传文档
     *
     * 流程：插入文档记录（状态=待处理） -> 异步触发 {@code AiragRagService#importDocument}
     *
     * @param uploadReqVO 上传信息
     * @return 文档编号
     */
    Long uploadDocument(AiragDocumentUploadReqVO uploadReqVO);

    /**
     * 删除文档
     *
     * 同时会从向量库中删除对应分块（若向量库启用）
     *
     * @param id 文档编号
     */
    void deleteDocument(Long id);

    /**
     * 获取文档
     *
     * @param id 文档编号
     * @return 文档
     */
    AiragDocumentDO getDocument(Long id);

    /**
     * 校验文档是否存在
     *
     * @param id 文档编号
     * @return 文档
     */
    AiragDocumentDO validateDocumentExists(Long id);

    /**
     * 获取文档分页
     *
     * @param pageReqVO 分页参数
     * @return 文档分页
     */
    PageResult<AiragDocumentDO> getDocumentPage(AiragDocumentPageReqVO pageReqVO);

    /**
     * 根据知识库编号获取文档列表
     *
     * @param knowledgeId 知识库编号
     * @return 文档列表
     */
    List<AiragDocumentDO> getDocumentListByKnowledgeId(Long knowledgeId);

}
