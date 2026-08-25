package cn.zhicloud.module.airag.service.knowledge;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.airag.controller.admin.knowledge.vo.AiragKnowledgePageReqVO;
import cn.zhicloud.module.airag.controller.admin.knowledge.vo.AiragKnowledgeSaveReqVO;
import cn.zhicloud.module.airag.dal.dataobject.AiragKnowledgeDO;
import cn.zhicloud.module.airag.dal.mysql.AiragKnowledgeMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.airag.enums.ErrorCodeConstants.KNOWLEDGE_NOT_EXISTS;

/**
 * AI RAG 知识库 Service 实现类
 *
 * @author zhicloud
 */
@Service
@Slf4j
public class AiragKnowledgeServiceImpl implements AiragKnowledgeService {

    @Resource
    private AiragKnowledgeMapper knowledgeMapper;

    @Override
    public Long createKnowledge(AiragKnowledgeSaveReqVO createReqVO) {
        AiragKnowledgeDO knowledge = BeanUtils.toBean(createReqVO, AiragKnowledgeDO.class);
        knowledgeMapper.insert(knowledge);
        return knowledge.getId();
    }

    @Override
    public void updateKnowledge(AiragKnowledgeSaveReqVO updateReqVO) {
        // 1. 校验存在
        validateKnowledgeExists(updateReqVO.getId());
        // 2. 更新
        AiragKnowledgeDO updateObj = BeanUtils.toBean(updateReqVO, AiragKnowledgeDO.class);
        knowledgeMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledge(Long id) {
        // 1. 校验存在
        validateKnowledgeExists(id);
        // 2. 删除知识库（关联文档由调用方/Controller 协调 AiragDocumentService 处理）
        knowledgeMapper.deleteById(id);
    }

    @Override
    public AiragKnowledgeDO getKnowledge(Long id) {
        return knowledgeMapper.selectById(id);
    }

    @Override
    public AiragKnowledgeDO validateKnowledgeExists(Long id) {
        AiragKnowledgeDO knowledge = knowledgeMapper.selectById(id);
        if (knowledge == null) {
            throw exception(KNOWLEDGE_NOT_EXISTS);
        }
        return knowledge;
    }

    @Override
    public PageResult<AiragKnowledgeDO> getKnowledgePage(AiragKnowledgePageReqVO pageReqVO) {
        return knowledgeMapper.selectPage(pageReqVO);
    }

    @Override
    public List<AiragKnowledgeDO> getKnowledgeListByStatus(Integer status) {
        return knowledgeMapper.selectListByStatus(status);
    }

}
