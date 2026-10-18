package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerDetailExcelVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerDetailRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerGeneralExcelVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerGeneralRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerMultiColumnExcelDataVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerMultiColumnRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerSubjectBalanceExcelVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerSubjectBalanceRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerSubjectRespVO;

import jakarta.validation.Valid;
import java.util.List;

/**
 * ERP 账表 Service 接口
 *
 * <p>账表（ledger）是纯查询域（ADR 266）：数据源 = 账套 → erp_account_book 账簿链 → 已审核凭证（status=20）
 * → erp_gl_voucher_entry 聚合，期初取 erp_gl_initial_balance（账套口径，C6③），科目树取 erp_gl_account。
 * 一期金额式四族：明细账 / 总账 / 科目余额表 / 多栏账；数量与辅助核算四族不建（分录层无该两类事实）。
 *
 * @author 智云
 */
public interface ErpGlLedgerService {

    /**
     * 行类型：期初行
     */
    int ROW_TYPE_OPENING = 1;
    /**
     * 行类型：明细行（明细账/多栏账）或月度合计行（总账）
     */
    int ROW_TYPE_ENTRY = 2;
    /**
     * 行类型：本期合计行
     */
    int ROW_TYPE_PERIOD_TOTAL = 3;
    /**
     * 行类型：本年累计行
     */
    int ROW_TYPE_YEAR_TOTAL = 4;

    /**
     * 获得明细账行（期初行 + 明细行 + 本期合计行 + 本年累计行）
     *
     * @param reqVO 查询条件
     * @return 明细账行列表；科目缺省或不可解析时为空列表
     */
    List<ErpGlLedgerDetailRespVO> getDetailList(@Valid ErpGlLedgerReqVO reqVO);

    /**
     * 获得总账行（按科目分组：期初行 + 各月合计行 + 本年累计行）
     *
     * @param reqVO 查询条件
     * @return 总账行列表
     */
    List<ErpGlLedgerGeneralRespVO> getGeneralList(@Valid ErpGlLedgerReqVO reqVO);

    /**
     * 获得科目余额表（树）
     *
     * @param reqVO 查询条件
     * @return 科目余额行树
     */
    List<ErpGlLedgerSubjectBalanceRespVO> getSubjectBalanceList(@Valid ErpGlLedgerReqVO reqVO);

    /**
     * 获得多栏账（专栏列 + 账簿行）
     *
     * @param reqVO 查询条件
     * @return 多栏账结果
     */
    ErpGlLedgerMultiColumnRespVO getMultiColumn(@Valid ErpGlLedgerReqVO reqVO);

    /**
     * 获得明细账科目树节点（期间内有发生额或期初非零的科目及其祖先，平铺）
     *
     * @param reqVO 查询条件
     * @return 科目节点列表
     */
    List<ErpGlLedgerSubjectRespVO> getDetailSubjectList(@Valid ErpGlLedgerReqVO reqVO);

    /**
     * 获得明细账导出数据
     *
     * @param reqVO 查询条件
     * @return 明细账 Excel 行
     */
    List<ErpGlLedgerDetailExcelVO> getDetailExcelList(@Valid ErpGlLedgerReqVO reqVO);

    /**
     * 获得总账导出数据
     *
     * @param reqVO 查询条件
     * @return 总账 Excel 行
     */
    List<ErpGlLedgerGeneralExcelVO> getGeneralExcelList(@Valid ErpGlLedgerReqVO reqVO);

    /**
     * 获得科目余额表导出数据（树 DFS 序扁平化）
     *
     * @param reqVO 查询条件
     * @return 科目余额 Excel 行
     */
    List<ErpGlLedgerSubjectBalanceExcelVO> getSubjectBalanceExcelList(@Valid ErpGlLedgerReqVO reqVO);

    /**
     * 获得多栏账导出数据（动态表头）
     *
     * @param reqVO 查询条件
     * @return 表头 + 行数据
     */
    ErpGlLedgerMultiColumnExcelDataVO getMultiColumnExcelData(@Valid ErpGlLedgerReqVO reqVO);

}
