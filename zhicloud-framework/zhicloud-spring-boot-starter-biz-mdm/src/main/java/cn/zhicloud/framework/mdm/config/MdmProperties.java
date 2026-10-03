package cn.zhicloud.framework.mdm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 主数据跨域映射配置（V101）
 *
 * @author 智云集成治理
 */
@ConfigurationProperties(prefix = "zhicloud.mdm")
@Data
public class MdmProperties {

    /**
     * 是否启用 MDM Starter，默认启用
     */
    private Boolean enabled = true;

    /**
     * 未显式传租户时使用的默认租户编号，与可靠集成数据面保持一致
     */
    private Long defaultTenantId = 0L;

}