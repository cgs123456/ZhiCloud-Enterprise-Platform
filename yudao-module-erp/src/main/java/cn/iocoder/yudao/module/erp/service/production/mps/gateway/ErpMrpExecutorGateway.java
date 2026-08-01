package cn.iocoder.yudao.module.erp.service.production.mps.gateway;

import cn.iocoder.yudao.module.erp.dal.dataobject.production.mps.ErpMpsPlanDO;

/**
 * ERP MPS 下发 MRP 执行器 SPI 网关
 *
 * <p>用于解耦 yudao-module-erp 与 yudao-module-mes 模块。
 * erp 模块依赖此接口，mes 模块提供实现（参考 MultiAgentExecutorGateway 模式）。
 *
 * <p>背景：MES MRP Service 位于 yudao-module-mes，erp 模块不能直接依赖 mes（避免循环依赖 / 可选模块）。
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
