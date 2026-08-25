package cn.zhicloud.module.system.framework.auditlog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 审计日志相关配置项
 *
 * 对应配置前缀：zhicloud.audit-log
 *
 * @author zhicloud
 */
@ConfigurationProperties(prefix = "zhicloud.audit-log")
@Data
public class AuditLogProperties {

    /**
     * Hash 链式审计配置
     */
    private HashChain hashChain = new HashChain();

    /**
     * 独立文件存储配置
     */
    private IndependentStorage independentStorage = new IndependentStorage();

    @Data
    public static class HashChain {

        /**
         * 是否启用 Hash 链式审计
         */
        private boolean enabled = true;

    }

    @Data
    public static class IndependentStorage {

        /**
         * 是否启用独立文件存储
         */
        private boolean enabled = true;

        /**
         * 独立存储路径
         */
        private String path = "${AUDIT_LOG_PATH:logs/audit}";

    }

}
