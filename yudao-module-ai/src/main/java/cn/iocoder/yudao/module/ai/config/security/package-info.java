/**
 * AI 安全审计四道防线运行时组件包（Task 14）
 *
 * <p>对应配置类 {@link cn.iocoder.yudao.module.ai.config.AiSecurityConfiguration}，
 * 文档：{@code .trae/specs/upgrade-tech-stack-and-ai-native/ai-security-audit.md}。
 *
 * <h3>四道防线</h3>
 * <ol>
 *   <li>SubTask 14.1 工具白/黑名单 —— {@link cn.iocoder.yudao.module.ai.config.security.ToolAccessInterceptor}
 *       拦截 {@link org.springframework.ai.tool.annotation.Tool} 方法，按白/黑名单规则放行或拦截，
 *       敏感工具触发二次确认流程（生成 confirmToken，5 分钟内凭 token 二次调用放行）。</li>
 *   <li>SubTask 14.2 提示词注入防护 —— {@link cn.iocoder.yudao.module.ai.config.security.PromptInjectionAspect}
 *       拦截 @Tool 方法的 String 参数以及 Controller 的 @RequestBody 参数，按正则黑名单匹配，
 *       命中处置策略 block 直接抛 400，warn 仅记录告警并在阈值触发后升级为 block。</li>
 *   <li>SubTask 14.3 Token 限流 —— {@link cn.iocoder.yudao.module.ai.config.security.TokenLimitFilter}
 *       基于 Servlet Filter + Redis Lua 滑动窗口计数，按用户（每小时）/租户（每天）/IP（每分钟）
 *       三维度限流，超限返回 HTTP 429，superadmin 角色跳过。</li>
 *   <li>SubTask 14.4 PII 脱敏 —— {@link cn.iocoder.yudao.module.ai.config.security.PiiMaskConverter}
 *       基于 {@link org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice}，
 *       在响应序列化前对 String 字段进行手机号/身份证/邮箱/银行卡脱敏。</li>
 * </ol>
 *
 * <h3>加载条件</h3>
 * <p>所有组件均通过 {@code @ConditionalOnProperty} 控制：
 * <ul>
 *   <li>{@code yudao.ai.security.token-limit.enabled=true}</li>
 *   <li>{@code yudao.ai.security.tool-access.whitelist-enabled=true}</li>
 *   <li>{@code yudao.ai.security.prompt-injection.enabled=true}</li>
 *   <li>{@code yudao.ai.security.pii-mask.enabled=true}</li>
 * </ul>
 * <p>默认关闭，未配置时不改变现有行为。RedisTemplate/SecurityFrameworkService 等可空 Bean
 * 使用 {@code @Autowired(required=false)} + {@code @Lazy} 注入，保证容器启动安全。
 *
 * @author 芋道源码
 */
package cn.iocoder.yudao.module.ai.config.security;
