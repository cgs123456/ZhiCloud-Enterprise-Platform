package cn.zhicloud.framework.common.biz.infra.logger;

import cn.zhicloud.framework.common.biz.infra.logger.dto.ApiErrorLogCreateReqDTO;

import jakarta.validation.Valid;
import org.springframework.scheduling.annotation.Async;

/**
 * API 错误日志的 API 接口
 *
 * @author 智云
 */
public interface ApiErrorLogCommonApi {

    /**
     * 创建 API 错误日志
     *
     * @param createDTO 创建信息
     */
    void createApiErrorLog(@Valid ApiErrorLogCreateReqDTO createDTO);

    /**
     * 【异步】创建 API 异常日志
     *
     * <p>指定审计日志专属线程池 auditLogExecutor（定义见 ZhiCloudAsyncAutoConfiguration，
     * 此处使用字符串避免 common 模块反向依赖 starter）
     *
     * @param createDTO 异常日志 DTO
     */
    @Async("auditLogExecutor")
    default void createApiErrorLogAsync(ApiErrorLogCreateReqDTO createDTO) {
        createApiErrorLog(createDTO);
    }

}
