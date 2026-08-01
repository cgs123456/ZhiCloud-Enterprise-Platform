package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.period.ErpPeriodCloseResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.period.ErpPeriodPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.period.ErpPeriodSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpPeriodDO;
import jakarta.validation.Valid;

/**
 * ERP 会计期间 Service 接口（P0-6）
 *
 * <p>提供会计期间 CRUD + 期末处理三大功能：
 * <ol>
 *   <li>{@link #executeMonthCheck(Long)} 月末检查</li>
 *   <li>{@link #executeRevaluation(Long, java.math.BigDecimal)} 调汇</li>
 *   <li>{@link #executeProfitLossTransfer(Long)} 损益结转</li>
 * </ol>
 * 关账流程：必须按顺序执行上述三步，最后调用 {@link #closePeriod(Long)} 完成关账。
 *
 * @author 芋道源码
 */
public interface ErpPeriodService {

    // ==================== 会计期间 CRUD ====================

    /**
     * 创建会计期间
     */
    Long createPeriod(@Valid ErpPeriodSaveReqVO createReqVO);

    /**
     * 更新会计期间
     */
    void updatePeriod(@Valid ErpPeriodSaveReqVO updateReqVO);

    /**
     * 删除会计期间（仅 OPEN 状态可删除）
     */
    void deletePeriod(Long id);

    /**
     * 获取会计期间
     */
    ErpPeriodDO getPeriod(Long id);

    /**
     * 根据期间编码获取
     */
    ErpPeriodDO getPeriodByCode(String code);

    /**
     * 分页查询会计期间
     */
    PageResult<ErpPeriodDO> getPeriodPage(ErpPeriodPageReqVO pageReqVO);

    // ==================== 期末处理链 ====================

    /**
     * 执行月末检查（统计未审核单据）
     *
     * <p>可重复执行，每次都覆盖最近一次的结果。不会修改业务数据。
     *
     * @param periodId 期间编号
     * @return 检查结果
     */
    ErpPeriodCloseResultRespVO executeMonthCheck(Long periodId);

    /**
     * 执行调汇
     *
     * <p>前置条件：已执行月末检查（不要求通过，仅要求执行过）。
     * <p>简化版：传入期末汇率，对启用外币的账户按「账面金额 × (期末汇率 - 记账汇率)」计算调整额。
     * 实际外币账户需要单独建模，本方法预留扩展点。
     *
     * @param periodId       期间编号
     * @param periodExchangeRate 期末汇率（人民币/外币，如 7.25 表示 1 美元 = 7.25 元）
     * @return 调汇结果
     */
    ErpPeriodCloseResultRespVO executeRevaluation(Long periodId, java.math.BigDecimal periodExchangeRate);

    /**
     * 执行损益结转
     *
     * <p>前置条件：已执行月末检查。
     * <p>简化版：基于本期间已审核的销售出库单（收入）和采购入库单（成本）汇总，
     * 计算本期净利润并写入期末处理记录。不生成凭证。
     *
     * @param periodId 期间编号
     * @return 损益结转结果
     */
    ErpPeriodCloseResultRespVO executeProfitLossTransfer(Long periodId);

    /**
     * 关账
     *
     * <p>前置条件：
     * <ul>
     *   <li>已执行月末检查</li>
     *   <li>已执行损益结转（调汇可选）</li>
     * </ul>
     * 关账后期间状态变为 CLOSED，禁止任何业务单据录入。
     *
     * @param periodId 期间编号
     */
    void closePeriod(Long periodId);

}
