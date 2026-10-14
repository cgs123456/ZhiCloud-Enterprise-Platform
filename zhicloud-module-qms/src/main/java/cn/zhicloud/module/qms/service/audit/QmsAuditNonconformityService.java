package cn.zhicloud.module.qms.service.audit;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.qms.controller.admin.audit.vo.QmsAuditNonconformityPageReqVO;
import cn.zhicloud.module.qms.controller.admin.audit.vo.QmsAuditNonconformityCompleteReqVO;
import cn.zhicloud.module.qms.controller.admin.audit.vo.QmsAuditNonconformityRectifyReqVO;
import cn.zhicloud.module.qms.controller.admin.audit.vo.QmsAuditNonconformitySaveReqVO;
import cn.zhicloud.module.qms.controller.admin.audit.vo.QmsAuditNonconformityVerifyReqVO;
import cn.zhicloud.module.qms.dal.dataobject.audit.QmsAuditNonconformityDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * QMS 审核不符合项 Service 接口
 *
 * @author 智云
 */
public interface QmsAuditNonconformityService {

    /**
     * 新增不符合项
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createNonconformity(@Valid QmsAuditNonconformitySaveReqVO createReqVO);

    /**
     * 更新不符合项
     *
     * @param updateReqVO 更新信息
     */
    void updateNonconformity(@Valid QmsAuditNonconformitySaveReqVO updateReqVO);

    /**
     * 删除不符合项
     *
     * @param id 编号
     */
    void deleteNonconformity(Long id);

    /**
     * 获得不符合项
     *
     * @param id 编号
     * @return 不符合项
     */
    QmsAuditNonconformityDO getNonconformity(Long id);

    /**
     * 获得不符合项分页
     *
     * @param pageReqVO 分页查询
     * @return 不符合项分页
     */
    PageResult<QmsAuditNonconformityDO> getNonconformityPage(QmsAuditNonconformityPageReqVO pageReqVO);

    /**
     * 提交整改措施，状态 10 待整改 -> 20 整改中
     *
     * @param reqVO 整改信息
     */
    void rectifyNonconformity(@Valid QmsAuditNonconformityRectifyReqVO reqVO);

    /**
     * 完成整改，状态 20 整改中 -> 30 已整改
     *
     * <p>台账 §12.218：补齐五态状态机的断环。此前 rectify 后停在 20，而 verify 要求
     * 30，中间无端点可推进——不符合项一旦提交整改便永远无法验证与关闭（真机实证：
     * 在 20 下调 verify/close 均返回 1044000045 状态非法）。
     *
     * @param reqVO 完成整改信息（completionNote 可空，留空保留原整改措施）
     */
    void completeRectification(@Valid QmsAuditNonconformityCompleteReqVO reqVO);

    /**
     * 验证整改效果，状态 30 已整改 -> 40 已验证
     *
     * @param reqVO 验证信息
     */
    void verifyNonconformity(@Valid QmsAuditNonconformityVerifyReqVO reqVO);

    /**
     * 关闭不符合项，状态 40 已验证 -> 50 已关闭
     *
     * @param id 编号
     */
    void closeNonconformity(Long id);

    /**
     * 获得审核报告关联的不符合项列表
     *
     * @param reportId 报告 ID
     * @return 不符合项列表
     */
    List<QmsAuditNonconformityDO> getNonconformityListByReportId(Long reportId);

}
