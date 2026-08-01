package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationWorksheetPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationWorksheetSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpConsolidationWorksheetDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP 合并工作底稿 Service 接口（P1-合并报表引擎）
 *
 * @author 芋道源码
 */
public interface ErpConsolidationWorksheetService {

    /**
     * 创建工作底稿
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createWorksheet(@Valid ErpConsolidationWorksheetSaveReqVO createReqVO);

    /**
     * 更新工作底稿
     *
     * @param updateReqVO 更新信息
     */
    void updateWorksheet(@Valid ErpConsolidationWorksheetSaveReqVO updateReqVO);

    /**
     * 删除工作底稿
     *
     * @param id 编号
     */
    void deleteWorksheet(Long id);

    /**
     * 获取工作底稿
     *
     * @param id 编号
     * @return 工作底稿
     */
    ErpConsolidationWorksheetDO getWorksheet(Long id);

    /**
     * 分页查询工作底稿
     *
     * @param pageReqVO 分页查询
     * @return 分页结果
     */
    PageResult<ErpConsolidationWorksheetDO> getWorksheetPage(ErpConsolidationWorksheetPageReqVO pageReqVO);

    /**
     * 按合并周期查询工作底稿列表
     *
     * @param consolidationPeriod 合并周期
     * @return 工作底稿列表
     */
    List<ErpConsolidationWorksheetDO> getWorksheetListByPeriod(String consolidationPeriod);

    /**
     * 审核工作底稿（待审核 → 已审核）
     *
     * @param id 编号
     */
    void approveWorksheet(Long id);

    /**
     * 驳回工作底稿（待审核 → 已驳回）
     *
     * @param id 编号
     */
    void rejectWorksheet(Long id);

}
