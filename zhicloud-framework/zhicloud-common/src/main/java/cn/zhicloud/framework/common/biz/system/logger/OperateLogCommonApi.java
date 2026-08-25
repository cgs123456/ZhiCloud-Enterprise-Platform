package cn.zhicloud.framework.common.biz.system.logger;

import cn.zhicloud.framework.common.biz.system.logger.dto.OperateLogCreateReqDTO;
import jakarta.validation.Valid;
import org.springframework.scheduling.annotation.Async;

/**
 * 操作日志 API 接口
 *
 * @author 智云
 */
public interface OperateLogCommonApi {

    /**
     * 创建操作日志
     *
     * @param createReqDTO 请求
     */
    void createOperateLog(@Valid OperateLogCreateReqDTO createReqDTO);

    /**
     * 【异步】创建操作日志
     *
     * <p>指定审计日志专属线程池 auditLogExecutor（定义见 ZhiCloudAsyncAutoConfiguration，
     * 此处使用字符串避免 common 模块反向依赖 starter）
     *
     * @param createReqDTO 请求
     */
    @Async("auditLogExecutor")
    default void createOperateLogAsync(OperateLogCreateReqDTO createReqDTO) {
        createOperateLog(createReqDTO);
    }

}
