package cn.iocoder.yudao.module.qms.service.audit;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.qms.controller.admin.audit.vo.QmsAuditReportPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.audit.vo.QmsAuditReportSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.audit.QmsAuditReportDO;
import jakarta.validation.Valid;

/**
 * QMS 审核报告 Service 接口
 *
 * @author 芋道源码
 */
public interface QmsAuditReportService {

    /**
     * 创建审核报告（含不符合项汇总）
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createAuditReport(@Valid QmsAuditReportSaveReqVO createReqVO);

    /**
     * 更新审核报告
     *
     * @param updateReqVO 更新信息
     */
    void updateAuditReport(@Valid QmsAuditReportSaveReqVO updateReqVO);

    /**
     * 删除审核报告
     *
     * @param id 编号
     */
    void deleteAuditReport(Long id);

    /**
     * 获得审核报告
     *
     * @param id 编号
     * @return 审核报告
     */
    QmsAuditReportDO getAuditReport(Long id);

    /**
     * 获得审核报告分页
     *
     * @param pageReqVO 分页查询
     * @return 审核报告分页
     */
    PageResult<QmsAuditReportDO> getAuditReportPage(QmsAuditReportPageReqVO pageReqVO);

}
