package cn.zhicloud.module.aimultiagent.util;

import cn.zhicloud.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link MultiAgentSsrfGuard} 单元测试：覆盖协议白名单 / 内网段拦截 /
 * 云元数据拦截 / 非法 URL / DNS 失败 / 公网放行。
 *
 * <p>说明：断言全部使用 IP 字面量与非法输入，不依赖外网 DNS，保证离线可跑。
 *
 * @author zhicloud
 */
class MultiAgentSsrfGuardTest {

    @Test
    void blankUrl_blocked() {
        assertThrows(ServiceException.class, () -> MultiAgentSsrfGuard.validateSafeUrl(null));
        assertThrows(ServiceException.class, () -> MultiAgentSsrfGuard.validateSafeUrl("   "));
    }

    @Test
    void nonHttpScheme_blocked() {
        assertThrows(ServiceException.class,
                () -> MultiAgentSsrfGuard.validateSafeUrl("ftp://8.8.8.8/file.txt"));
        assertThrows(ServiceException.class,
                () -> MultiAgentSsrfGuard.validateSafeUrl("file:///etc/passwd"));
    }

    @Test
    void malformedUrl_blocked() {
        assertThrows(ServiceException.class,
                () -> MultiAgentSsrfGuard.validateSafeUrl("http://"));
        assertThrows(ServiceException.class,
                () -> MultiAgentSsrfGuard.validateSafeUrl("not-a-url"));
    }

    @Test
    void loopback_blocked() {
        assertThrows(ServiceException.class,
                () -> MultiAgentSsrfGuard.validateSafeUrl("http://127.0.0.1/admin"));
        assertThrows(ServiceException.class,
                () -> MultiAgentSsrfGuard.validateSafeUrl("http://localhost:8080/api"));
    }

    @Test
    void privateRanges_blocked() {
        assertThrows(ServiceException.class,
                () -> MultiAgentSsrfGuard.validateSafeUrl("http://10.1.2.3/"));
        assertThrows(ServiceException.class,
                () -> MultiAgentSsrfGuard.validateSafeUrl("https://172.16.5.6:8443/x"));
        assertThrows(ServiceException.class,
                () -> MultiAgentSsrfGuard.validateSafeUrl("http://192.168.1.100/"));
        assertThrows(ServiceException.class,
                () -> MultiAgentSsrfGuard.validateSafeUrl("http://0.0.0.0/"));
    }

    @Test
    void cloudMetadata_blocked() {
        assertThrows(ServiceException.class,
                () -> MultiAgentSsrfGuard.validateSafeUrl("http://169.254.169.254/latest/meta-data/"));
        assertThrows(ServiceException.class,
                () -> MultiAgentSsrfGuard.validateSafeUrl("http://metadata.google.internal/"));
    }

    @Test
    void linkLocal_blocked() {
        assertThrows(ServiceException.class,
                () -> MultiAgentSsrfGuard.validateSafeUrl("http://169.254.10.20/"));
    }

    // 注：不可解析域名（UnknownHostException → 拒绝）分支不做单测——沙箱 DNS 多为通配解析，
    // 该分支行为依赖环境，在 MultiAgentSkillServiceImpl.validateConfigJson 的集成联调中覆盖。

    @Test
    void publicIpLiteral_allowed() {
        // 公网 IP 字面量无需 DNS，直接放行（离线可跑）
        assertDoesNotThrow(() -> MultiAgentSsrfGuard.validateSafeUrl("http://8.8.8.8/"));
        assertDoesNotThrow(() -> MultiAgentSsrfGuard.validateSafeUrl("https://1.1.1.1:443/dns-query"));
    }

    @Test
    void blockedErrorCode_isSkillSegment() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> MultiAgentSsrfGuard.validateSafeUrl("http://127.0.0.1/"));
        // 错误码须落在技能目录段 1-042-004-xxx
        assertEquals(1_042_004_003, ex.getCode());
    }

}
