package cn.iocoder.yudao.module.airag.service.document;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.airag.controller.admin.document.vo.AiragDocumentPageReqVO;
import cn.iocoder.yudao.module.airag.controller.admin.document.vo.AiragDocumentUploadReqVO;
import cn.iocoder.yudao.module.airag.dal.dataobject.AiragDocumentDO;
import cn.iocoder.yudao.module.airag.dal.mysql.AiragDocumentMapper;
import cn.iocoder.yudao.module.airag.service.rag.AiragRagService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.airag.enums.ErrorCodeConstants.DOCUMENT_NOT_EXISTS;

/**
 * AI RAG 文档 Service 实现类
 *
 * @author yudao
 */
@Service
@Slf4j
public class AiragDocumentServiceImpl implements AiragDocumentService {

    /**
     * 文档处理状态
     */
    private static final int STATUS_PENDING = 0;

    @Resource
    private AiragDocumentMapper documentMapper;

    @Resource
    @Lazy // 延迟加载，避免与 AiragRagService 之间的循环依赖
    private AiragRagService ragService;

    @Override
    public Long uploadDocument(AiragDocumentUploadReqVO uploadReqVO) {
        // 1. 插入文档记录（初始状态=待处理）
        AiragDocumentDO document = BeanUtils.toBean(uploadReqVO, AiragDocumentDO.class)
                .setStatus(STATUS_PENDING)
                .setChunkCount(0);
        documentMapper.insert(document);

        // 2. 异步触发向量化导入（@Async 在 AiragRagService 接口上，通过 Spring 代理生效）
        ragService.importDocument(document.getKnowledgeId(), document.getId());
        return document.getId();
    }

    @Override
    public void deleteDocument(Long id) {
        // 1. 校验存在
        AiragDocumentDO document = validateDocumentExists(id);
        // 2. 从向量库删除（若启用，内部做空判断与异常兜底）
        try {
            ragService.deleteDocument(id);
        } catch (Exception e) {
            log.warn("[deleteDocument][从向量库删除文档失败，documentId={}，原因={}]", id, e.getMessage());
        }
        // 3. 删除文档记录
        documentMapper.deleteById(id);
    }

    @Override
    public AiragDocumentDO getDocument(Long id) {
        return documentMapper.selectById(id);
    }

    @Override
    public AiragDocumentDO validateDocumentExists(Long id) {
        AiragDocumentDO document = documentMapper.selectById(id);
        if (document == null) {
            throw exception(DOCUMENT_NOT_EXISTS);
        }
        return document;
    }

    @Override
    public PageResult<AiragDocumentDO> getDocumentPage(AiragDocumentPageReqVO pageReqVO) {
        return documentMapper.selectPage(pageReqVO);
    }

    @Override
    public List<AiragDocumentDO> getDocumentListByKnowledgeId(Long knowledgeId) {
        return documentMapper.selectListByKnowledgeId(knowledgeId);
    }

}
