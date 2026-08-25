package cn.zhicloud.framework.common.biz.infra.logger;

import cn.zhicloud.framework.common.biz.infra.logger.dto.ApiAccessLogCreateReqDTO;
import jakarta.validation.Valid;
import org.springframework.scheduling.annotation.Async;

/**
 * API 访问日志的 API 接口
 *
 * @author 智云
 */
public interface ApiAccessLogCommonApi {

    /**
     * 创建 API 访问日志
     *
     * @param createDTO 创建信息
     */
    void createApiAccessLog(@Valid ApiAccessLogCreateReqDTO createDTO);

    /**
     * 【异步】创建 API 访问日志
     *
     * <p>指定审计日志专属线程池 auditLogExecutor（定义见 ZhiCloudAsyncAutoConfiguration，
     * 此处使用字符串避免 common 模块反向依赖 starter）
     *
     * @param createDTO 访问日志 DTO
     */
    @Async("auditLogExecutor")
    default void createApiAccessLogAsync(ApiAccessLogCreateReqDTO createDTO) {
        createApiAccessLog(createDTO);
    }

}
