package cn.iocoder.yudao.module.oa.service.knowledge;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeCategoryPageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeCategorySaveReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.knowledge.OaKnowledgeCategoryDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * OA 知识库分类 Service 接口
 *
 * @author yudao
 */
public interface OaKnowledgeCategoryService {

    /**
     * 创建知识库分类
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCategory(@Valid OaKnowledgeCategorySaveReqVO createReqVO);

    /**
     * 更新知识库分类
     *
     * @param updateReqVO 更新信息
     */
    void updateCategory(@Valid OaKnowledgeCategorySaveReqVO updateReqVO);

    /**
     * 删除知识库分类
     *
     * @param id 编号
     */
    void deleteCategory(Long id);

    /**
     * 获得知识库分类
     *
     * @param id 编号
     * @return 知识库分类
     */
    OaKnowledgeCategoryDO getCategory(Long id);

    /**
     * 获得知识库分类分页
     *
     * @param pageReqVO 分页查询
     * @return 知识库分类分页
     */
    PageResult<OaKnowledgeCategoryDO> getCategoryPage(OaKnowledgeCategoryPageReqVO pageReqVO);

    /**
     * 获得全部知识库分类列表（用于构建树）
     *
     * @return 分类列表
     */
    List<OaKnowledgeCategoryDO> getCategoryList();

}
