package cn.zhicloud.framework.integration.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 死信人工重放命令（V105）
 *
 * <p>操作者身份由服务端登录上下文注入，禁止直接信任客户端传入的操作者字段。
 * 时间窗用于把误操作限制在已排查的事件范围内，避免一次请求重放任意历史死信。
 *
 * @author 智云集成治理
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDlqReplayCommand {

    /**
     * 待重放事件编号
     */
    private List<String> eventIds;

    /**
     * 操作者用户编号
     */
    private Long operatorId;

    /**
     * 操作者展示名称
     */
    private String operatorName;

    /**
     * 重放原因，必须可追溯到变更或故障处置记录
     */
    private String reason;

    /**
     * 事件业务发生时间下界（含）
     */
    private LocalDateTime fromTime;

    /**
     * 事件业务发生时间上界（含）
     */
    private LocalDateTime toTime;

}
