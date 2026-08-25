package cn.zhicloud.module.mes.service.pro.piecework;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.mes.controller.admin.pro.piecework.vo.MesProPieceworkRecordPageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.piecework.MesProPieceworkRecordDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.piecework.MesProPieceworkRuleDO;
import cn.zhicloud.module.mes.dal.mysql.pro.piecework.MesProPieceworkRecordMapper;
import cn.zhicloud.module.mes.service.pro.feedback.MesProFeedbackService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_PIECEWORK_RECORD_FEEDBACK_DUPLICATE;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_PIECEWORK_RULE_NOT_MATCHED;

/**
 * MES 计件工资明细 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
@Slf4j
public class MesProPieceworkRecordServiceImpl implements MesProPieceworkRecordService {

    /**
     * 月份格式（yyyyMM）
     */
    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    @Resource
    private MesProPieceworkRecordMapper pieceworkRecordMapper;
    @Resource
    @Lazy
    private MesProFeedbackService feedbackService;
    @Resource
    @Lazy
    private MesProPieceworkRuleService pieceworkRuleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateRecordFromFeedback(Long feedbackId) {
        // 1. 校验报工存在
        MesProFeedbackDO feedback = feedbackService.getFeedback(feedbackId);
        if (feedback == null) {
            return null;
        }
        // 2. 校验未重复生成
        if (pieceworkRecordMapper.selectByFeedbackId(feedbackId) != null) {
            throw exception(PRO_PIECEWORK_RECORD_FEEDBACK_DUPLICATE);
        }
        // 3. 匹配计件规则
        LocalDate effectDate = feedback.getFeedbackTime() != null
                ? feedback.getFeedbackTime().toLocalDate() : LocalDate.now();
        MesProPieceworkRuleDO rule = pieceworkRuleService.matchRule(
                feedback.getProcessId(), feedback.getItemId(),
                feedback.getWorkstationId(), feedback.getRouteId(), effectDate);
        if (rule == null) {
            log.warn("[generateRecordFromFeedback][报工单 {} 未匹配到计件规则]", feedbackId);
            throw exception(PRO_PIECEWORK_RULE_NOT_MATCHED);
        }
        // 4. 计算工资
        BigDecimal qualifiedQty = nullToZero(feedback.getQualifiedQuantity());
        BigDecimal laborScrapQty = nullToZero(feedback.getLaborScrapQuantity());
        BigDecimal materialScrapQty = nullToZero(feedback.getMaterialScrapQuantity());
        BigDecimal otherScrapQty = nullToZero(feedback.getOtherScrapQuantity());
        BigDecimal scrapQty = laborScrapQty.add(materialScrapQty).add(otherScrapQty);

        BigDecimal unitPrice = nullToZero(rule.getQualifiedUnitPrice());
        BigDecimal scrapUnitPrice = nullToZero(rule.getScrapUnitPrice());
        BigDecimal totalAmount = qualifiedQty.multiply(unitPrice)
                .add(laborScrapQty.multiply(scrapUnitPrice));

        // 5. 构建明细
        String periodMonth = feedback.getFeedbackTime() != null
                ? feedback.getFeedbackTime().format(PERIOD_FORMATTER)
                : LocalDate.now().format(PERIOD_FORMATTER);
        MesProPieceworkRecordDO record = MesProPieceworkRecordDO.builder()
                .feedbackId(feedbackId)
                .feedbackUserId(feedback.getFeedbackUserId())
                .workOrderId(feedback.getWorkOrderId())
                .processId(feedback.getProcessId())
                .itemId(feedback.getItemId())
                .workstationId(feedback.getWorkstationId())
                .qualifiedQty(qualifiedQty)
                .scrapQty(scrapQty)
                .laborScrapQty(laborScrapQty)
                .unitPrice(unitPrice)
                .scrapUnitPrice(scrapUnitPrice)
                .totalAmount(totalAmount)
                .periodMonth(periodMonth)
                .status(0) // 0=正常
                .build();
        pieceworkRecordMapper.insert(record);
        return record.getId();
    }

    @Override
    public MesProPieceworkRecordDO getPieceworkRecord(Long id) {
        return pieceworkRecordMapper.selectById(id);
    }

    @Override
    public PageResult<MesProPieceworkRecordDO> getPieceworkRecordPage(MesProPieceworkRecordPageReqVO pageReqVO) {
        return pieceworkRecordMapper.selectPage(pageReqVO);
    }

    @Override
    public List<MesProPieceworkRecordDO> getPieceworkRecordListByPeriod(String periodMonth) {
        if (periodMonth == null || periodMonth.isEmpty()) {
            return List.of();
        }
        return pieceworkRecordMapper.selectListByPeriod(periodMonth);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

}
