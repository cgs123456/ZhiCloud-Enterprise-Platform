package cn.iocoder.yudao.module.oa.service.knowledge;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeCategoryPageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeCategorySaveReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.knowledge.OaKnowledgeCategoryDO;
import cn.iocoder.yudao.module.oa.dal.mysql.knowledge.OaKnowledgeArticleMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.knowledge.OaKnowledgeCategoryMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.oa.enums.ErrorCodeConstants.OA_KNOWLEDGE_CATEGORY_HAS_ARTICLES;
import static cn.iocoder.yudao.module.oa.enums.ErrorCodeConstants.OA_KNOWLEDGE_CATEGORY_HAS_CHILDREN;
import static cn.iocoder.yudao.module.oa.enums.ErrorCodeConstants.OA_KNOWLEDGE_CATEGORY_NAME_DUPLICATE;
import static cn.iocoder.yudao.module.oa.enums.ErrorCodeConstants.OA_KNOWLEDGE_CATEGORY_NOT_EXISTS;

/**
 * OA 知识库分类 Service 实现类
 *
 * @author yudao
 */
@Service
@Validated
public class OaKnowledgeCategoryServiceImpl implements OaKnowledgeCategoryService {

    /**
     * 根分类父 ID
     */
    private static final long ROOT_PARENT_ID = 0L;
    /**
     * 启用状态
     */
    private static final int STATUS_ENABLED = 0;

    @Resource
    private OaKnowledgeCategoryMapper categoryMapper;
    @Resource
    private OaKnowledgeArticleMapper articleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(OaKnowledgeCategorySaveReqVO createReqVO) {
        // 校验同级分类名称唯一
        validateNameUnique(null, createReqVO.getParentId(), createReqVO.getName());
        // 插入分类
        OaKnowledgeCategoryDO category = BeanUtils.toBean(createReqVO, OaKnowledgeCategoryDO.class);
        if (category.getParentId() == null) {
            category.setParentId(ROOT_PARENT_ID);
        }
        if (category.getStatus() == null) {
            category.setStatus(STATUS_ENABLED);
        }
        categoryMapper.insert(category);
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(OaKnowledgeCategorySaveReqVO updateReqVO) {
        // 校验存在
        validateCategoryExists(updateReqVO.getId());
        // 校验同级分类名称唯一
        validateNameUnique(updateReqVO.getId(), updateReqVO.getParentId(), updateReqVO.getName());
        // 更新分类
        OaKnowledgeCategoryDO updateObj = BeanUtils.toBean(updateReqVO, OaKnowledgeCategoryDO.class);
        categoryMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        // 校验存在
        validateCategoryExists(id);
        // 校验是否存在子分类
        if (categoryMapper.selectCountByParentId(id) > 0) {
            throw exception(OA_KNOWLEDGE_CATEGORY_HAS_CHILDREN);
        }
        // 校验分类下是否存在文章
        if (articleMapper.selectCountByCategoryId(id) > 0) {
            throw exception(OA_KNOWLEDGE_CATEGORY_HAS_ARTICLES);
        }
        // 删除分类
        categoryMapper.deleteById(id);
    }

    @Override
    public OaKnowledgeCategoryDO getCategory(Long id) {
        return categoryMapper.selectById(id);
    }

    @Override
    public PageResult<OaKnowledgeCategoryDO> getCategoryPage(OaKnowledgeCategoryPageReqVO pageReqVO) {
        return categoryMapper.selectPage(pageReqVO);
    }

    @Override
    public List<OaKnowledgeCategoryDO> getCategoryList() {
        return categoryMapper.selectList();
    }

    private void validateNameUnique(Long id, Long parentId, String name) {
        if (name == null) {
            return;
        }
        Long actualParentId = parentId != null ? parentId : ROOT_PARENT_ID;
        OaKnowledgeCategoryDO category = categoryMapper.selectByParentIdAndName(actualParentId, name);
        if (category == null) {
            return;
        }
        if (id == null || !category.getId().equals(id)) {
            throw exception(OA_KNOWLEDGE_CATEGORY_NAME_DUPLICATE);
        }
    }

    private OaKnowledgeCategoryDO validateCategoryExists(Long id) {
        OaKnowledgeCategoryDO category = categoryMapper.selectById(id);
        if (category == null) {
            throw exception(OA_KNOWLEDGE_CATEGORY_NOT_EXISTS);
        }
        return category;
    }

}
