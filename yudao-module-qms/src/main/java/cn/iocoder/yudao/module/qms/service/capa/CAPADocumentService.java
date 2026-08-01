package cn.iocoder.yudao.module.qms.service.capa;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.qms.controller.admin.capa.vo.CAPADocumentPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.capa.vo.CAPADocumentSaveReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.capa.vo.CAPAStageTransitionReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.capa.vo.CAPAVerificationReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.capa.CAPADocumentDO;
import jakarta.validation.Valid;

/**
 * QMS CAPA 文档 Service 接口
 *
 * @author 芋道源码
 */
public interface CAPADocumentService {

    /**
     * 创建 CAPA 文档
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCAPADocument(@Valid CAPADocumentSaveReqVO createReqVO);

    /**
     * 更新 CAPA 文档
     *
     * @param updateReqVO 更新信息
     */
    void updateCAPADocument(@Valid CAPADocumentSaveReqVO updateReqVO);

    /**
     * 删除 CAPA 文档
     *
     * @param id 编号
     */
    void deleteCAPADocument(Long id);

    /**
     * 获得_CAPA 文档
     *
     * @param id 编号
     * @return CAPA 文档
     */
    CAPADocumentDO getCAPADocument(Long id);

    /**
     * 获得 CAPA 文档分页
     *
     * @param pageReqVO 分页查询
     * @return CAPA 文档分页
     */
    PageResult<CAPADocumentDO> getCAPADocumentPage(CAPADocumentPageReqVO pageReqVO);

    /**
     * 关闭 CAPA 文档（P0-4 兼容旧接口，内部走状态机最后一步）
     *
     * <p>仅在 stage=VERIFICATION 且 verificationResult=PASSED 时允许关闭，
     * 关闭后将 stage 置为 CLOSED、status 置为 CLOSED、closeDate 设为当前时间。
     *
     * @param id 编号
     */
    void closeCAPADocument(Long id);

    // ==================== P0-4 CAPA 全流程状态机 ====================

    /**
     * CAPA 阶段流转（前进 / 后退 1 步）
     *
     * <p>目标阶段必须比当前阶段前进或后退 1 步，否则抛出
     * {@link cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants#CAPA_DOCUMENT_STAGE_TRANSITION_INVALID}。
     * 前进时校验当前阶段必填字段已填写：
     * <ul>
     *   <li>CREATED → ROOT_CAUSE_ANALYSIS：无强制校验</li>
     *   <li>ROOT_CAUSE_ANALYSIS → CORRECTIVE_ACTION：rootCauseAnalysis 必填</li>
     *   <li>CORRECTIVE_ACTION → PREVENTIVE_ACTION：correctiveAction 必填</li>
     *   <li>PREVENTIVE_ACTION → VERIFICATION：preventiveAction 必填</li>
     *   <li>VERIFICATION → CLOSED：verificationResult 必须为 PASSED（建议通过 closeCAPADocument 走关闭流程）</li>
     * </ul>
     *
     * @param reqVO 流转请求
     */
    void transitionStage(@Valid CAPAStageTransitionReqVO reqVO);

    /**
     * 提交有效性验证结果
     *
     * <p>仅在 stage=VERIFICATION 时允许。提交 PASSED 后可调用 closeCAPADocument 关闭；
     * 提交 FAILED 后将自动回退到 CORRECTIVE_ACTION 阶段，重新走纠正→预防→验证流程。
     *
     * @param reqVO 验证请求
     */
    void submitVerification(@Valid CAPAVerificationReqVO reqVO);

}
