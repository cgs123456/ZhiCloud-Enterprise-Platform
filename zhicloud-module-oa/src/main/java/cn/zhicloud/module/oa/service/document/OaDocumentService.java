package cn.zhicloud.module.oa.service.document;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.oa.controller.admin.document.vo.OaDocumentPageReqVO;
import cn.zhicloud.module.oa.controller.admin.document.vo.OaDocumentSaveReqVO;
import cn.zhicloud.module.oa.dal.dataobject.document.OaDocumentDO;
import jakarta.validation.Valid;

/**
 * OA 公文 Service 接口
 *
 * @author zhicloud
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
     * 回写 BPM 审批结果（由 {@link cn.zhicloud.module.oa.service.document.listener.OaDocumentStatusListener} 调用）：
     * 通过 -> 待签发(25)，驳回/撤销 -> 草稿(10)。只有审批中(20)的公文才落一次，
     * BPM 结果事件是至少一次投递，重复投递不能把已核稿/已签发的公文倒退。
     *
     * @param id                编号
     * @param status            公文状态（25 或 10）
     * @param processInstanceId 流程实例编号
     */
    void updateDocumentStatus(Long id, Integer status, String processInstanceId);

    /**
     * 增加阅读量
     *
     * @param id 编号
     */
    void incrementReadCount(Long id);

}
