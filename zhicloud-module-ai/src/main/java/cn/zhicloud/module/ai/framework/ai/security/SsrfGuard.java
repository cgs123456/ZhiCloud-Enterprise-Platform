package cn.zhicloud.module.ai.framework.ai.security;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.ai.enums.ErrorCodeConstants.AI_SSRF_BLOCKED;
import static cn.zhicloud.module.ai.enums.ErrorCodeConstants.AI_SSRF_RESOLVE_FAIL;
import static cn.zhicloud.module.ai.enums.ErrorCodeConstants.AI_SSRF_URL_INVALID;

/**
 * SSRF（Server-Side Request Forgery）防护器
 *
 * <p>背景：AI 工具调用中可能涉及 URL 读取（如 Web Search、MCP 工具中的 HTTP 请求）。
 * 若不校验 URL，恶意用户可能通过 AI 工具访问内网地址（如云元数据 169.254.169.254、
 * 本地服务 127.0.0.1、内网网段 10.x / 172.16.x / 192.168.x），造成 SSRF 攻击。</p>
 *
 * <p>本工具提供以下防护：</p>
 * <ol>
 *   <li>协议白名单：仅允许 http / https</li>
 *   <li>内网 IP 黑名单：禁止访问 10.0.0.0/8、172.16.0.0/12、192.168.0.0/16、
 *       127.0.0.0/8、169.254.0.0/16、0.0.0.0/8 等 CIDR 段</li>
 *   <li>DNS 重绑定防护：解析域名后逐一校验所有解析到的 IP</li>
 *   <li>IPv6 基础防护：拦截回环、链路本地、任意本地等危险地址</li>
 * </ol>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Resource
 * private SsrfGuard ssrfGuard;
 *
 * public String fetch(String url) {
 *     ssrfGuard.validateUrl(url); // 校验失败抛出 ServiceException
 *     return httpClient.get(url);
 * }
 * }</pre>
 *
 * <p>说明：本工具仅校验 URL 与解析到的 IP，无法防止「校验后再次 DNS 解析时返回不同 IP」
 * 的极端 DNS 重绑定场景。彻底防护需在 HTTP 客户端层固定 IP，本工具仅作为基础防线。</p>
 *
 * @author 智云
 */
@Component
@Slf4j
public class SsrfGuard {

    /** 禁止访问的 IPv4 CIDR 段 */
    private static final String[] BLOCKED_CIDR = {
            "10.0.0.0/8",      // A 类私网
            "172.16.0.0/12",   // B 类私网
            "192.168.0.0/16",  // C 类私网
            "127.0.0.0/8",     // IPv4 回环
            "169.254.0.0/16",  // 链路本地（含云元数据 169.254.169.254）
            "0.0.0.0/8"        // 不可路由段
    };

    /** 预解析的 CIDR 段，避免每次校验时重复解析 */
    private final CidrRange[] blockedRanges;

    public SsrfGuard() {
        this.blockedRanges = new CidrRange[BLOCKED_CIDR.length];
        for (int i = 0; i < BLOCKED_CIDR.length; i++) {
            this.blockedRanges[i] = CidrRange.parse(BLOCKED_CIDR[i]);
        }
    }

    /**
     * 校验 URL 是否安全可访问
     *
     * <p>校验流程：</p>
     * <ol>
     *   <li>解析 URL，校验协议为 http / https</li>
     *   <li>提取 host（域名或 IP 字面量）</li>
     *   <li>DNS 解析 host，获取所有解析到的 IP</li>
     *   <li>逐一校验 IP 不在禁止的 CIDR 段内</li>
     * </ol>
     *
     * @param urlString 待校验的 URL 字符串
     * @return true 表示通过校验
     * @throws ServiceException URL 格式错误、协议非法、DNS 解析失败或命中黑名单时抛出
     */
    public boolean validateUrl(String urlString) {
        if (StrUtil.isBlank(urlString)) {
            throw exception(AI_SSRF_URL_INVALID, urlString);
        }

        // 1. 解析 URL
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            log.warn("[validateUrl] URL 格式非法：{}", urlString);
            throw exception(AI_SSRF_URL_INVALID, urlString);
        }

