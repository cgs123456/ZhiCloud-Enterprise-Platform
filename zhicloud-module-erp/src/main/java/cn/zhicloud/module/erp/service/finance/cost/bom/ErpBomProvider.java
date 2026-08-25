package cn.zhicloud.module.erp.service.finance.cost.bom;

import java.util.List;

/**
 * ERP BOM 数据提供者接口（SPI 网关）
 *
 * <p>由于当前 ERP 模块未内置独立 BOM 模块，本接口作为 SPI 网关，
 * 由外部模块（如 MES 的 {@code MesMdProductBomService}）实现并注入。
 *
 * <p>参考模式：{@code MultiAgentExecutorGateway} 跨模块 SPI 网关。
 * 当后续接入 MES BOM 或独立 ERP BOM 模块时，只需提供本接口的实现 Bean，
 * 成本卷积算法即可自动启用 BOM 递归卷积。
 *
 * @author 智云
 */
public interface ErpBomProvider {

    /**
     * 查询某产品的 BOM 子件列表
     *
     * @param productId 父件产品编号
     * @return 子件列表（包含子件产品编号 + 用量）；无 BOM 时返回空列表
     */
    List<ErpBomComponent> getBomComponents(Long productId);

}
