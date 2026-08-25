package cn.zhicloud.framework.banner.core;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.util.ClassUtils;

import java.util.concurrent.TimeUnit;

/**
 * 项目启动成功后，提供文档相关的地址
 *
 * @author 智云
 */
@Slf4j
public class BannerApplicationRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        ThreadUtil.execute(() -> {
            ThreadUtil.sleep(1, TimeUnit.SECONDS); // 延迟 1 秒，保证输出到结尾
            log.info("\n----------------------------------------------------------\n\t" +
                            "项目启动成功！\n\t" +
                            "接口文档: \t{} \n\t" +
                            "开发文档: \t{} \n\t" +
                            "视频教程: \t{} \n" +
                            "----------------------------------------------------------",
                    "https://doc.zhicloud.cn/api-doc/",
                    "https://doc.zhicloud.cn",
                    "https://t.zsxq.com/02Yf6M7Qn");

            // 数据报表
            if (isNotPresent("cn.zhicloud.module.report.framework.security.config.SecurityConfiguration")) {
                log.info("[报表模块 zhicloud-module-report - 已禁用][参考 https://doc.zhicloud.cn/report/ 开启]");
            }
            // 工作流
            if (isNotPresent("cn.zhicloud.module.bpm.framework.flowable.config.BpmFlowableConfiguration")) {
                log.info("[工作流模块 zhicloud-module-bpm - 已禁用][参考 https://doc.zhicloud.cn/bpm/ 开启]");
            }
            // 商城系统
            if (isNotPresent("cn.zhicloud.module.trade.framework.web.config.TradeWebConfiguration")) {
                log.info("[商城系统 zhicloud-module-mall - 已禁用][参考 https://doc.zhicloud.cn/mall/build/ 开启]");
            }
            // ERP 系统
            if (isNotPresent("cn.zhicloud.module.erp.framework.web.config.ErpWebConfiguration")) {
                log.info("[ERP 系统 zhicloud-module-erp - 已禁用][参考 https://doc.zhicloud.cn/erp/build/ 开启]");
            }
            // WMS 仓库管理系统
            if (isNotPresent("cn.zhicloud.module.wms.framework.web.config.WmsWebConfiguration")) {
                log.info("[WMS 仓库管理系统 zhicloud-module-wms - 已禁用][参考 https://doc.zhicloud.cn/wms/build/ 开启]");
            }
            // CRM 系统
            if (isNotPresent("cn.zhicloud.module.crm.framework.web.config.CrmWebConfiguration")) {
                log.info("[CRM 系统 zhicloud-module-crm - 已禁用][参考 https://doc.zhicloud.cn/crm/build/ 开启]");
            }
            // MES 系统
            if (isNotPresent("cn.zhicloud.module.mes.framework.web.config.MesWebConfiguration")) {
                log.info("[MES 系统 zhicloud-module-mes - 已禁用][参考 https://doc.zhicloud.cn/mes/build/ 开启]");
            }
            // 微信公众号
            if (isNotPresent("cn.zhicloud.module.mp.framework.mp.config.MpConfiguration")) {
                log.info("[微信公众号 zhicloud-module-mp - 已禁用][参考 https://doc.zhicloud.cn/mp/build/ 开启]");
            }
            // 支付平台
            if (isNotPresent("cn.zhicloud.module.pay.framework.pay.config.PayConfiguration")) {
                log.info("[支付系统 zhicloud-module-pay - 已禁用][参考 https://doc.zhicloud.cn/pay/build/ 开启]");
            }
            // AI 大模型
            if (isNotPresent("cn.zhicloud.module.ai.framework.web.config.AiWebConfiguration")) {
                log.info("[AI 大模型 zhicloud-module-ai - 已禁用][参考 https://doc.zhicloud.cn/ai/build/ 开启]");
            }
            // IoT 物联网
            if (isNotPresent("cn.zhicloud.module.iot.framework.web.config.IotWebConfiguration")) {
                log.info("[IoT 物联网 zhicloud-module-iot - 已禁用][参考 https://doc.zhicloud.cn/iot/build/ 开启]");
            }
            // IM 即时通讯
            if (isNotPresent("cn.zhicloud.module.im.framework.web.config.ImWebConfiguration")) {
                log.info("[IM 即时通讯 zhicloud-module-im - 已禁用][参考 https://doc.zhicloud.cn/im/build/ 开启]");
            }
        });
    }

    private static boolean isNotPresent(String className) {
        return !ClassUtils.isPresent(className, ClassUtils.getDefaultClassLoader());
    }

}
