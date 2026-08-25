package cn.zhicloud.module.wms.service.order.pickstrategy;

import cn.zhicloud.module.wms.service.order.pickstrategy.dto.WmsPickTaskDTO;

import java.util.List;

/**
 * WMS 拣选策略引擎 Service 接口
 *
 * <p>核心作业逻辑：根据出库单生成拣货任务，并基于 ABC 分类 + 就近排序优化拣货路径。
 *
 * @author 智云
 */
public interface WmsPickStrategyService {

    /**
     * 生成拣货任务
     *
     * <p>根据出库单类型、SKU 数量、库位分布选择拣选模式：
     * <ul>
     *   <li>按单拣（wavePick=false）：SKU 少、库位集中</li>
     *   <li>批量拣（wavePick=true）：SKU 多、库位分散</li>
     *   <li>二次分拣（sortWhilePick=false）：库位极度分散时边拣边分不可行</li>
     * </ul>
     *
     * @param shipmentOrderId 出库单编号
     * @return 拣货任务编号列表
     */
    List<Long> generatePickTasks(Long shipmentOrderId);

    /**
     * 优化拣货路径
     *
     * <p>基于库位 ABC 分类 + 最短路径算法（就近排序）：将任务按库位伪坐标进行最近邻排序，
     * 设置 pickSequence 字段。
     *
     * @param tasks 拣货任务列表
     * @return 排序后的拣货任务列表
     */
    List<WmsPickTaskDTO> optimizePickPath(List<WmsPickTaskDTO> tasks);

}
