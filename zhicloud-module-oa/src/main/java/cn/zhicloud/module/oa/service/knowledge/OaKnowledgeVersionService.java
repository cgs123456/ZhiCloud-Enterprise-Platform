package cn.zhicloud.module.oa.service.knowledge;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.oa.controller.admin.knowledge.vo.OaKnowledgeVersionPageReqVO;
import cn.zhicloud.module.oa.controller.admin.knowledge.vo.OaKnowledgeVersionSaveReqVO;
import cn.zhicloud.module.oa.dal.dataobject.knowledge.OaKnowledgeVersionDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * OA 知识库版本 Service 接口
 *
 * @author zhicloud
 */
public interface OaKnowledgeVersionService {

    /**
     * 创建知识库版本
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createVersion(@Valid OaKnowledgeVersionSaveReqVO createReqVO);

    /**
     * 更新知识库版本
     *
     * @param updateReqVO 更新信息
     */
    void updateVersion(@Valid OaKnowledgeVersionSaveReqVO updateReqVO);

    /**
     * 删除知识库版本
     *
     * @param id 编号
     */
    void deleteVersion(Long id);

    /**
     * 获得知识库版本
     *
     * @param id 编号
     * @return 知识库版本
     */
    OaKnowledgeVersionDO getVersion(Long id);

    /**
     * 获得知识库版本分页
     *
     * @param pageReqVO 分页查询
     * @return 知识库版本分页
     */
    PageResult<OaKnowledgeVersionDO> getVersionPage(OaKnowledgeVersionPageReqVO pageReqVO);

    /**
     * 获得某文章的所有版本列表
     *
     * @param articleId 文章 ID
     * @return 版本列表
     */
    List<OaKnowledgeVersionDO> getVersionListByArticleId(Long articleId);

}
