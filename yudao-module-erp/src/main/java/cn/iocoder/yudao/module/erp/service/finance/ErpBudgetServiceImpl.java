package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.budget.ErpBudgetPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.budget.ErpBudgetSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpBudgetDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpBudgetDetailDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpGlAccountDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpBudgetDetailMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpBudgetMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpGlAccountMapper;
import cn.iocoder.yudao.module.erp.enums.finance.ErpBudgetStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 预算 Service 实现类（P0-14）
 *
 * <p>核心实现：
 * <ul>
 *   <li>创建/更新时校验期间唯一性、明细金额合法性</li>
 *   <li>预算总额由明细金额合计自动计算</li>
 *   <li>仅草稿状态可修改/删除</li>
 *   <li>审批：草稿 → 已审批</li>
 * </ul>
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class ErpBudgetServiceImpl implements ErpBudgetService {

    @Resource
    private ErpBudgetMapper budgetMapper;
    @Resource
    private ErpBudgetDetailMapper budgetDetailMapper;
    @Resource
    private ErpGlAccountMapper glAccountMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBudget(ErpBudgetSaveReqVO createReqVO) {
        // 校验期间唯一性
        validatePeriodUnique(createReqVO, null);
        // 校验明细金额 + 计算合计
        BigDecimal totalAmount = validateAndSumDetails(createReqVO.getDetails());
        // 保存预算主表
        ErpBudgetDO budget = BeanUtils.toBean(createReqVO, ErpBudgetDO.class);
        budget.setTotalAmount(totalAmount);
        budget.setStatus(ErpBudgetStatusEnum.DRAFT.getStatus());
        budgetMapper.insert(budget);
        // 保存明细
        saveDetails(budget.getId(), createReqVO.getDetails());
        return budget.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBudget(ErpBudgetSaveReqVO updateReqVO) {
        ErpBudgetDO existing = validateBudgetExists(updateReqVO.getId());
        // 仅草稿状态可修改
        if (!ErpBudgetStatusEnum.DRAFT.getStatus().equals(existing.getStatus())) {
            throw exception(BUDGET_STATUS_NOT_DRAFT, existing.getBudgetNo());
        }
        // 校验期间唯一性（排除自身）
        validatePeriodUnique(updateReqVO, updateReqVO.getId());
        // 校验明细金额 + 计算合计
        BigDecimal totalAmount = validateAndSumDetails(updateReqVO.getDetails());
        // 更新主表
        ErpBudgetDO updateObj = BeanUtils.toBean(updateReqVO, ErpBudgetDO.class);
        updateObj.setTotalAmount(totalAmount);
        budgetMapper.updateById(updateObj);
        // 删除旧明细，重新插入
        budgetDetailMapper.deleteByBudgetId(updateReqVO.getId());
        saveDetails(updateReqVO.getId(), updateReqVO.getDetails());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBudget(Long id) {
        ErpBudgetDO existing = validateBudgetExists(id);
        // 仅草稿可删
        if (!ErpBudgetStatusEnum.DRAFT.getStatus().equals(existing.getStatus())) {
            throw exception(BUDGET_STATUS_NOT_DRAFT, existing.getBudgetNo());
        }
        // 级联删除明细
        budgetDetailMapper.deleteByBudgetId(id);
        budgetMapper.deleteById(id);
    }

    @Override
    public ErpBudgetDO getBudget(Long id) {
        return budgetMapper.selectById(id);
    }

    @Override
    public PageResult<ErpBudgetDO> getBudgetPage(ErpBudgetPageReqVO pageReqVO) {
        return budgetMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveBudget(Long id) {
        ErpBudgetDO existing = validateBudgetExists(id);
        // 草稿 → 已审批；非草稿不允许重复审批
        if (!ErpBudgetStatusEnum.DRAFT.getStatus().equals(existing.getStatus())) {
            throw exception(BUDGET_ALREADY_APPROVED, existing.getBudgetNo());
        }
        ErpBudgetDO updateObj = new ErpBudgetDO();
        updateObj.setId(id);
        updateObj.setStatus(ErpBudgetStatusEnum.APPROVED.getStatus());
        budgetMapper.updateById(updateObj);
    }

    @Override
    public List<ErpBudgetDetailDO> getBudgetDetailListByBudgetId(Long budgetId) {
        return budgetDetailMapper.selectListByBudgetId(budgetId);
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 校验预算存在
     */
    private ErpBudgetDO validateBudgetExists(Long id) {
        if (id == null) {
            throw exception(BUDGET_NOT_EXISTS);
        }
        ErpBudgetDO budget = budgetMapper.selectById(id);
        if (budget == null) {
            throw exception(BUDGET_NOT_EXISTS);
        }
        return budget;
    }

    /**
     * 校验期间唯一性：同一预算年度 + 会计期间 + 预算类型下不能重复
     *
     * @param reqVO      预算信息
     * @param excludeId  排除的预算编号（更新时传自身编号，创建时传 null）
     */
    private void validatePeriodUnique(ErpBudgetSaveReqVO reqVO, Long excludeId) {
        LambdaQueryWrapperX<ErpBudgetDO> wrapper = new LambdaQueryWrapperX<ErpBudgetDO>()
                .eq(ErpBudgetDO::getBudgetYear, reqVO.getBudgetYear());
        // 会计期间：null 表示年度预算，需用 isNull 匹配
        if (reqVO.getPeriodId() != null) {
            wrapper.eq(ErpBudgetDO::getPeriodId, reqVO.getPeriodId());
        } else {
            wrapper.isNull(ErpBudgetDO::getPeriodId);
        }
        // 预算类型
        if (reqVO.getBudgetType() != null) {
            wrapper.eq(ErpBudgetDO::getBudgetType, reqVO.getBudgetType());
        } else {
            wrapper.isNull(ErpBudgetDO::getBudgetType);
        }
        if (excludeId != null) {
            wrapper.ne(ErpBudgetDO::getId, excludeId);
        }
        List<ErpBudgetDO> existing = budgetMapper.selectList(wrapper);
        if (existing != null && !existing.isEmpty()) {
            String periodLabel = reqVO.getPeriodCode() != null
                    ? reqVO.getPeriodCode() : String.valueOf(reqVO.getBudgetYear());
            throw exception(BUDGET_PERIOD_DUPLICATE, periodLabel);
        }
    }

    /**
     * 校验明细金额合法性（必须 >= 0），并返回合计
     */
    private BigDecimal validateAndSumDetails(List<ErpBudgetSaveReqVO.Detail> details) {
        BigDecimal total = BigDecimal.ZERO;
        for (ErpBudgetSaveReqVO.Detail detail : details) {
            if (detail.getBudgetAmount() == null
                    || detail.getBudgetAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw exception(BUDGET_DETAIL_AMOUNT_INVALID);
            }
            total = total.add(detail.getBudgetAmount());
        }
        return total;
    }

    /**
     * 保存预算明细，自动填充科目编码/名称（冗余字段）
     */
    private void saveDetails(Long budgetId, List<ErpBudgetSaveReqVO.Detail> details) {
        if (details == null || details.isEmpty()) {
            return;
        }
        for (ErpBudgetSaveReqVO.Detail detail : details) {
            ErpBudgetDetailDO detailDO = BeanUtils.toBean(detail, ErpBudgetDetailDO.class);
            detailDO.setId(null); // 新插入，忽略前端传入的 id
            detailDO.setBudgetId(budgetId);
            // 冗余填充科目编码/名称
            ErpGlAccountDO account = glAccountMapper.selectById(detail.getAccountId());
            if (account != null) {
                detailDO.setAccountCode(account.getCode());
                detailDO.setAccountName(account.getName());
            }
            // 初始化实际/差异字段
            detailDO.setActualAmount(BigDecimal.ZERO);
            detailDO.setVarianceAmount(BigDecimal.ZERO);
            detailDO.setVarianceRate(BigDecimal.ZERO);
            if (detailDO.getSort() == null) {
                detailDO.setSort(0);
            }
            budgetDetailMapper.insert(detailDO);
        }
    }

}
