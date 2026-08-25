package cn.zhicloud.module.ai.service.predictive;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.zhicloud.module.ai.dal.dataobject.model.AiModelDO;
import cn.zhicloud.module.ai.enums.model.AiModelTypeEnum;
import cn.zhicloud.module.ai.service.model.AiModelService;
import cn.zhicloud.module.mes.service.dv.gateway.DeviceMaintenanceDataGateway;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.ai.enums.ErrorCodeConstants.PREDICTIVE_DATA_UNAVAILABLE;
import static cn.zhicloud.module.ai.enums.ErrorCodeConstants.PREDICTIVE_DEVICE_NOT_EXISTS;

/**
 * 预测性维护 AI Service 实现类
 *
 * <h3>预测算法（指数衰减模型 + MTBF 趋势分析）</h3>
 * <pre>
 * 1. 基础概率：基于"距离上次维修时间 / MTBF"的指数衰减
 *    - ratio = daysSinceLastRepair / mtbfDays
 *    - baseProbability = 1 - exp(-ratio)  （ratio 越大，概率越趋近 1）
 *
 * 2. MTBF 趋势修正：
 *    - 比较最近两期 MTBF，若下降（mtbfLatest < mtbfPrev）则故障率上升
 *    - trendFactor = mtbfPrev / mtbfLatest（>1 表示恶化）
 *    - probability = baseProbability * trendFactor
 *
 * 3. 故障频率修正：
 *    - 维修次数越多，设备越脆弱
 *    - repairCount > 10 → 概率 ×1.2；> 20 → ×1.5
 *
 * 4. 截断到 [0, 1] 区间
 * </pre>
 *
 * @author zhicloud
 */
@Service
@Slf4j
public class PredictiveMaintenanceServiceImpl implements PredictiveMaintenanceService {

    /**
     * 高风险概率阈值
     */
    private static final double HIGH_RISK_THRESHOLD = 0.7;
    /**
     * 中风险概率阈值
     */
    private static final double MEDIUM_RISK_THRESHOLD = 0.3;
    /**
     * 默认 MTBF（小时），当历史 KPI 缺失时使用
     */
    private static final double DEFAULT_MTBF_HOURS = 720.0; // 30 天
    /**
     * 维修次数-脆弱度修正阈值
     */
    private static final int REPAIR_COUNT_MODERATE = 10;
    private static final int REPAIR_COUNT_HIGH = 20;

    /**
     * 设备维护数据 SPI 网关（可选；mes 模块未加载时为 null）
     */
    @Autowired(required = false)
    private DeviceMaintenanceDataGateway dataGateway;

    /**
     * ChatClient Bean（可选；用于自然语言分析增强）
     */
    @Autowired(required = false)
    private ChatClient chatClient;

    /**
     * AiModelService（可选；用于在 ChatClient 不可用时按需构建 ChatModel）
     */
    @Autowired(required = false)
    private AiModelService aiModelService;

    @Override
    public FailurePredictionResult predictFailure(Long deviceId) {
        // 1. 校验数据网关可用
        validateGatewayAvailable(deviceId);
        // 2. 采集数据
        LocalDateTime lastRepairTime = dataGateway.getLastRepairTime(deviceId);
        Long repairCount = dataGateway.getRepairCount(deviceId);
        List<DeviceMaintenanceDataGateway.KpiSnapshot> kpiHistory = dataGateway.getKpiHistory(deviceId, 3);
        // 3. 数据可用性校验
        if (repairCount == null) {
            repairCount = 0L;
        }
        if (kpiHistory == null || kpiHistory.isEmpty()) {
            if (lastRepairTime == null) {
                throw exception(PREDICTIVE_DATA_UNAVAILABLE);
            }
            // KPI 缺失但有维修记录：使用默认 MTBF 兜底
            log.warn("[predictFailure][设备 {} 无 KPI 历史，使用默认 MTBF={} 小时]", deviceId, DEFAULT_MTBF_HOURS);
        }
        // 4. 计算故障概率
        double probability = calculateProbability(lastRepairTime, repairCount, kpiHistory);
        // 5. 风险等级
        FailurePredictionResult.RiskLevel riskLevel = classifyRisk(probability);
        // 6. 建议维护日期（基于 MTBF 推算下次故障窗口）
        LocalDate recommendedDate = calculateRecommendedDate(lastRepairTime, kpiHistory);
        // 7. 推理过程
        String reasoning = buildReasoning(deviceId, probability, riskLevel, lastRepairTime, repairCount, kpiHistory);
        log.info("[predictFailure][deviceId={}, probability={}, riskLevel={}, recommendedDate={}]",
                deviceId, probability, riskLevel, recommendedDate);
        return new FailurePredictionResult()
                .setDeviceId(deviceId)
                .setProbability(probability)
                .setRiskLevel(riskLevel)
                .setRecommendedDate(recommendedDate)
                .setReasoning(reasoning);
    }

