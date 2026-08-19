package cn.iocoder.yudao.framework.inventory.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 共享库存配置（P1-4 / P1-1）
 *
 * @author 智云库存治理
 */
@ConfigurationProperties(prefix = "yudao.inventory")
@Data
public class InventoryProperties {

    /**
     * 是否启用共享库存 Starter，默认启用
     */
    private Boolean enabled = true;

    /**
     * P1-1 单一真值源开关（M2 阶段 A）。
     * <p>开启后库存写经共享 Starter 落到 WMS 真值，ERP/MES 库存退化为只读投影；关闭则维持三真值源现状。
     * 通过 Feature Flag 隔离，可一键回退。
     */
    private Boolean enableSingleSource = false;

}
