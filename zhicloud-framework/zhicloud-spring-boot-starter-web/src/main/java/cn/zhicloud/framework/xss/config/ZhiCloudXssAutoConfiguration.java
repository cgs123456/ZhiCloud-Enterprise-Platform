package cn.zhicloud.framework.xss.config;

import cn.zhicloud.framework.common.enums.WebFilterOrderEnum;
import cn.zhicloud.framework.xss.core.clean.JsoupXssCleaner;
import cn.zhicloud.framework.xss.core.clean.XssCleaner;
import cn.zhicloud.framework.xss.core.filter.XssFilter;
import cn.zhicloud.framework.xss.core.json.XssStringJsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static cn.zhicloud.framework.web.config.ZhiCloudWebAutoConfiguration.createFilterBean;

@AutoConfiguration
@EnableConfigurationProperties(XssProperties.class)
@ConditionalOnProperty(prefix = "zhicloud.xss", name = "enable", havingValue = "true", matchIfMissing = true) // 设置为 false 时，禁用
public class ZhiCloudXssAutoConfiguration implements WebMvcConfigurer {

    /**
     * Xss 清理者
     *
     * @return XssCleaner
     */
    @Bean
    @ConditionalOnMissingBean(XssCleaner.class)
    public XssCleaner xssCleaner() {
        return new JsoupXssCleaner();
    }

    /**
     * 只在「Web MVC 请求体反序列化」这一条链上做 XSS 过滤。
     * <p>
     * 原先注册 {@link Jackson2ObjectMapperBuilderCustomizer} 会改到 Spring 全局 ObjectMapper，
     * 而全局 mapper 又被 {@code JsonUtils} 与 MyBatis 的 {@code JacksonTypeHandler} 复用，
     * 于是数据库 JSON 列里存的 {@code >} 读出来变成 {@code &gt;}：
     * IoT 场景联动的比较符被静默改写，规则永远匹配不上（真机复现，见 handoff §12.100.2）。
     *
     * @return 只作用于消息转换器的 WebMvcConfigurer
     */
    @Bean
    @ConditionalOnMissingBean(name = "xssMessageConverterConfigurer")
    @ConditionalOnProperty(value = "zhicloud.xss.enable", havingValue = "true")
    public WebMvcConfigurer xssMessageConverterConfigurer(XssProperties properties, PathMatcher pathMatcher,
                                                          XssCleaner xssCleaner) {
        return new WebMvcConfigurer() {

            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                for (HttpMessageConverter<?> converter : converters) {
                    if (!(converter instanceof AbstractJackson2HttpMessageConverter jacksonConverter)) {
                        continue;
                    }
                    SimpleModule xssModule = new SimpleModule();
                    xssModule.addDeserializer(String.class,
                            new XssStringJsonDeserializer(properties, pathMatcher, xssCleaner));
                    // 用副本替换，避免把 XSS 模块带回全局 mapper
                    jacksonConverter.setObjectMapper(jacksonConverter.getObjectMapper().copy().registerModule(xssModule));
                }
            }

        };
    }

    /**
     * 创建 XssFilter Bean，解决 Xss 安全问题
     */
    @Bean
    @ConditionalOnBean(XssCleaner.class)
    public FilterRegistrationBean<XssFilter> xssFilter(XssProperties properties, PathMatcher pathMatcher, XssCleaner xssCleaner) {
        return createFilterBean(new XssFilter(properties, pathMatcher, xssCleaner), WebFilterOrderEnum.XSS_FILTER);
    }

}
