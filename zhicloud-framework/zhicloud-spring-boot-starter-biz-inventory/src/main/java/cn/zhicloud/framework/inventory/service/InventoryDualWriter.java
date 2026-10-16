package cn.zhicloud.framework.inventory.service;

import java.math.BigDecimal;

/**
 * 库存双写写入器 SPI（M2 阶段 B）
 *
 * <p>由各业务模块（ERP / MES）实现并注册为 Spring Bean，
 * 由业务侧 Service 通过 {@code @Autowired(required = false) List<InventoryDualWriter>}
 * 注入后**在同事务内直接调用**实现双写
 * （见 {@code ErpStockServiceImpl#triggerDualWrite} 与
 * {@code MesWmTransactionServiceImpl} 的同名方法）。</p>
 *
 * <p><b>关于曾经写在这里的一句错话</b>：早先注释声称由
 * {@code cn.zhicloud.framework.inventory.config.InventoryDualWriteRegistrar}
 * 「在事务提交前统一触发」。<b>该类在仓库中并不存在</b>（全库 grep 零命中），
 * 实际机制就是普通的 Bean 注入 + 业务代码调用——没有任何注册器，
 * 也没有 {@code TransactionSynchronization} 之类的事务提交钩子。</p>
 *
 * <p>这个区别不是文字游戏，它有实际后果：</p>
 * <ul>
 *   <li><b>调用时机由业务代码写在哪个方法里决定</b>。若某个业务 Service 漏了
 *       {@code triggerDualWrite}，双写就静默不发生，没有框架层兜底。</li>
 *   <li><b>「同 TX」也只是「在同事务的连接上执行 SQL」</b>。双写失败按各调用点
 *       自己的异常处理走，不存在自动补偿、重试或 DLQ。</li>
 *   <li>默认 {@code InventoryProperties.enableDualWrite=false}，且全仓库无任何
 *       yaml 打开它。打开前先确认上面两点可接受，并<b>实测</b>一次
 *       「双写是否真的同事务生效」——2026-10 的普查只做了代码结构推断，
 *       没有跑 {@code enableDualWrite=true} 的实测，不得据此当已验证。</li>
 * </ul>
 *
 * <p>实现类不反向 import 任何共享 Starter 内部类，仅实现本接口并由业务侧调用。</p>
 *
 * @author 智云库存治理
 */
public interface InventoryDualWriter {

    /**
     * 执行一次库存增量双写。
     *
     * @param itemId      物品编号（规范键）
     * @param warehouseId 仓库编号（规范键）
     * @param locationId  库位编号（规范键，ERP 传入 null 由框架按 0L 处理）
     * @param areaId      库区编号（规范键，ERP 传入 null 由框架按 0L 处理）
     * @param batchId     批次编号（规范键，ERP 传入 null 由框架按 0L 处理）
     * @param batchCode   批次编码（冗余字段，MES 保留，ERP 传 null）
     * @param quantityDelta   数量增量（正=入库，负=出库）
     * @param lockedDelta     锁定数量增量（正=预占，负=释放；MES 恒为 0）
     */
    void dualWrite(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId,
                   String batchCode, BigDecimal quantityDelta, BigDecimal lockedDelta);

}
