package cn.zhicloud.module.member.convert.auth;

import cn.zhicloud.module.member.controller.app.auth.vo.*;
import cn.zhicloud.module.member.controller.app.social.vo.AppSocialUserUnbindReqVO;
import cn.zhicloud.module.member.controller.app.user.vo.AppMemberUserResetPasswordReqVO;
import cn.zhicloud.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenRespDTO;
import cn.zhicloud.module.system.api.sms.dto.code.SmsCodeSendReqDTO;
import cn.zhicloud.module.system.api.sms.dto.code.SmsCodeUseReqDTO;
import cn.zhicloud.module.system.api.sms.dto.code.SmsCodeValidateReqDTO;
import cn.zhicloud.module.system.api.social.dto.SocialUserBindReqDTO;
import cn.zhicloud.module.system.api.social.dto.SocialUserUnbindReqDTO;
import cn.zhicloud.module.system.api.social.dto.SocialWxJsapiSignatureRespDTO;
import cn.zhicloud.module.system.enums.sms.SmsSceneEnum;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AuthConvert {

    AuthConvert INSTANCE = Mappers.getMapper(AuthConvert.class);

    SocialUserBindReqDTO convert(Long userId, Integer userType, AppAuthSocialLoginReqVO reqVO);
    SocialUserUnbindReqDTO convert(Long userId, Integer userType, AppSocialUserUnbindReqVO reqVO);

    SmsCodeSendReqDTO convert(AppAuthSmsSendReqVO reqVO);
    SmsCodeUseReqDTO convert(AppMemberUserResetPasswordReqVO reqVO, SmsSceneEnum scene, String usedIp);
    SmsCodeUseReqDTO convert(AppAuthSmsLoginReqVO reqVO, Integer scene, String usedIp);

    AppAuthLoginRespVO convert(OAuth2AccessTokenRespDTO bean, String openid);

    SmsCodeValidateReqDTO convert(AppAuthSmsValidateReqVO bean);

    SocialWxJsapiSignatureRespDTO convert(SocialWxJsapiSignatureRespDTO bean);

}
