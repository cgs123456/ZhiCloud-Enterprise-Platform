package cn.iocoder.yudao.module.ai.service.predictive;

/**
 * 预测性维护 AI Service 接口
 *
 * <p>基于设备历史维修记录、OEE 数据、点检记录预测故障概率，并给出维护建议。
 *
 * <h3>算法说明</h3>
 * <ul>
 *   <li>基于历史 MTBF/MTTR 趋势 + 故障频率 + 上次维修时间</li>
 *   <li>MTBF 呈下降趋势 → 故障概率升高</li>
 *   <li>距离上次维修时间越接近 MTBF → 概率越高（指数衰减模型）</li>
 *   <li>可选：调用 ChatClient 做自然语言分析增强</li>
 * </ul>
 *
 * @author yudao
 */
public interface PredictiveMaintenanceService {

    /**
     * 预测设备故障
     *
     * @param deviceId 设备编号
     * @return 故障预测结果（概率、风险等级、建议维护时间、推理过程）
     */
    FailurePredictionResult predictFailure(Long deviceId);

    /**
     * 获取维护建议（自然语言）
     *
     * @param deviceId 设备编号
     * @return 维护建议文本
     */
    String getMaintenanceRecommendation(Long deviceId);

}
