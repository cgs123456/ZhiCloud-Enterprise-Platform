package cn.zhicloud.module.system.service.sms;

import cn.zhicloud.framework.datasource.config.ZhiCloudDataSourceAutoConfiguration;
import cn.zhicloud.framework.mybatis.config.ZhiCloudMybatisAutoConfiguration;
import cn.zhicloud.framework.mybatis.core.util.MyBatisUtils;
import cn.zhicloud.framework.tenant.config.TenantProperties;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.zhicloud.framework.tenant.core.util.TenantUtils;
import cn.zhicloud.framework.test.config.SqlInitializationTestConfiguration;
import cn.hutool.extra.spring.SpringUtil;
import cn.zhicloud.module.system.api.sms.dto.code.SmsCodeSendReqDTO;
import cn.zhicloud.module.system.api.sms.dto.code.SmsCodeValidateReqDTO;
import cn.zhicloud.module.system.dal.dataobject.sms.SmsCodeDO;
import cn.zhicloud.module.system.dal.mysql.sms.SmsCodeMapper;
import cn.zhicloud.module.system.enums.sms.SmsSceneEnum;
import cn.zhicloud.module.system.framework.sms.config.SmsCodeProperties;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.time.Duration;
import java.time.LocalDateTime;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.system.enums.ErrorCodeConstants.SMS_CODE_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * 短信验证码的租户隔离回归测试。
 *
 * <p>登录发码接口可以没有租户上下文，但验证码数据本身不能因此变成全局共享数据。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = SmsCodeTenantIsolationTest.TestApplication.class)
@ActiveProfiles("unit-test")
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class SmsCodeTenantIsolationTest {

    @Import({
            ZhiCloudDataSourceAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            SqlInitializationTestConfiguration.class,
            ZhiCloudMybatisAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            MybatisPlusJoinAutoConfiguration.class,
            SmsCodeServiceImpl.class,
            SpringUtil.class,
            TenantInterceptorTestConfiguration.class
    })
    public static class TestApplication {
    }

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    public static class TenantInterceptorTestConfiguration {

        @Bean
        public TenantProperties tenantProperties() {
            return new TenantProperties();
        }

        @Bean
        @org.springframework.context.annotation.Lazy(false)
        public TenantLineInnerInterceptor tenantLineInnerInterceptor(TenantProperties properties,
                                                                     MybatisPlusInterceptor interceptor) {
            TenantLineInnerInterceptor inner = new TenantLineInnerInterceptor(
                    new TenantDatabaseInterceptor(properties));
            MyBatisUtils.addInterceptor(interceptor, inner, 0);
            return inner;
        }
    }

    @Resource
    private SmsCodeService smsCodeService;

    @Resource
    private SmsCodeMapper smsCodeMapper;

    @MockitoBean
    private SmsCodeProperties smsCodeProperties;
    @MockitoBean
    private SmsSendService smsSendService;

    @BeforeEach
    public void setUp() {
        when(smsCodeProperties.getExpireTimes()).thenReturn(Duration.ofMinutes(5));
        when(smsCodeProperties.getSendFrequency()).thenReturn(Duration.ofMinutes(1));
        when(smsCodeProperties.getSendMaximumQuantityPerDay()).thenReturn(10);
        when(smsCodeProperties.getBeginCode()).thenReturn(9999);
        when(smsCodeProperties.getEndCode()).thenReturn(9999);
    }

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    public void testNoTenantContextUsesDefaultTenant() {
        String mobile = "15601691300";
        Integer scene = SmsSceneEnum.MEMBER_LOGIN.getScene();

        TenantContextHolder.clear();
        SmsCodeSendReqDTO sendReqDTO = new SmsCodeSendReqDTO();
        sendReqDTO.setMobile(mobile);
        sendReqDTO.setScene(scene);
        sendReqDTO.setCreateIp("127.0.0.1");
        smsCodeService.sendSmsCode(sendReqDTO);

        SmsCodeDO smsCode = TenantUtils.execute(0L,
                () -> smsCodeMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SmsCodeDO>()
                        .eq(SmsCodeDO::getMobile, mobile)));
        assertNotNull(smsCode);
        assertEquals(0L, smsCode.getTenantId());

        SmsCodeValidateReqDTO validateReqDTO = new SmsCodeValidateReqDTO();
        validateReqDTO.setMobile(mobile);
        validateReqDTO.setCode(smsCode.getCode());
        validateReqDTO.setScene(scene);
        TenantContextHolder.clear();
        smsCodeService.validateSmsCode(validateReqDTO);

        TenantUtils.execute(1L, () -> assertServiceException(
                () -> smsCodeService.validateSmsCode(validateReqDTO), SMS_CODE_NOT_FOUND));
    }

    @Test
    public void testValidateDoesNotSeeOtherTenantCode() {
        String mobile = "15601691300";
        String code = "123456";
        Integer scene = SmsSceneEnum.MEMBER_LOGIN.getScene();

        TenantUtils.execute(1L, () -> {
            SmsCodeDO smsCode = SmsCodeDO.builder()
                    .mobile(mobile).code(code).scene(scene).createIp("127.0.0.1")
                    .todayIndex(1).used(false).build();
            smsCode.setCreateTime(LocalDateTime.now());
            smsCodeMapper.insert(smsCode);
        });

        SmsCodeValidateReqDTO reqDTO = new SmsCodeValidateReqDTO();
        reqDTO.setMobile(mobile);
        reqDTO.setCode(code);
        reqDTO.setScene(scene);

        TenantUtils.execute(2L, () -> assertServiceException(
                () -> smsCodeService.validateSmsCode(reqDTO), SMS_CODE_NOT_FOUND));
    }

}
