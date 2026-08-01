package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.fixedasset.ErpFixedAssetPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.fixedasset.ErpFixedAssetSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherEntryReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFixedAssetDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFixedAssetDepreciationDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpPeriodDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFixedAssetDepreciationMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFixedAssetMapper;
import cn.iocoder.yudao.module.erp.enums.finance.ErpDepreciationMethodEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFixedAssetStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * ERP 固定资产 Service 实现类（P0-14）
 *
 * <p>提供固定资产主数据 CRUD + 月度折旧计提（直线法 / DDB / SYD）。
 *
 * <p>折旧公式（直线法）：
 * <pre>
 *   月折旧额 = (原值 - 残值) / 使用年限月数
 *   累计折旧 = 月折旧额 × 已折旧月数
 *   账面净值 = 原值 - 累计折旧
 *   提足判断：已折旧月数 >= 使用年限月数，或 账面净值 <= 残值
 * </pre>
 *
 * <p>折旧公式（DDB 双倍余额递减法）：
 * <pre>
 *   月折旧率 = 2 / 使用年限月数
 *   月折旧额 = (原值 - 累计折旧) × 月折旧率
 *   最后两年切换为直线法：月折旧额 = (账面净值 - 残值) / 剩余月数
 * </pre>
 *
 * <p>折旧公式（SYD 年数总和法）：
 * <pre>
 *   年数总和 = n × (n + 1) / 2，n = 使用年限年数
 *   月折旧额 = (原值 - 残值) × 剩余年数 / 年数总和 / 12
 * </pre>
 *
 * <p>尾差处理：最后一个月折旧额 = 原值 - 残值 - 当前累计折旧，确保账面净值恰好等于残值。
 *
 * <p>折旧审核过账：approveDepreciation 审核时，根据资产配置的折旧费用科目和累计折旧科目，
 * 自动构造 GL 转账凭证（借：折旧费用/管理费用/制造费用，贷：累计折旧）并审核过账，更新科目余额。
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class ErpFixedAssetServiceImpl implements ErpFixedAssetService {

    /** 折旧金额精度：2 位小数 */
    private static final int SCALE = 2;

    @Resource
    private ErpFixedAssetMapper fixedAssetMapper;
    @Resource
    private ErpFixedAssetDepreciationMapper depreciationMapper;
    @Resource
    private ErpPeriodService periodService;
    @Resource
    private ErpGlVoucherService glVoucherService;

    // ==================== 固定资产主数据 CRUD ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFixedAsset(ErpFixedAssetSaveReqVO createReqVO) {
        // 1. 校验编码唯一
        if (fixedAssetMapper.selectByCode(createReqVO.getCode()) != null) {
            throw exception(FIXED_ASSET_CODE_DUPLICATE, createReqVO.getCode());
        }
        // 2. 校验使用年限 > 0
        validateUsefulLife(createReqVO.getUsefulLifeMonths());
        // 3. 校验残值 < 原值
        validateSalvageValue(createReqVO.getOriginalValue(), createReqVO.getSalvageValue());
        // 4. 构建对象并初始化折旧字段
        ErpFixedAssetDO asset = BeanUtils.toBean(createReqVO, ErpFixedAssetDO.class);
        // 初始化折旧相关字段
        asset.setDepreciatedMonths(0);
        asset.setAccumulatedDepreciation(BigDecimal.ZERO);
        asset.setNetBookValue(asset.getOriginalValue());
        // 初始化默认值
        if (asset.getDepreciationMethod() == null) {
            asset.setDepreciationMethod(ErpDepreciationMethodEnum.STRAIGHT_LINE.getMethod());
        }
        if (asset.getStatus() == null) {
            asset.setStatus(ErpFixedAssetStatusEnum.IN_USE.getStatus());
        }
        if (asset.getSalvageValue() == null) {
            asset.setSalvageValue(BigDecimal.ZERO);
        }
        fixedAssetMapper.insert(asset);
        return asset.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFixedAsset(ErpFixedAssetSaveReqVO updateReqVO) {
        // 1. 校验存在
        validateFixedAssetExists(updateReqVO.getId());
        // 2. 校验编码唯一（排除自身）
        ErpFixedAssetDO byCode = fixedAssetMapper.selectByCode(updateReqVO.getCode());
        if (byCode != null && !Objects.equals(byCode.getId(), updateReqVO.getId())) {
            throw exception(FIXED_ASSET_CODE_DUPLICATE, updateReqVO.getCode());
        }
        // 3. 校验使用年限 > 0
        validateUsefulLife(updateReqVO.getUsefulLifeMonths());
        // 4. 校验残值 < 原值
        validateSalvageValue(updateReqVO.getOriginalValue(), updateReqVO.getSalvageValue());
        // 5. 更新（保留折旧字段不被覆盖：使用 toBean 后手动重置）
        ErpFixedAssetDO existing = fixedAssetMapper.selectById(updateReqVO.getId());
        ErpFixedAssetDO updateObj = BeanUtils.toBean(updateReqVO, ErpFixedAssetDO.class);
        // 保留已有的折旧计算结果，不被 VO 覆盖
        updateObj.setDepreciatedMonths(existing.getDepreciatedMonths());
        updateObj.setAccumulatedDepreciation(existing.getAccumulatedDepreciation());
        updateObj.setNetBookValue(existing.getNetBookValue());
        fixedAssetMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFixedAsset(Long id) {
        ErpFixedAssetDO existing = validateFixedAssetExists(id);
        // 校验无折旧记录（有折旧记录的资产不允许删除）
        List<ErpFixedAssetDepreciationDO> depreciations = depreciationMapper.selectListByFixedAssetId(id);
        if (depreciations != null && !depreciations.isEmpty()) {
            throw exception(FIXED_ASSET_HAS_DEPRECIATION_RECORDS, existing.getCode());
        }
        fixedAssetMapper.deleteById(id);
    }

    @Override
    public ErpFixedAssetDO getFixedAsset(Long id) {
        return fixedAssetMapper.selectById(id);
    }

    @Override
    public PageResult<ErpFixedAssetDO> getFixedAssetPage(ErpFixedAssetPageReqVO pageReqVO) {
        return fixedAssetMapper.selectPage(pageReqVO);
    }

    // ==================== 折旧计提 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long calculateMonthlyDepreciation(Long fixedAssetId, Long periodId) {
        // 1. 校验资产存在
        ErpFixedAssetDO asset = validateFixedAssetExists(fixedAssetId);
        // 2. 校验资产状态为在用
        if (!Objects.equals(asset.getStatus(), ErpFixedAssetStatusEnum.IN_USE.getStatus())) {
            throw exception(FIXED_ASSET_NOT_IN_USE, asset.getCode());
        }
        // 3. 获取折旧方法（支持直线法/DDB/SYD，由 calculateMonthlyAmount 内部分发）
        Integer method = asset.getDepreciationMethod();
        // 4. 校验未提足折旧
        validateNotFullyDepreciated(asset);
        // 5. 校验期间无重复折旧记录
        ErpFixedAssetDepreciationDO existingDep = depreciationMapper
                .selectByFixedAssetIdAndPeriodId(fixedAssetId, periodId);
        if (existingDep != null) {
            throw exception(FIXED_ASSET_DEPRECIATION_PERIOD_DUPLICATE, asset.getCode(), periodId);
        }
        // 6. 获取会计期间信息
        ErpPeriodDO period = periodService.getPeriod(periodId);
        if (period == null) {
            throw exception(PERIOD_NOT_EXISTS);
        }
        // 7. 计算月折旧额（含尾差处理）
        BigDecimal monthlyDepreciation = calculateMonthlyAmount(asset);
        // 8. 计算新累计折旧、已折旧月数、账面净值
        BigDecimal newAccumulatedDep = asset.getAccumulatedDepreciation().add(monthlyDepreciation);
        int newDepreciatedMonths = asset.getDepreciatedMonths() + 1;
        BigDecimal newNetBookValue = asset.getOriginalValue().subtract(newAccumulatedDep);
        // 安全兜底：账面净值不能低于残值
        BigDecimal salvage = asset.getSalvageValue() == null ? BigDecimal.ZERO : asset.getSalvageValue();
        if (newNetBookValue.compareTo(salvage) < 0) {
            newNetBookValue = salvage;
        }
        // 9. 创建折旧记录
        ErpFixedAssetDepreciationDO depreciation = ErpFixedAssetDepreciationDO.builder()
                .fixedAssetId(fixedAssetId)
                .assetCode(asset.getCode())
                .assetName(asset.getName())
                .periodId(periodId)
                .periodCode(period.getCode())
                .depreciationDate(LocalDate.now())
                .depreciationAmount(monthlyDepreciation)
                .accumulatedDepreciation(newAccumulatedDep)
                .netBookValue(newNetBookValue)
                .depreciatedMonths(newDepreciatedMonths)
                .depreciationMethod(method)
                .status(10) // 10=待审核 DRAFT
                .build();
        depreciationMapper.insert(depreciation);
        // 10. 更新资产折旧字段
        ErpFixedAssetDO updateAsset = new ErpFixedAssetDO();
        updateAsset.setId(fixedAssetId);
        updateAsset.setDepreciatedMonths(newDepreciatedMonths);
        updateAsset.setAccumulatedDepreciation(newAccumulatedDep);
        updateAsset.setNetBookValue(newNetBookValue);
        fixedAssetMapper.updateById(updateAsset);
        log.info("[calculateMonthlyDepreciation][资产 {} 计提折旧成功，月折旧额={}, 累计折旧={}, 账面净值={}]",
                asset.getCode(), monthlyDepreciation, newAccumulatedDep, newNetBookValue);
        return depreciation.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveDepreciation(Long depreciationId) {
        ErpFixedAssetDepreciationDO depreciation = depreciationMapper.selectById(depreciationId);
        if (depreciation == null) {
            throw exception(FIXED_ASSET_DEPRECIATION_NOT_EXISTS);
        }
        if (Objects.equals(depreciation.getStatus(), 20)) { // 20=已审核 APPROVED
            throw exception(FIXED_ASSET_DEPRECIATION_ALREADY_APPROVED);
        }
        // 生成凭证编号
        String voucherNo = "FA-DEP-" + YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-" + depreciationId;
        // 查询资产获取折旧科目配置，构造 GL 凭证并过账
        ErpFixedAssetDO asset = fixedAssetMapper.selectById(depreciation.getFixedAssetId());
        Long voucherId = null;
        if (asset != null
                && asset.getDepreciationExpenseAccountId() != null
                && asset.getAccumulatedDepreciationAccountId() != null) {
            try {
                // 构造 GL 转账凭证：借-折旧费用科目（管理费用/制造费用），贷-累计折旧科目
                ErpGlVoucherSaveReqVO voucherReqVO = new ErpGlVoucherSaveReqVO();
                voucherReqVO.setVoucherNo(voucherNo);
                voucherReqVO.setVoucherDate(depreciation.getDepreciationDate() != null
                        ? depreciation.getDepreciationDate() : LocalDate.now());
                voucherReqVO.setPeriodId(depreciation.getPeriodId());
                voucherReqVO.setVoucherType(30); // 30=转账凭证
                int depMonth = (depreciation.getDepreciationDate() != null
                        ? depreciation.getDepreciationDate() : LocalDate.now()).getMonthValue();
                voucherReqVO.setSummary("计提" + depMonth + "月固定资产折旧");
                List<ErpGlVoucherEntryReqVO> entries = new ArrayList<>();
                // 借：折旧费用科目（管理费用/制造费用）
                ErpGlVoucherEntryReqVO debitEntry = new ErpGlVoucherEntryReqVO();
                debitEntry.setAccountId(asset.getDepreciationExpenseAccountId());
                debitEntry.setSummary("计提折旧-" + depreciation.getAssetCode());
                debitEntry.setDebitAmount(depreciation.getDepreciationAmount());
                debitEntry.setCreditAmount(BigDecimal.ZERO);
                debitEntry.setSort(1);
                entries.add(debitEntry);
                // 贷：累计折旧科目
                ErpGlVoucherEntryReqVO creditEntry = new ErpGlVoucherEntryReqVO();
                creditEntry.setAccountId(asset.getAccumulatedDepreciationAccountId());
                creditEntry.setSummary("累计折旧-" + depreciation.getAssetCode());
                creditEntry.setDebitAmount(BigDecimal.ZERO);
                creditEntry.setCreditAmount(depreciation.getDepreciationAmount());
                creditEntry.setSort(2);
                entries.add(creditEntry);
                voucherReqVO.setEntries(entries);
                // 创建 GL 凭证并审核过账（更新科目累计发生额与期末余额）
                voucherId = glVoucherService.createGlVoucher(voucherReqVO);
                glVoucherService.approveGlVoucher(voucherId);
            } catch (Exception e) {
                // P1 修复：GL 凭证创建失败必须回滚事务，避免"折旧已审核但凭证缺失"的账实不符
                log.error("[approveDepreciation][折旧记录 {} 创建 GL 凭证失败，voucherNo={}，事务将回滚]",
                        depreciationId, voucherNo, e);
                throw exception(FIXED_ASSET_DEPRECIATION_GL_VOUCHER_FAIL, depreciationId);
            }
        } else {
            log.warn("[approveDepreciation][资产 {} 未配置折旧费用/累计折旧科目，跳过 GL 凭证过账]",
                    depreciation.getAssetCode());
        }
        // 更新折旧记录状态
        ErpFixedAssetDepreciationDO updateObj = new ErpFixedAssetDepreciationDO();
        updateObj.setId(depreciationId);
        updateObj.setStatus(20); // 20=已审核 APPROVED
        updateObj.setVoucherNo(voucherNo);
        if (voucherId != null) {
            updateObj.setVoucherId(voucherId);
        }
        depreciationMapper.updateById(updateObj);
        log.info("[approveDepreciation][折旧记录 {} 审核成功，凭证编号={}，GL 凭证 ID={}]",
                depreciationId, voucherNo, voucherId);
    }

    @Override
    public List<ErpFixedAssetDepreciationDO> getDepreciationListByFixedAssetId(Long fixedAssetId) {
        return depreciationMapper.selectListByFixedAssetId(fixedAssetId);
    }

    @Override
    public List<ErpFixedAssetDepreciationDO> getDepreciationListByPeriodId(Long periodId) {
        return depreciationMapper.selectListByPeriodId(periodId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchDepreciationResult batchCalculateMonthlyDepreciation(List<Long> assetIds, LocalDate period) {
        int successCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (assetIds == null || assetIds.isEmpty()) {
            return new BatchDepreciationResult(0, BigDecimal.ZERO);
        }
        // 1. 根据期间日期定位会计期间（期间编码格式 yyyyMM）
        String periodCode = period.format(DateTimeFormatter.ofPattern("yyyyMM"));
        ErpPeriodDO periodDO = periodService.getPeriodByCode(periodCode);
        if (periodDO == null) {
            throw exception(PERIOD_NOT_EXISTS);
        }
        // 2. 逐个资产计提折旧，单个失败不影响其他资产
        for (Long assetId : assetIds) {
            try {
                Long depId = calculateMonthlyDepreciation(assetId, periodDO.getId());
                if (depId != null) {
                    ErpFixedAssetDepreciationDO dep = depreciationMapper.selectById(depId);
                    if (dep != null) {
                        successCount++;
                        totalAmount = totalAmount.add(dep.getDepreciationAmount());
                    }
                }
            } catch (Exception e) {
                log.warn("[batchCalculateMonthlyDepreciation][资产 {} 计提折旧失败：{}]", assetId, e.getMessage());
            }
        }
        log.info("[batchCalculateMonthlyDepreciation][期间 {} 批量计提完成，成功 {} 个，总金额 {}]",
                periodCode, successCount, totalAmount);
        return new BatchDepreciationResult(successCount, totalAmount);
    }

    @Override
    public ErpFixedAssetDO validateFixedAssetExists(Long id) {
        if (id == null) {
            throw exception(FIXED_ASSET_NOT_EXISTS);
        }
        ErpFixedAssetDO asset = fixedAssetMapper.selectById(id);
        if (asset == null) {
            throw exception(FIXED_ASSET_NOT_EXISTS);
        }
        return asset;
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 校验使用年限 > 0
     */
    private void validateUsefulLife(Integer usefulLifeMonths) {
        if (usefulLifeMonths == null || usefulLifeMonths <= 0) {
            throw exception(FIXED_ASSET_USEFUL_LIFE_INVALID);
        }
    }

    /**
     * 校验残值 < 原值
     */
    private void validateSalvageValue(BigDecimal originalValue, BigDecimal salvageValue) {
        if (originalValue == null) {
            throw exception(FIXED_ASSET_USEFUL_LIFE_INVALID);
        }
        BigDecimal salvage = salvageValue == null ? BigDecimal.ZERO : salvageValue;
        if (salvage.compareTo(originalValue) >= 0) {
            throw exception(FIXED_ASSET_SALVAGE_INVALID);
        }
    }

    /**
     * 校验资产未提足折旧
     *
     * <p>提足条件：已折旧月数 >= 使用年限月数，或 账面净值 <= 残值
     */
    private void validateNotFullyDepreciated(ErpFixedAssetDO asset) {
        Integer depreciatedMonths = asset.getDepreciatedMonths() == null ? 0 : asset.getDepreciatedMonths();
        Integer usefulLifeMonths = asset.getUsefulLifeMonths();
        if (depreciatedMonths >= usefulLifeMonths) {
            throw exception(FIXED_ASSET_ALREADY_FULLY_DEPRECIATED, asset.getCode());
        }
        BigDecimal salvage = asset.getSalvageValue() == null ? BigDecimal.ZERO : asset.getSalvageValue();
        BigDecimal netBookValue = asset.getNetBookValue() == null
                ? asset.getOriginalValue() : asset.getNetBookValue();
        if (netBookValue.compareTo(salvage) <= 0) {
            throw exception(FIXED_ASSET_ALREADY_FULLY_DEPRECIATED, asset.getCode());
        }
    }

    /**
     * 计算月折旧额（根据折旧方法分派）
     *
     * <p>支持：直线法（10）/ 双倍余额递减法 DDB（20）/ 年数总和法 SYD（30）
     *
     * @param asset 固定资产
     * @return 月折旧额
     */
    private BigDecimal calculateMonthlyAmount(ErpFixedAssetDO asset) {
        Integer method = asset.getDepreciationMethod();
        if (Objects.equals(method, ErpDepreciationMethodEnum.STRAIGHT_LINE.getMethod())) {
            return calculateStraightLineMonthlyAmount(asset);
        } else if (Objects.equals(method, ErpDepreciationMethodEnum.DOUBLE_DECLINING_BALANCE.getMethod())) {
            return calculateDdbMonthlyAmount(asset);
        } else if (Objects.equals(method, ErpDepreciationMethodEnum.SUM_OF_YEARS_DIGITS.getMethod())) {
            return calculateSydMonthlyAmount(asset);
        }
        throw exception(FIXED_ASSET_DEPRECIATION_METHOD_NOT_SUPPORT, method);
    }

    /**
     * 直线法（年限平均法）月折旧额，含尾差处理
     *
     * <p>公式：月折旧额 = (原值 - 残值) / 使用年限月数
     *
     * <p>尾差处理：最后一个月（已折旧月数 + 1 == 使用年限月数）时，
     * 月折旧额 = 原值 - 残值 - 当前累计折旧，确保账面净值恰好等于残值。
     */
    private BigDecimal calculateStraightLineMonthlyAmount(ErpFixedAssetDO asset) {
        BigDecimal originalValue = asset.getOriginalValue();
        BigDecimal salvage = asset.getSalvageValue() == null ? BigDecimal.ZERO : asset.getSalvageValue();
        int usefulLifeMonths = asset.getUsefulLifeMonths();
        int depreciatedMonths = asset.getDepreciatedMonths() == null ? 0 : asset.getDepreciatedMonths();
        // 可折旧总额 = 原值 - 残值
        BigDecimal depreciableAmount = originalValue.subtract(salvage);
        // 标准月折旧额 = (原值 - 残值) / 使用年限月数
        BigDecimal standardMonthly = depreciableAmount
                .divide(BigDecimal.valueOf(usefulLifeMonths), SCALE, RoundingMode.HALF_UP);
        // 尾差处理：最后一个月取剩余可折旧额
        if (depreciatedMonths + 1 >= usefulLifeMonths) {
            BigDecimal remaining = depreciableAmount.subtract(asset.getAccumulatedDepreciation());
            // 剩余额不能小于 0
            return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
        }
        return standardMonthly;
    }

    /**
     * 双倍余额递减法（DDB）月折旧额，含尾差处理
     *
     * <p>公式：
     * <pre>
     *   月折旧率 = 2 / 使用年限月数
     *   月折旧额 = (原值 - 累计折旧) × 月折旧率
     * </pre>
     *
     * <p>最后两年切换为直线法：月折旧额 = (账面净值 - 残值) / 剩余月数
     *
     * <p>尾差处理：最后一个月取剩余可折旧额，确保账面净值恰好等于残值。
     */
    private BigDecimal calculateDdbMonthlyAmount(ErpFixedAssetDO asset) {
        BigDecimal originalValue = asset.getOriginalValue();
        BigDecimal salvage = asset.getSalvageValue() == null ? BigDecimal.ZERO : asset.getSalvageValue();
        int usefulLifeMonths = asset.getUsefulLifeMonths();
        int depreciatedMonths = asset.getDepreciatedMonths() == null ? 0 : asset.getDepreciatedMonths();
        BigDecimal accumulatedDepreciation = asset.getAccumulatedDepreciation() == null
                ? BigDecimal.ZERO : asset.getAccumulatedDepreciation();
        // 账面净值 = 原值 - 累计折旧
        BigDecimal netBookValue = originalValue.subtract(accumulatedDepreciation);
        // 可折旧总额 = 原值 - 残值
        BigDecimal depreciableAmount = originalValue.subtract(salvage);
        int remainingMonths = usefulLifeMonths - depreciatedMonths;

        // 尾差处理：最后一个月取剩余可折旧额
        if (depreciatedMonths + 1 >= usefulLifeMonths) {
            BigDecimal remaining = depreciableAmount.subtract(accumulatedDepreciation);
            return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
        }

        // 最后两年（24 个月）切换为直线法
        if (remainingMonths <= 24) {
            BigDecimal slMonthly = netBookValue.subtract(salvage)
                    .divide(BigDecimal.valueOf(remainingMonths), SCALE, RoundingMode.HALF_UP);
            // 直线法月折旧额不能超过剩余可折旧额
            BigDecimal remaining = depreciableAmount.subtract(accumulatedDepreciation);
            return slMonthly.compareTo(remaining) > 0 ? remaining : slMonthly;
        }

        // DDB 月折旧额 = 账面净值 × 2 / 使用年限月数
        // 注意：必须「先乘后除、只舍入一次」。若先把月折旧率 2/n 预舍入成有限小数再相乘，
        // 会引入可观的精度损失——例如 2/60 预舍入 6 位得 0.033333，乘以 100000 得 3333.30，
        // 而正确值为 3333.33；该误差还会随折旧期数累积，最终全部挤压到尾月尾差中。
        BigDecimal ddbMonthly = netBookValue.multiply(BigDecimal.valueOf(2))
                .divide(BigDecimal.valueOf(usefulLifeMonths), SCALE, RoundingMode.HALF_UP);
        // 不能超过剩余可折旧额
        BigDecimal remaining = depreciableAmount.subtract(accumulatedDepreciation);
        return ddbMonthly.compareTo(remaining) > 0 ? remaining : ddbMonthly;
    }

    /**
     * 年数总和法（SYD）月折旧额，含尾差处理
     *
     * <p>公式：
     * <pre>
     *   年数总和 = n × (n + 1) / 2，n = 使用年限年数
     *   年折旧率 = 剩余使用年数 / 年数总和
     *   月折旧额 = (原值 - 残值) × 年折旧率 / 12
     * </pre>
     *
     * <p>尾差处理：最后一个月取剩余可折旧额，确保账面净值恰好等于残值。
     */
    private BigDecimal calculateSydMonthlyAmount(ErpFixedAssetDO asset) {
        BigDecimal originalValue = asset.getOriginalValue();
        BigDecimal salvage = asset.getSalvageValue() == null ? BigDecimal.ZERO : asset.getSalvageValue();
        int usefulLifeMonths = asset.getUsefulLifeMonths();
        int depreciatedMonths = asset.getDepreciatedMonths() == null ? 0 : asset.getDepreciatedMonths();
        BigDecimal accumulatedDepreciation = asset.getAccumulatedDepreciation() == null
                ? BigDecimal.ZERO : asset.getAccumulatedDepreciation();
        // 可折旧总额 = 原值 - 残值
        BigDecimal depreciableAmount = originalValue.subtract(salvage);

        // 尾差处理：最后一个月取剩余可折旧额
        if (depreciatedMonths + 1 >= usefulLifeMonths) {
            BigDecimal remaining = depreciableAmount.subtract(accumulatedDepreciation);
            return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
        }

        // 使用年限年数（向上取整，确保折旧期覆盖全部月份）
        int usefulLifeYears = (usefulLifeMonths + 11) / 12;
        // 年数总和 = n × (n + 1) / 2
        int sumOfYears = usefulLifeYears * (usefulLifeYears + 1) / 2;
        // 已折旧年数（已折旧月数 / 12，取整）
        int elapsedYears = depreciatedMonths / 12;
        // 剩余使用年数（含当年）
        int remainingYears = usefulLifeYears - elapsedYears;
        // 月折旧额 = (原值 - 残值) × 剩余年数 / (年数总和 × 12)
        // 同 DDB：合并为一次除法、只舍入一次，避免「先除年数总和再除 12」两次舍入带来的精度损失
        BigDecimal sydMonthly = depreciableAmount
                .multiply(BigDecimal.valueOf(remainingYears))
                .divide(BigDecimal.valueOf((long) sumOfYears * 12), SCALE, RoundingMode.HALF_UP);
        // 不能超过剩余可折旧额
        BigDecimal remaining = depreciableAmount.subtract(accumulatedDepreciation);
        return sydMonthly.compareTo(remaining) > 0 ? remaining : sydMonthly;
    }

}
