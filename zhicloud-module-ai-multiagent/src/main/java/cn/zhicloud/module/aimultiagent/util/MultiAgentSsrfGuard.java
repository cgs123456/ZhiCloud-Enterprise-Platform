package cn.zhicloud.module.aimultiagent.util;

import cn.hutool.core.util.StrUtil;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Set;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.aimultiagent.enums.ErrorCodeConstants.SKILL_URL_BLOCKED;

/**
 * 多 Agent 技能 URL 安全校验（SSRF 防护）
 *
 * <p>背景：技能 config_json 可配置 endpoint_url，服务端可能据此发起出站请求。
 * 若不校验，恶意管理员/被污染的配置可指向内网服务（如 127.0.0.1、10.x.x.x、
 * 云元数据 169.254.169.254），造成 SSRF 攻击。
 *
 * <p>防护策略（与 ai 模块 SsrfGuard / ai-rag UrlSafetyUtils 同级）：
 * <ol>
 *   <li>协议白名单：仅允许 http / https</li>
 *   <li>显式拒绝云元数据地址：169.254.169.254 与 metadata.google.internal</li>
 *   <li>DNS 解析所有地址，任一命中回环 / 私网 / 链路本地 / 站点本地 / 通配地址即拒绝</li>
 *   <li>DNS 解析失败直接拒绝（fail-fast）</li>
 * </ol>
 *
 * @author zhicloud
 */
public class MultiAgentSsrfGuard {

    /**
     * 允许的协议
     */
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    /**
     * 显式拒绝的主机名 / 地址（云元数据服务）
     */
    private static final Set<String> FORBIDDEN_HOSTS = Set.of("169.254.169.254", "metadata.google.internal");

    private MultiAgentSsrfGuard() {
    }

    /**
     * 校验 URL 是否安全，不合法时抛出 ServiceException
     *
     * @param urlString 待校验的 URL 字符串
     */
    public static void validateSafeUrl(String urlString) {
        if (StrUtil.isBlank(urlString)) {
            throw exception(SKILL_URL_BLOCKED, urlString);
        }
        // 1. 解析 URL 并校验协议白名单
        URL url;
        try {
            url = new URL(urlString.trim());
        } catch (MalformedURLException e) {
            throw exception(SKILL_URL_BLOCKED, urlString);
        }
        String scheme = url.getProtocol();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw exception(SKILL_URL_BLOCKED, urlString);
        }
        // 2. 校验 host 存在，并显式拒绝云元数据地址（不依赖 DNS 解析结果）
        String host = url.getHost();
        if (StrUtil.isBlank(host) || FORBIDDEN_HOSTS.contains(host.toLowerCase())) {
            throw exception(SKILL_URL_BLOCKED, urlString);
        }
        // 3. DNS 解析所有地址并逐一校验，解析失败直接拒绝
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw exception(SKILL_URL_BLOCKED, urlString);
        }
        for (InetAddress address : addresses) {
            if (address.isLoopbackAddress() || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress() || address.isAnyLocalAddress()) {
                throw exception(SKILL_URL_BLOCKED, urlString);
            }
        }
    }

}
