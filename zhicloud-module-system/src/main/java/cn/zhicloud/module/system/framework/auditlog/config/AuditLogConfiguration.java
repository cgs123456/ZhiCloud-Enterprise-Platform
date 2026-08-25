package cn.zhicloud.module.system.framework.auditlog.config;

import cn.zhicloud.module.system.framework.auditlog.core.service.AuditLogPersistService;
import cn.zhicloud.module.system.framework.auditlog.core.service.AuditLogHashChainService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 审计日志配置类
 *
 * 注册：
 * 1. {@link AuditLogProperties} 配置属性
 * 2. {@link AuditLogHashChainService} Hash 链计算与验证服务
 * 3. {@link AuditLogPersistService} 独立文件存储服务
 *
 * @author zhicloud
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuditLogProperties.class)
public class AuditLogConfiguration {

    @Bean
    public AuditLogHashChainService auditLogHashChainService(AuditLogProperties properties) {
        return new AuditLogHashChainService(properties);
    }

    @Bean
    public AuditLogPersistService auditLogPersistService(AuditLogProperties properties) {
        return new AuditLogPersistService(properties);
    }

}
