package cn.zhicloud.module.ai.service.predictive;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

/**
 * 故障预测结果
 *
 * <p>包含设备故障概率、风险等级、建议维护时间与推理过程。
 *
 * @author zhicloud
 */
@Data
@Accessors(chain = true)
public class FailurePredictionResult {

    /**
     * 设备编号
     */
    private Long deviceId;
    /**
     * 故障概率（0-1，1 表示必然故障）
     */
    private Double probability;
    /**
     * 风险等级
     *
     * 枚举 {@link RiskLevel}
     */
    private RiskLevel riskLevel;
    /**
     * 建议维护日期
     */
    private LocalDate recommendedDate;
    /**
     * 推理过程（自然语言描述，供 LLM 增强或人工 review）
     */
    private String reasoning;

    /**
     * 风险等级枚举
     */
    public enum RiskLevel {
        /**
         * 高风险（概率 >= 0.7）
         */
        HIGH,
        /**
         * 中风险（0.3 <= 概率 < 0.7）
         */
        MEDIUM,
        /**
         * 低风险（概率 < 0.3）
         */
        LOW
    }

}