    @Override
    public String getMaintenanceRecommendation(Long deviceId) {
        FailurePredictionResult result = predictFailure(deviceId);
        // 优先使用 LLM 生成自然语言建议
        String llmAdvice = callLlmForRecommendation(result);
        if (StrUtil.isNotBlank(llmAdvice)) {
            return llmAdvice;
        }
        // 兜底：基于规则的模板建议
        return buildRuleBasedRecommendation(result);
    }

    // ==================== 核心算法 ====================

    /**
     * 计算故障概率（指数衰减 + MTBF 趋势 + 故障频率修正）
     */
    private double calculateProbability(LocalDateTime lastRepairTime, Long repairCount,
                                        List<DeviceMaintenanceDataGateway.KpiSnapshot> kpiHistory) {
        // 1. 获取最新 MTBF（小时）
        double mtbfHours = DEFAULT_MTBF_HOURS;
        double mtbfPrevHours = DEFAULT_MTBF_HOURS;
        if (CollUtil.isNotEmpty(kpiHistory)) {
            DeviceMaintenanceDataGateway.KpiSnapshot latest = kpiHistory.get(kpiHistory.size() - 1);
            if (latest.mtbf() != null && latest.mtbf().doubleValue() > 0) {
                mtbfHours = latest.mtbf().doubleValue();
            }
            if (kpiHistory.size() >= 2) {
                DeviceMaintenanceDataGateway.KpiSnapshot prev = kpiHistory.get(kpiHistory.size() - 2);
                if (prev.mtbf() != null && prev.mtbf().doubleValue() > 0) {
                    mtbfPrevHours = prev.mtbf().doubleValue();
                }
            }
        }
        // 2. 基础概率：指数衰减
        double mtbfDays = mtbfHours / 24.0;
        double daysSinceLastRepair = 0;
        if (lastRepairTime != null) {
            daysSinceLastRepair = ChronoUnit.HOURS.between(lastRepairTime, LocalDateTime.now()) / 24.0;
        }
        double ratio = mtbfDays > 0 ? daysSinceLastRepair / mtbfDays : 0;
        double baseProbability = 1 - Math.exp(-ratio);
        // 3. MTBF 趋势修正（下降趋势 → 概率升高）
        double trendFactor = 1.0;
        if (mtbfHours > 0 && mtbfPrevHours > 0 && mtbfHours < mtbfPrevHours) {
            trendFactor = mtbfPrevHours / mtbfHours;
        }
        double probability = baseProbability * trendFactor;
        // 4. 故障频率修正
        if (repairCount >= REPAIR_COUNT_HIGH) {
            probability *= 1.5;
        } else if (repairCount >= REPAIR_COUNT_MODERATE) {
            probability *= 1.2;
        }
        // 5. 截断到 [0, 1]
        return Math.max(0.0, Math.min(1.0, probability));
    }

    /**
     * 风险等级分类
     */
    private FailurePredictionResult.RiskLevel classifyRisk(double probability) {
        if (probability >= HIGH_RISK_THRESHOLD) {
            return FailurePredictionResult.RiskLevel.HIGH;
        } else if (probability >= MEDIUM_RISK_THRESHOLD) {
            return FailurePredictionResult.RiskLevel.MEDIUM;
        }
        return FailurePredictionResult.RiskLevel.LOW;
    }

    /**
     * 计算建议维护日期（下次故障窗口的 80% 分位）
     *
     * <p>策略：上次维修时间 + MTBF × 0.8，避免等到故障发生才维护。
     */
    private LocalDate calculateRecommendedDate(LocalDateTime lastRepairTime,
                                               List<DeviceMaintenanceDataGateway.KpiSnapshot> kpiHistory) {
        double mtbfHours = DEFAULT_MTBF_HOURS;
        if (CollUtil.isNotEmpty(kpiHistory)) {
            DeviceMaintenanceDataGateway.KpiSnapshot latest = kpiHistory.get(kpiHistory.size() - 1);
            if (latest.mtbf() != null && latest.mtbf().doubleValue() > 0) {
                mtbfHours = latest.mtbf().doubleValue();
            }
        }
        LocalDateTime base = lastRepairTime != null ? lastRepairTime : LocalDateTime.now();
        // MTBF 的 80% 作为预防性维护窗口
        long maintainAfterDays = (long) ((mtbfHours * 0.8) / 24.0);
        return base.plusDays(Math.max(1, maintainAfterDays)).toLocalDate();
    }

