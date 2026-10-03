package cn.zhicloud.framework.tenant.core.security;

import cn.zhicloud.framework.security.core.LoginUser;
import cn.zhicloud.framework.tenant.config.TenantProperties;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.framework.tenant.core.service.TenantFrameworkService;
import cn.zhicloud.framework.tenant.core.web.TenantContextWebFilter;
import cn.zhicloud.framework.web.config.WebProperties;
import cn.zhicloud.framework.web.core.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 验证请求头中的租户编号不能覆盖已认证用户的真实租户编号。
 */
class TenantContextSecurityIsolationTest {

    private TenantFrameworkService tenantFrameworkService;
    private TenantSecurityWebFilter tenantSecurityWebFilter;

    @BeforeEach
    void setUp() {
        tenantFrameworkService = mock(TenantFrameworkService.class);
        tenantSecurityWebFilter = new TenantSecurityWebFilter(
                new WebProperties(),
                new TenantProperties(),
                Set.of(),
                new GlobalExceptionHandler("test", null),
                tenantFrameworkService);
        LoginUser loginUser = new LoginUser()
                .setId(100L)
                .setUserType(2)
                .setTenantId(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void forgedPlatformTenantHeaderIsRejectedBeforeDatabaseAccess() throws Exception {
        MockHttpServletRequest request = buildRequest("0");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean downstreamInvoked = new AtomicBoolean(false);

        new TenantContextWebFilter().doFilter(request, response,
                (filteredRequest, filteredResponse) -> tenantSecurityWebFilter.doFilter(
                        filteredRequest, filteredResponse,
                        (ignoredRequest, ignoredResponse) -> downstreamInvoked.set(true)));

        assertFalse(downstreamInvoked.get());
        assertTrue(response.getContentAsString().contains("您无权访问该租户的数据"));
        verify(tenantFrameworkService, never()).validTenant(0L);
    }

    @Test
    void authenticatedTenantHeaderIsAccepted() throws Exception {
        MockHttpServletRequest request = buildRequest("1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new TenantContextWebFilter().doFilter(request, response,
                (filteredRequest, filteredResponse) -> tenantSecurityWebFilter.doFilter(
                        filteredRequest, filteredResponse, chain));

        assertNotNull(chain.getRequest());
        verify(tenantFrameworkService).validTenant(1L);
    }

    private static MockHttpServletRequest buildRequest(String tenantId) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin-api/erp/accounting-event");
        request.setContextPath("");
        request.addHeader("tenant-id", tenantId);
        return request;
    }

}