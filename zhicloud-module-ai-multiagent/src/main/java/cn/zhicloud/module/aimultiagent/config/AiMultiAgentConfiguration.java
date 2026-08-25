package cn.zhicloud.module.aimultiagent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 多 Agent 编排模块自动配置
 *
 * 本模块的核心 Bean（Service、Agent、Mapper 等）通过 {@code @Service} / {@code @Component} 注解自动注册，
 * 此处仅作为模块配置入口，并记录启动日志。
 *
 * 关于 ChatClient：本模块不直接创建 ChatClient Bean，而是通过 {@link ChatClientHelper} 在运行时按需构建，
 * 以避免无 LLM API key 时容器启动失败。
 *
 * @author zhicloud
 */
@Configuration
@EnableConfigurationProperties(MultiAgentProperties.class)
@Slf4j
public class AiMultiAgentConfiguration {

}
