package cn.zhicloud.module.system.service.auth;

import cn.zhicloud.module.system.controller.admin.auth.vo.*;
import cn.zhicloud.module.system.dal.dataobject.user.AdminUserDO;

import jakarta.validation.Valid;

/**
 * 管理后台的认证 Service 接口
 *
 * 提供用户的登录、登出的能力
 *
 * @author 智云
 */
public interface AdminAuthService {

    /**
     * 验证账号 + 密码。如果通过，则返回用户
     *
     * @param username 账号
     * @param password 密码
     * @return 用户
     */
    AdminUserDO authenticate(String username, String password);

    /**
     * 账号登录
     *
     * @param reqVO 登录信息
     * @return 登录结果
     */
    AuthLoginRespVO login(@Valid AuthLoginReqVO reqVO);

    /**
     * TOTP 双因素认证登录
     *
     * 在账号密码验证通过、且用户已启用 TOTP 后，使用此方法完成登录
     *
     * @param reqVO 登录信息（含账号、密码、TOTP 验证码）
     * @return 登录结果
     */
    AuthLoginRespVO loginByTotp(@Valid AuthLoginByTotpReqVO reqVO);

    /**
     * 基于 token 退出登录
     *
     * @param token token
     * @param logType 登出类型
     */
    void logout(String token, Integer logType);

    /**
     * 短信验证码发送
     *
     * @param reqVO 发送请求
     */
    void sendSmsCode(AuthSmsSendReqVO reqVO);

    /**
     * 短信登录
     *
     * @param reqVO 登录信息
     * @return 登录结果
     */
    AuthLoginRespVO smsLogin(AuthSmsLoginReqVO reqVO);

    /**
     * 社交快捷登录，使用 code 授权码
     *
     * @param reqVO 登录信息
     * @return 登录结果
     */
    AuthLoginRespVO socialLogin(@Valid AuthSocialLoginReqVO reqVO);

    /**
     * 刷新访问令牌
     *
     * @param refreshToken 刷新令牌
     * @return 登录结果
     */
    AuthLoginRespVO refreshToken(String refreshToken);

    /**
     * 用户注册
     *
     * @param createReqVO 注册用户
     * @return 注册结果
     */
    AuthLoginRespVO register(AuthRegisterReqVO createReqVO);

    /**
     * 重置密码
     *
     * @param reqVO 验证码信息
     */
    void resetPassword(AuthResetPasswordReqVO reqVO);

}
