package cn.zhicloud.module.system.service.oauth2;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.system.controller.admin.oauth2.vo.client.OAuth2ClientPageReqVO;
import cn.zhicloud.module.system.controller.admin.oauth2.vo.client.OAuth2ClientSaveReqVO;
import cn.zhicloud.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

/**
 * OAuth2.0 Client Service 接口
 *
 * 从功能上，和 JdbcClientDetailsService 的功能，提供客户端的操作
 *
 * @author 智云
 */
public interface OAuth2ClientService {

    /**
     * 创建 OAuth2 客户端
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createOAuth2Client(@Valid OAuth2ClientSaveReqVO createReqVO);

    /**
     * 更新 OAuth2 客户端
     *
     * @param updateReqVO 更新信息
     */
    void updateOAuth2Client(@Valid OAuth2ClientSaveReqVO updateReqVO);

    /**
     * 删除 OAuth2 客户端
     *
     * @param id 编号
     */
    void deleteOAuth2Client(Long id);

    /**
     * 批量删除 OAuth2 客户端
     *
     * @param ids 编号数组
     */
    void deleteOAuth2ClientList(List<Long> ids);

    /**
     * 获得 OAuth2 客户端
     *
     * @param id 编号
     * @return OAuth2 客户端
     */
    OAuth2ClientDO getOAuth2Client(Long id);

    /**
     * 获得 OAuth2 客户端，从缓存中
     *
     * @param clientId 客户端编号
     * @return OAuth2 客户端
     */
    OAuth2ClientDO getOAuth2ClientFromCache(String clientId);

    /**
     * 获得 OAuth2 客户端分页
     *
     * @param pageReqVO 分页查询
     * @return OAuth2 客户端分页
     */
    PageResult<OAuth2ClientDO> getOAuth2ClientPage(OAuth2ClientPageReqVO pageReqVO);

    /**
     * 从缓存中，校验客户端是否合法（不校验客户端密钥）
     *
     * 适用于「服务端内部信任调用」（登录、刷新令牌）与「用户代理（浏览器）授权流程」：
     * 前者由登录凭据 / 刷新令牌本身背书，后者由已登录用户会话背书，均不持有 client_secret。
     *
     * @param clientId 客户端编号
     * @return 客户端
     */
    default OAuth2ClientDO validOAuthClientFromCache(String clientId) {
        return validOAuthClientFromCache(clientId, null, null, null);
    }

    /**
     * 从缓存中，校验客户端是否合法（不校验客户端密钥，但校验授权方式 / 范围 / 回调地址）
     *
     * @param clientId 客户端编号
     * @param authorizedGrantType 授权方式
     * @param scopes 授权范围
     * @param redirectUri 重定向地址
     * @return 客户端
     */
    OAuth2ClientDO validOAuthClientFromCache(String clientId, String authorizedGrantType,
                                             Collection<String> scopes, String redirectUri);

    /**
     * 从缓存中，校验客户端是否合法，并校验客户端密钥
     *
     * 语义：只要客户端在库中配置了非空 secret，调用方就必须提供完全一致的 secret；
     * 调用方传空不再能跳过校验。secret 为空的客户端视为公开客户端（public client），允许不传密钥。
     * 该重载用于 /oauth2/token、/oauth2/check-token 等需要「客户端认证」的端点。
     *
     * @param clientId 客户端编号
     * @param clientSecret 客户端密钥
     * @param authorizedGrantType 授权方式
     * @param scopes 授权范围
     * @param redirectUri 重定向地址
     * @return 客户端
     */
    OAuth2ClientDO validOAuthClientFromCache(String clientId, String clientSecret, String authorizedGrantType,
                                             Collection<String> scopes, String redirectUri);

}
