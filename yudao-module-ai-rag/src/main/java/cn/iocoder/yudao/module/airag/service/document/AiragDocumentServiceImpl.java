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
    // @tx-ignore 这是跨存储（向量库 + MySQL）删除，@Transactional 管不到向量库，
    // 加上它只会造成「已加事务」的错觉。正确性由「删除顺序 + 失败即中止」保证，见下方说明。
    public void deleteDocument(Long id) {
        // 1. 校验存在
        validateDocumentExists(id);
        // 2. 先从向量库删除，失败则直接抛出、不再往下走
        //    一致性修复：此处原先用 try/catch 吞掉异常后照样执行第 3 步，
        //    结果是「向量没删掉，DB 记录却删了」——文档从后台消失但内容仍会被 RAG 召回，
        //    且清理向量所需的 chunkCount 随记录一起没了，形成无法回收的孤儿向量。
        //    （实际上内层 AiragRagServiceImpl 也在吞异常，这里的 catch 是死代码，两层已一并修复。）
        ragService.deleteDocument(id);
        // 3. 向量删除成功后再删文档记录
        //    顺序不可颠倒：DB 记录是定位向量的唯一线索，必须最后删。
        //    若本步失败，则「向量已删、记录仍在」，用户可重试；向量删除按确定性 chunkId 执行且幂等，
        //    重试无副作用，系统最终收敛到一致状态。
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
