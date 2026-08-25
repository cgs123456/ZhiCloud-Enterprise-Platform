package cn.zhicloud.module.erp.api;

import cn.zhicloud.module.erp.dal.dataobject.production.mps.ErpMpsPlanDO;

/**
 * ERP MPS 下发 MRP 执行器 SPI 网关（跨模块契约，位于 erp.api 边界包）
 *
 * <p>P1-3 模块边界治理：该接口从 service.production.mps.gateway 迁至 erp.api 包，
 * 作为 zhicloud-module-erp 对外发布的跨模块契约边界，供 zhicloud-module-mes 实现，
 * 与 qms.api.InspectionOrderApi 等既有 api 包范式对齐（跨模块调用须经 api 包，禁止直接 import service 层）。
 *
 * <p>用于解耦 zhicloud-module-erp 与 zhicloud-module-mes 模块。
 * erp 模块依赖此接口，mes 模块提供实现（参考 MultiAgentExecutorGateway 模式）。
 *
 * <p>背景：MES MRP Service 位于 zhicloud-module-mes，erp 模块不能直接依赖 mes（避免循环依赖 / 可选模块）。
 * 通过此 SPI 接口实现依赖反转：erp 模块在 MPS 下发时通过此网关触发 MRP 运算，
 * 当未启用 mes 模块时，erp 侧 {@code @Autowired(required = false)} 注入为空，降级为日志。
 *
 * @author ERP 修复
 */
public interface ErpMrpExecutorGateway {

    /**
     * 基于 MPS 计划执行 MRP 物料需求运算
     *
     * <p>实现方应：创建 MRP 计划并执行 MRP 展算（BOM 展开计算净需求）。
     *
     * @param plan MPS 主生产计划
     */
    void executeMrp(ErpMpsPlanDO plan);

}
