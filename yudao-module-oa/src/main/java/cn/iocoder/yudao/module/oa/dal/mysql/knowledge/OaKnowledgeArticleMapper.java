package cn.iocoder.yudao.module.oa.dal.mysql.knowledge;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeArticlePageReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.knowledge.OaKnowledgeArticleDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * OA 知识库文章 Mapper
 *
 * @author yudao
 */
@Mapper
public interface OaKnowledgeArticleMapper extends BaseMapperX<OaKnowledgeArticleDO> {

    default PageResult<OaKnowledgeArticleDO> selectPage(OaKnowledgeArticlePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OaKnowledgeArticleDO>()
                .eqIfPresent(OaKnowledgeArticleDO::getCategoryId, reqVO.getCategoryId())
                .likeIfPresent(OaKnowledgeArticleDO::getTitle, reqVO.getTitle())
                .likeIfPresent(OaKnowledgeArticleDO::getTags, reqVO.getTags())
                .eqIfPresent(OaKnowledgeArticleDO::getStatus, reqVO.getStatus())
                .eqIfPresent(OaKnowledgeArticleDO::getAuthorUserId, reqVO.getAuthorUserId())
                .eqIfPresent(OaKnowledgeArticleDO::getTopFlag, reqVO.getTopFlag())
                .orderByDesc(OaKnowledgeArticleDO::getTopFlag)
                .orderByDesc(OaKnowledgeArticleDO::getId));
    }

    /**
     * 全文检索：在 title/summary/content/tags 上进行 LIKE 关键词检索
     */
    default PageResult<OaKnowledgeArticleDO> searchPage(PageParam pageParam, String keyword) {
        return selectPage(pageParam, new LambdaQueryWrapperX<OaKnowledgeArticleDO>()
                .and(keyword != null, w -> w.like(OaKnowledgeArticleDO::getTitle, keyword)
                        .or().like(OaKnowledgeArticleDO::getSummary, keyword)
                        .or().like(OaKnowledgeArticleDO::getContent, keyword)
                        .or().like(OaKnowledgeArticleDO::getTags, keyword))
                .orderByDesc(OaKnowledgeArticleDO::getTopFlag)
                .orderByDesc(OaKnowledgeArticleDO::getId));
    }

    default Long selectCountByCategoryId(Long categoryId) {
        return selectCount(OaKnowledgeArticleDO::getCategoryId, categoryId);
    }

    /**
     * 原子递增文章版本号，并带乐观锁条件（where current_version = 期望值），
     * 避免并发更新导致版本号丢失自增或 ABA 问题。
     *
     * @param id 文章编号
     * @param expectedVersion 期望的当前版本号（用于乐观锁校验）
     * @return 影响行数（0 表示版本号已被其他事务修改，需重试）
     */
    default int incrementVersionWithOptimisticLock(Long id, Integer expectedVersion) {
        LambdaUpdateWrapper<OaKnowledgeArticleDO> updateWrapper = new LambdaUpdateWrapper<OaKnowledgeArticleDO>()
                .eq(OaKnowledgeArticleDO::getId, id)
                .eq(OaKnowledgeArticleDO::getCurrentVersion, expectedVersion)
                .setSql("current_version = current_version + 1");
        return update(null, updateWrapper);
    }

}
