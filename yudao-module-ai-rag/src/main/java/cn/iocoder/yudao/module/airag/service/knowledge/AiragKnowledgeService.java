package cn.iocoder.yudao.module.airag.service.knowledge;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.airag.controller.admin.knowledge.vo.AiragKnowledgePageReqVO;
import cn.iocoder.yudao.module.airag.controller.admin.knowledge.vo.AiragKnowledgeSaveReqVO;
import cn.iocoder.yudao.module.airag.dal.dataobject.AiragKnowledgeDO;

import java.util.List;

/**
 * AI RAG 知识库 Service 接口
 *
 * @author yudao
 */
public interface AiragKnowledgeService {

    /**
     * 创建知识库
     *
     * @param createReqVO 创建信息
     * @return 知识库编号
     */
    Long createKnowledge(AiragKnowledgeSaveReqVO createReqVO);

    /**
     * 更新知识库
     *
     * @param updateReqVO 更新信息
     */
    void updateKnowledge(AiragKnowledgeSaveReqVO updateReqVO);

    /**
     * 删除知识库
     *
     * @param id 知识库编号
     */
    void deleteKnowledge(Long id);

    /**
     * 获取知识库
     *
     * @param id 知识库编号
     * @return 知识库
     */
    AiragKnowledgeDO getKnowledge(Long id);

    /**
     * 校验知识库是否存在
     *
     * @param id 知识库编号
     * @return 知识库
     */
    AiragKnowledgeDO validateKnowledgeExists(Long id);

    /**
     * 获取知识库分页
     *
     * @param pageReqVO 分页参数
     * @return 知识库分页
     */
    PageResult<AiragKnowledgeDO> getKnowledgePage(AiragKnowledgePageReqVO pageReqVO);

    /**
     * 获取指定状态的知识库列表
     *
     * @param status 状态
     * @return 知识库列表
     */
    List<AiragKnowledgeDO> getKnowledgeListByStatus(Integer status);

}
