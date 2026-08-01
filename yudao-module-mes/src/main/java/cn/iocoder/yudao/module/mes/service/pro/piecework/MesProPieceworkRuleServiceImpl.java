package cn.iocoder.yudao.module.mes.service.pro.piecework;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.piecework.vo.MesProPieceworkRulePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.piecework.vo.MesProPieceworkRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.piecework.MesProPieceworkRuleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.piecework.MesProPieceworkRuleMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;

/**
 * MES 计件工资规则 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class MesProPieceworkRuleServiceImpl implements MesProPieceworkRuleService {

    @Resource
    private MesProPieceworkRuleMapper pieceworkRuleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPieceworkRule(MesProPieceworkRuleSaveReqVO createReqVO) {
        validateRuleDateRange(createReqVO);
        validateRulePrice(createReqVO);
        MesProPieceworkRuleDO rule = BeanUtils.toBean(createReqVO, MesProPieceworkRuleDO.class);
        pieceworkRuleMapper.insert(rule);
        return rule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePieceworkRule(MesProPieceworkRuleSaveReqVO updateReqVO) {
        validatePieceworkRuleExists(updateReqVO.getId());
        validateRuleDateRange(updateReqVO);
        validateRulePrice(updateReqVO);
        MesProPieceworkRuleDO updateObj = BeanUtils.toBean(updateReqVO, MesProPieceworkRuleDO.class);
        pieceworkRuleMapper.updateById(updateObj);
    }

    @Override
    public void deletePieceworkRule(Long id) {
        validatePieceworkRuleExists(id);
        pieceworkRuleMapper.deleteById(id);
    }

    @Override
    public MesProPieceworkRuleDO getPieceworkRule(Long id) {
        return pieceworkRuleMapper.selectById(id);
    }

    @Override
    public PageResult<MesProPieceworkRuleDO> getPieceworkRulePage(MesProPieceworkRulePageReqVO pageReqVO) {
        return pieceworkRuleMapper.selectPage(pageReqVO);
    }

    @Override
    public MesProPieceworkRuleDO matchRule(Long processId, Long itemId, Long workstationId, Long routeId, LocalDate effectDate) {
        LocalDate date = effectDate != null ? effectDate : LocalDate.now();
        List<MesProPieceworkRuleDO> candidates = pieceworkRuleMapper.selectEnabledEffectiveRules(date);
        if (CollUtil.isEmpty(candidates)) {
            return null;
        }
        // 过滤匹配维度（规则字段为空表示不限；非空则要求精确匹配）
        List<MesProPieceworkRuleDO> filtered = candidates.stream()
                .filter(r -> matchNullable(r.getProcessId(), processId))
                .filter(r -> matchNullable(r.getItemId(), itemId))
                .filter(r -> matchNullable(r.getWorkstationId(), workstationId))
                .filter(r -> matchNullable(r.getRouteId(), routeId))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(filtered)) {
            return null;
        }
        // 按匹配精度排序：非空字段越多越优先；同精度取 id 最大（最近创建）
        filtered.sort((a, b) -> {
            int scoreA = countNonNull(a);
            int scoreB = countNonNull(b);
            if (scoreA != scoreB) {
                return Integer.compare(scoreB, scoreA); // 高分优先
            }
            return Long.compare(b.getId(), a.getId()); // id 大优先
        });
        return filtered.get(0);
    }

    private void validatePieceworkRuleExists(Long id) {
        if (pieceworkRuleMapper.selectById(id) == null) {
            throw exception(PRO_PIECEWORK_RULE_NOT_EXISTS);
        }
    }

    private void validateRuleDateRange(MesProPieceworkRuleSaveReqVO reqVO) {
        if (reqVO.getEffectiveDate() == null || reqVO.getExpireDate() == null) {
            return;
        }
        if (!reqVO.getEffectiveDate().isBefore(reqVO.getExpireDate())) {
            throw exception(PRO_PIECEWORK_RULE_DATE_RANGE_INVALID);
        }
    }

    private void validateRulePrice(MesProPieceworkRuleSaveReqVO reqVO) {
        if (reqVO.getQualifiedUnitPrice() != null
                && reqVO.getQualifiedUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw exception(PRO_PIECEWORK_RULE_PRICE_INVALID);
        }
        if (reqVO.getScrapUnitPrice() != null
                && reqVO.getScrapUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw exception(PRO_PIECEWORK_RULE_PRICE_INVALID);
        }
    }

    /**
     * 规则字段为空表示「不限」，视为匹配；非空则要求精确相等
     */
    private boolean matchNullable(Long ruleValue, Long inputValue) {
        if (ruleValue == null) {
            return true;
        }
        if (inputValue == null) {
            return false;
        }
        return ObjectUtil.equal(ruleValue, inputValue);
    }

    private int countNonNull(MesProPieceworkRuleDO rule) {
        int score = 0;
        if (rule.getProcessId() != null) score++;
        if (rule.getItemId() != null) score++;
        if (rule.getWorkstationId() != null) score++;
        if (rule.getRouteId() != null) score++;
        return score;
    }

}
