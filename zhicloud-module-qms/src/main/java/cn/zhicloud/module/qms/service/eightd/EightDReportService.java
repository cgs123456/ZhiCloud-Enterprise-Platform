package cn.zhicloud.module.qms.service.eightd;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.qms.controller.admin.eightd.vo.EightDReportPageReqVO;
import cn.zhicloud.module.qms.controller.admin.eightd.vo.EightDReportSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.eightd.EightDReportDO;
import jakarta.validation.Valid;

/**
 * QMS 8D 报告 Service 接口
 *
 * @author zhicloud
 */
public interface EightDReportService {

    /**
     * 创建 8D 报告
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createEightDReport(@Valid EightDReportSaveReqVO createReqVO);

    /**
     * 更新 8D 报告
     *
     * @param updateReqVO 更新信息
     */
    void updateEightDReport(@Valid EightDReportSaveReqVO updateReqVO);

    /**
     * 删除 8D 报告
     *
     * @param id 编号
     */
    void deleteEightDReport(Long id);

    /**
     * 获得 8D 报告
     *
     * @param id 编号
     * @return 8D 报告
     */
    EightDReportDO getEightDReport(Long id);

    /**
     * 获得 8D 报告分页
     *
     * @param pageReqVO 分页查询
     * @return 8D 报告分页
     */
    PageResult<EightDReportDO> getEightDReportPage(EightDReportPageReqVO pageReqVO);

    /**
     * 推进 8D 阶段
     *
     * @param id 编号
     */
    void advanceStage(Long id);

    /**
     * 关闭 8D 报告（D8 团队表彰/关闭）
     *
     * @param id 编号
     */
    void closeEightDReport(Long id);

}