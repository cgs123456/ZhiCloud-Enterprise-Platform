package cn.zhicloud.module.iot.framework.tdengine.config;

import cn.zhicloud.module.iot.service.device.message.IotDeviceMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * TDengine 表初始化的 Configuration
 *
 * @author alwayssuper
 */
@Component
@RequiredArgsConstructor
@Slf4j
// 未配置 TDengine 数据源（spring.datasource.dynamic.datasource.tdengine.url）时不装载本 Runner：
// 否则设备消息超级表初始化必然失败，而失败路径是 System.exit(1)，会让整个服务端起不来。
// 此时产品/设备/物模型/告警等 MySQL 侧功能仍可用，只有依赖 TDengine 的消息与属性接口不可用。
@ConditionalOnProperty(prefix = "spring.datasource.dynamic.datasource.tdengine", name = "url")
public class TDengineTableInitRunner implements ApplicationRunner {

    private final IotDeviceMessageService deviceMessageService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            // 初始化设备消息表
            deviceMessageService.defineDeviceMessageStable();
        } catch (Exception ex) {
            // 初始化失败时打印错误消息并退出系统
            log.error("[run][TDengine初始化设备消息表结构失败，系统无法正常运行，即将退出]", ex);
            System.exit(1);
        }
    }

}
