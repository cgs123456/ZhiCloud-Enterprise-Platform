package cn.iocoder.yudao.module.oa.service.document;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.controller.admin.document.vo.OaDocumentPageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.document.vo.OaDocumentSaveReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.document.OaDocumentDO;
import jakarta.validation.Valid;

/**
 * OA 公文 Service 接口
 *
 * @author yudao
 */
public interface OaDocumentService {

    /**
     * 创建公文
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createDocument(@Valid OaDocumentSaveReqVO createReqVO);

    /**
     * 更新公文
     *
     * @param updateReqVO 更新信息
     */
    void updateDocument(@Valid OaDocumentSaveReqVO updateReqVO);

    /**
     * 删除公文
     *
     * @param id 编号
     */
    void deleteDocument(Long id);

    /**
     * 获得公文
     *
     * @param id 编号
     * @return 公文
     */
    OaDocumentDO getDocument(Long id);

    /**
     * 获得公文分页
     *
     * @param pageReqVO 分页查询
     * @return 公文分页
     */
    PageResult<OaDocumentDO> getDocumentPage(OaDocumentPageReqVO pageReqVO);

    /**
     * 提交审核：草稿 -> 审核中
     *
     * @param id 编号
     */
    void submitDocument(Long id);

    /**
     * 发布公文：审核中 -> 已发布
     *
     * @param id 编号
     */
    void publishDocument(Long id);

    /**
     * 废止公文：已发布 -> 已废止
     *
     * @param id 编号
     */
    void voidDocument(Long id);

    /**
     * 核稿通过：核稿中 -> 待签发
     *
     * @param id       编号
     * @param opinion  核稿意见
     */
    void reviewPassDocument(Long id, String opinion);

    /**
     * 核稿驳回：核稿中 -> 草稿（退回修改）
     *
     * @param id       编号
     * @param opinion  核稿意见
     */
    void reviewRejectDocument(Long id, String opinion);

    /**
     * 签发公文：待签发 -> 已发布
     *
     * @param id       编号
     * @param opinion  签发意见
     */
    void signDocument(Long id, String opinion);

    /**
     * 归档公文：已发布 -> 已归档
     *
     * @param id         编号
     * @param archiveNo  归档编号
     */
    void archiveDocument(Long id, String archiveNo);

    /**
     * 增加阅读量
     *
     * @param id 编号
     */
    void incrementReadCount(Long id);

}
