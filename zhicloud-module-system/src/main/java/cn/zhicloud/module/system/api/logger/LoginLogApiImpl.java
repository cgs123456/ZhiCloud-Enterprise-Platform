package cn.zhicloud.module.system.api.logger;

import cn.zhicloud.module.system.api.logger.dto.LoginLogCreateReqDTO;
import cn.zhicloud.module.system.service.logger.LoginLogService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;

/**
 * 登录日志的 API 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class LoginLogApiImpl implements LoginLogApi {

    @Resource
    private LoginLogService loginLogService;

    @Override
    public void createLoginLog(LoginLogCreateReqDTO reqDTO) {
        loginLogService.createLoginLog(reqDTO);
    }

}
