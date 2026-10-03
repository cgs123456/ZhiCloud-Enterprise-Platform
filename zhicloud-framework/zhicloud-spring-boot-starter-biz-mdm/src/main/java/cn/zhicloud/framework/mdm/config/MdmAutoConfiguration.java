package cn.zhicloud.framework.mdm.config;

import cn.zhicloud.framework.mdm.service.MdmCrossRefService;
import cn.zhicloud.framework.mdm.service.MdmCrossRefServiceImpl;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 主数据跨域映射自动配置（V101）
 *
 * <p>Mapper 位于 {@code cn.zhicloud.framework.mdm.dal.mysql}，不在业务模块默认的
 * {@code zhicloud.info.base-package}（{@code cn.zhicloud.module}）扫描范围内，
 * 故用 {@link MapperScan} 显式限定包，避免与全局扫描冲突。
 *
 * @author 智云集成治理
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "zhicloud.mdm", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MdmProperties.class)
@MapperScan("cn.zhicloud.framework.mdm.dal.mysql")
public class MdmAutoConfiguration {

    @Bean
    public MdmCrossRefService mdmCrossRefService() {
        return new MdmCrossRefServiceImpl();
    }

}