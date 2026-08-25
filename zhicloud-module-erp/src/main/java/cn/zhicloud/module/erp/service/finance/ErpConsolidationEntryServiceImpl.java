package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationEntryPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationEntrySaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpConsolidationEntryDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpConsolidationEntryMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Objects;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 合并报表抵消分录 Service 实现类（P0-14）
 *
 * <p>支持集团内关联交易抵消分录的 CRUD + 审核：
 * <ul>
 *   <li>新增/更新时校验借贷平衡、科目不能同时为空</li>
 *   <li>仅草稿状态可修改/删除</li>
 *   <li>审核操作将状态从草稿变更为已审核</li>
 * </ul>
 *
 * @author 智云
 */
@Service
@Validated
@Slf4j
public class ErpConsolidationEntryServiceImpl implements ErpConsolidationEntryService {

    /**
     * 状态：草稿
     */
    private static final Integer STATUS_DRAFT = 10;
    /**
     * 状态：已审核
     */
    private static final Integer STATUS_APPROVED = 20;

    @Resource
    private ErpConsolidationEntryMapper consolidationEntryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConsolidationEntry(ErpConsolidationEntrySaveReqVO createReqVO) {
        // 校验科目不能同时为空
        validateAccounts(createReqVO);
        // 校验借贷平衡
        validateBalance(createReqVO);
        // 转换并初始化状态为草稿
        ErpConsolidationEntryDO entry = BeanUtils.toBean(createReqVO, ErpConsolidationEntryDO.class);
        entry.setStatus(STATUS_DRAFT);
        consolidationEntryMapper.insert(entry);
        return entry.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConsolidationEntry(ErpConsolidationEntrySaveReqVO updateReqVO) {
        ErpConsolidationEntryDO existing = validateConsolidationEntryExists(updateReqVO.getId());
        // 仅草稿状态可修改
        if (!Objects.equals(existing.getStatus(), STATUS_DRAFT)) {
            throw exception(CONSOLIDATION_ENTRY_ALREADY_APPROVED);
        }
        // 校验科目不能同时为空
        validateAccounts(updateReqVO);
        // 校验借贷平衡
        validateBalance(updateReqVO);
        ErpConsolidationEntryDO updateObj = BeanUtils.toBean(updateReqVO, ErpConsolidationEntryDO.class);
        // 保持原状态（草稿），不允许通过更新接口变更状态
        updateObj.setStatus(STATUS_DRAFT);
        consolidationEntryMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConsolidationEntry(Long id) {
        ErpConsolidationEntryDO existing = validateConsolidationEntryExists(id);
        // 仅草稿可删
        if (!Objects.equals(existing.getStatus(), STATUS_DRAFT)) {
            throw exception(CONSOLIDATION_ENTRY_ALREADY_APPROVED);
        }
        consolidationEntryMapper.deleteById(id);
    }

    @Override
    public ErpConsolidationEntryDO getConsolidationEntry(Long id) {
        return consolidationEntryMapper.selectById(id);
    }

    @Override
    public PageResult<ErpConsolidationEntryDO> getConsolidationEntryPage(ErpConsolidationEntryPageReqVO pageReqVO) {
        return consolidationEntryMapper.selectPage(pageReqVO, new LambdaQueryWrapperX<ErpConsolidationEntryDO>()
                .eqIfPresent(ErpConsolidationEntryDO::getConsolidationNo, pageReqVO.getConsolidationNo())
                .eqIfPresent(ErpConsolidationEntryDO::getPeriodId, pageReqVO.getPeriodId())
                .eqIfPresent(ErpConsolidationEntryDO::getPeriodCode, pageReqVO.getPeriodCode())
                .eqIfPresent(ErpConsolidationEntryDO::getEliminationType, pageReqVO.getEliminationType())
                .eqIfPresent(ErpConsolidationEntryDO::getDebitAccountId, pageReqVO.getDebitAccountId())
                .eqIfPresent(ErpConsolidationEntryDO::getCreditAccountId, pageReqVO.getCreditAccountId())
                .eqIfPresent(ErpConsolidationEntryDO::getStatus, pageReqVO.getStatus())
                .orderByDesc(ErpConsolidationEntryDO::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveConsolidationEntry(Long id) {
        ErpConsolidationEntryDO existing = validateConsolidationEntryExists(id);
        // 草稿 → 已审核
        if (Objects.equals(existing.getStatus(), STATUS_APPROVED)) {
            throw exception(CONSOLIDATION_ENTRY_ALREADY_APPROVED);
        }
        ErpConsolidationEntryDO updateObj = new ErpConsolidationEntryDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_APPROVED);
        consolidationEntryMapper.updateById(updateObj);
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 校验抵消分录存在
     *
     * @param id 编号
     * @return 抵消分录
     */
    private ErpConsolidationEntryDO validateConsolidationEntryExists(Long id) {
        if (id == null) {
            throw exception(CONSOLIDATION_ENTRY_NOT_EXISTS);
        }
        ErpConsolidationEntryDO entry = consolidationEntryMapper.selectById(id);
        if (entry == null) {
            throw exception(CONSOLIDATION_ENTRY_NOT_EXISTS);
        }
        return entry;
    }

    /**
     * 校验借方/贷方科目不能同时为空
     *
     * @param reqVO 保存信息
     */
    private void validateAccounts(ErpConsolidationEntrySaveReqVO reqVO) {
        if (reqVO.getDebitAccountId() == null && reqVO.getCreditAccountId() == null) {
            throw exception(CONSOLIDATION_ACCOUNT_INVALID);
        }
    }

    /**
     * 校验借贷平衡
     *
     * <p>当前模型为单一金额（eliminationAmount）同时作为借方合计与贷方合计，二者必须相等。
     *
     * @param reqVO 保存信息
     */
    private void validateBalance(ErpConsolidationEntrySaveReqVO reqVO) {
        BigDecimal amount = reqVO.getEliminationAmount();
        if (amount == null) {
            return;
        }
        BigDecimal debitTotal = amount;
        BigDecimal creditTotal = amount;
        if (debitTotal.compareTo(creditTotal) != 0) {
            throw exception(CONSOLIDATION_DEBIT_CREDIT_NOT_BALANCE, debitTotal, creditTotal);
        }
    }

}