    /**
     * 构建推理过程（结构化文本）
     */
    private String buildReasoning(Long deviceId, double probability, FailurePredictionResult.RiskLevel riskLevel,
                                  LocalDateTime lastRepairTime, Long repairCount,
                                  List<DeviceMaintenanceDataGateway.KpiSnapshot> kpiHistory) {
        StringBuilder sb = new StringBuilder();
        sb.append("设备编号：").append(deviceId).append("\n");
        sb.append("故障概率：").append(BigDecimal.valueOf(probability).setScale(4, RoundingMode.HALF_UP)).append("\n");
        sb.append("风险等级：").append(riskLevel).append("\n");
        sb.append("上次维修时间：").append(lastRepairTime != null ? lastRepairTime : "无记录").append("\n");
        sb.append("历史维修次数：").append(repairCount).append("\n");
        if (CollUtil.isNotEmpty(kpiHistory)) {
            DeviceMaintenanceDataGateway.KpiSnapshot latest = kpiHistory.get(kpiHistory.size() - 1);
            sb.append("最近 MTBF：").append(latest.mtbf()).append(" 小时\n");
            sb.append("最近 MTTR：").append(latest.mttr()).append(" 小时\n");
            if (kpiHistory.size() >= 2) {
                DeviceMaintenanceDataGateway.KpiSnapshot prev = kpiHistory.get(kpiHistory.size() - 2);
                BigDecimal latestMtbf = latest.mtbf();
                BigDecimal prevMtbf = prev.mtbf();
                if (latestMtbf != null && prevMtbf != null) {
                    if (latestMtbf.compareTo(prevMtbf) < 0) {
                        sb.append("MTBF 趋势：下降（").append(prevMtbf).append(" → ").append(latestMtbf)
                                .append("），设备可靠性恶化，故障风险升高\n");
                    } else {
                        sb.append("MTBF 趋势：稳定或上升，设备可靠性良好\n");
                    }
                }
            }
        } else {
            sb.append("无 KPI 历史数据，使用默认 MTBF=").append(DEFAULT_MTBF_HOURS).append(" 小时\n");
        }
        if (repairCount >= REPAIR_COUNT_HIGH) {
            sb.append("维修次数 ≥ ").append(REPAIR_COUNT_HIGH).append("，设备脆弱度高，已应用 1.5× 修正系数\n");
        } else if (repairCount >= REPAIR_COUNT_MODERATE) {
            sb.append("维修次数 ≥ ").append(REPAIR_COUNT_MODERATE).append("，设备脆弱度中等，已应用 1.2× 修正系数\n");
        }
        return sb.toString();
    }

    /**
     * 基于规则的模板建议（LLM 不可用时兜底）
     */
    private String buildRuleBasedRecommendation(FailurePredictionResult result) {
        String prefix = StrUtil.format("设备 {} 的故障预测结果：概率 {}，风险等级 {}，建议维护日期 {}。\n",
                result.getDeviceId(), result.getProbability(), result.getRiskLevel(), result.getRecommendedDate());
        String action = switch (result.getRiskLevel()) {
            case HIGH -> "建议立即安排预防性维护，重点检查易损件与关键传动部件，避免非计划停机。";
            case MEDIUM -> "建议在建议维护日期前安排例行点检，并准备备件，关注 MTBF 变化趋势。";
            case LOW -> "设备运行状态良好，按常规计划保养即可，持续监控 KPI 指标。";
        };
        return prefix + action + "\n\n【推理过程】\n" + result.getReasoning();
    }

    // ==================== LLM 调用 ====================

    /**
     * 调用 LLM 生成自然语言维护建议
     *
     * @return LLM 生成的建议文本，不可用时返回 null
     */
    private String callLlmForRecommendation(FailurePredictionResult result) {
        String systemPrompt = "你是一个设备预测性维护专家。根据设备故障预测结果，" +
                "用专业但易懂的中文给出维护建议，包括：维护优先级、建议检查项、备件准备、维护时间窗口。";
        String userPrompt = StrUtil.format(
                "设备编号：{}\n故障概率：{}\n风险等级：{}\n建议维护日期：{}\n\n推理过程：\n{}",
                result.getDeviceId(), result.getProbability(), result.getRiskLevel(),
                result.getRecommendedDate(), result.getReasoning());
        try {
            if (chatClient != null) {
                return chatClient.prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .call()
                        .content();
            }
            if (aiModelService != null) {
                AiModelDO model = aiModelService.getRequiredDefaultModel(AiModelTypeEnum.CHAT.getType());
                ChatModel chatModel = aiModelService.getChatModel(model.getId());
                ChatClient client = ChatClient.builder(chatModel).build();
                return client.prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .call()
                        .content();
            }
        } catch (Exception e) {
            log.warn("[callLlmForRecommendation][LLM 调用失败，降级为规则建议，deviceId={}]",
                    result.getDeviceId(), e);
            return null;
        }
        return null;
    }

    // ==================== 校验 ====================

    /**
     * 校验数据网关可用
     */
    private void validateGatewayAvailable(Long deviceId) {
        if (dataGateway == null) {
            log.warn("[validateGatewayAvailable][设备维护数据网关未注入，deviceId={}]", deviceId);
            throw exception(PREDICTIVE_DEVICE_NOT_EXISTS);
        }
    }

}