        // 2. 校验协议
        String protocol = url.getProtocol();
        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            log.warn("[validateUrl] 协议非法：{}，仅允许 http/https", protocol);
            throw exception(AI_SSRF_URL_INVALID, urlString);
        }

        // 3. 提取 host
        String host = url.getHost();
        if (StrUtil.isBlank(host)) {
            log.warn("[validateUrl] URL 缺少 host：{}", urlString);
            throw exception(AI_SSRF_URL_INVALID, urlString);
        }

        // 4. DNS 解析并校验所有 IP
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            log.warn("[validateUrl] DNS 解析失败：{}", host);
            throw exception(AI_SSRF_RESOLVE_FAIL, host);
        }

        // 5. 逐个校验 IP
        for (InetAddress address : addresses) {
            if (isInternalIp(address)) {
                log.warn("[validateUrl] 命中内网/黑名单 IP：{}（来自 host={}）", address.getHostAddress(), host);
                throw exception(AI_SSRF_BLOCKED, address.getHostAddress());
            }
        }
        return true;
    }

    /**
     * 判断 {@link InetAddress} 是否为内网或危险地址
     *
     * <p>对 IPv4 走 CIDR 黑名单匹配；对 IPv6 通过 JDK 内置方法判断回环、链路本地、任意本地等。</p>
     *
     * @param ip 待判断的 IP 地址
     * @return true 表示是内网或危险地址
     */
    public boolean isInternalIp(InetAddress ip) {
        if (ip == null) {
            return true;
        }
        // IPv4：走 CIDR 黑名单
        if (ip instanceof Inet4Address) {
            return isBlockedCidr(ip.getHostAddress());
        }
        // IPv6：使用 JDK 内置方法判断
        return ip.isLoopbackAddress()
                || ip.isLinkLocalAddress()
                || ip.isAnyLocalAddress()
                || ip.isSiteLocalAddress();
    }

    /**
     * 判断 IPv4 字符串是否命中禁止的 CIDR 段
     *
     * @param ip IPv4 字符串，如 {@code "169.254.169.254"}
     * @return true 表示命中黑名单
     */
    public boolean isBlockedCidr(String ip) {
        if (StrUtil.isBlank(ip)) {
            return true;
        }
        long ipLong = ipToInt(ip);
        for (CidrRange range : blockedRanges) {
            if (range.contains(ipLong)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将 IPv4 字符串转换为无符号 32 位 int（用 long 承载，避免符号问题）
     *
     * @param ip IPv4 字符串，如 {@code "192.168.1.1"}
     * @return IP 对应的 long 值
     */
    private static long ipToInt(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("非法的 IPv4 地址：" + ip);
        }
        long result = 0L;
        for (String part : parts) {
            int octet = Integer.parseInt(part);
            if (octet < 0 || octet > 255) {
                throw new IllegalArgumentException("非法的 IPv4 地址：" + ip);
            }
            result = (result << 8) | octet;
        }
        return result & 0xFFFFFFFFL;
    }

    /**
     * IPv4 CIDR 段表示，使用预计算的 network 与 mask 加速匹配
     */
    private static final class CidrRange {

        /** 网络地址（无符号，存于 long 低 32 位） */
        private final long network;
        /** 子网掩码（无符号，存于 long 低 32 位） */
        private final long mask;

        private CidrRange(long network, long mask) {
            this.network = network & 0xFFFFFFFFL;
            this.mask = mask & 0xFFFFFFFFL;
        }

        /**
         * 解析 CIDR 字符串，如 {@code "10.0.0.0/8"}
         */
        public static CidrRange parse(String cidr) {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                throw new IllegalArgumentException("非法的 CIDR：" + cidr);
            }
            long network = ipToInt(parts[0]);
            int prefix = Integer.parseInt(parts[1]);
            if (prefix < 0 || prefix > 32) {
                throw new IllegalArgumentException("非法的 CIDR 前缀长度：" + prefix);
            }
            // prefix=32 → 0xFFFFFFFF；prefix=0 → 0x00000000
            long mask = prefix == 0 ? 0L : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
            return new CidrRange(network, mask);
        }

        /**
         * 判断 IP 是否在本 CIDR 段内
         */
        public boolean contains(long ipLong) {
            return (ipLong & mask) == (network & mask);
        }
    }

}
