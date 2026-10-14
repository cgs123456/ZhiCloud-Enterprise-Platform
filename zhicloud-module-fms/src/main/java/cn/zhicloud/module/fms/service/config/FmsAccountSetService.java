package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsAccountBookRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsAccountSetInitializeReqVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsAccountSetSaveReqVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsAccountSetVoucherRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsBalanceSheetRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsCashFlowStatementRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsIncomeStatementRespVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsAccountSetDO;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

/**
 * FMS 账套 Service 接口
 *
 * @author 智云
 */
public interface FmsAccountSetService {

    Long createAccountSet(Long userId, @Valid FmsAccountSetSaveReqVO createReqVO);

    void updateAccountSet(Long userId, @Valid FmsAccountSetSaveReqVO updateReqVO);

    void initializeAccountSet(@Valid FmsAccountSetInitializeReqVO reqVO);

    FmsAccountSetDO getAccountSet(Long id);

    List<FmsAccountSetDO> getAccountSetList(Long userId);

    /**
     * 获得账套关联的 ERP 账簿列表（V191 打通「账套 → 账簿」层级后的跨模块只读查询）
     *
     * @param accountSetId 账套编号
     * @return 账簿列表，无则空列表
     */
    List<FmsAccountBookRespVO> getAccountBookList(Long accountSetId);

    /**
     * 分页查询账套下的 ERP 凭证流水（V193 只读；不建平行表，经 erp.api 边界包）
     *
     * @param accountSetId 账套编号
     * @param pageNo 页码
     * @param pageSize 每页条数
     * @return 凭证分页结果
     */
    PageResult<FmsAccountSetVoucherRespVO> getVoucherPage(Long accountSetId, Integer pageNo, Integer pageSize);

    /**
     * 账套视角资产负债表（只读；数据来自 ERP 已审核凭证，经 erp.api 边界包）
     *
     * @param accountSetId 账套编号
     * @param asOfDate 报表日期
     * @return 资产负债表
     */
    FmsBalanceSheetRespVO getBalanceSheet(Long accountSetId, LocalDate asOfDate);

    /**
     * 账套视角利润表（只读；数据来自 ERP 已审核凭证，经 erp.api 边界包）
     *
     * @param accountSetId 账套编号
     * @param startDate 起始日期
     * @param endDate 结束日期
     * @return 利润表
     */
    FmsIncomeStatementRespVO getIncomeStatement(Long accountSetId, LocalDate startDate, LocalDate endDate);

    /**
     * 账套视角现金流量表（只读；数据来自 ERP 已审核凭证，经 erp.api 边界包）
     *
     * @param accountSetId 账套编号
     * @param startDate 起始日期
     * @param endDate 结束日期
     * @return 现金流量表
     */
    FmsCashFlowStatementRespVO getCashFlowStatement(Long accountSetId, LocalDate startDate, LocalDate endDate);

}
