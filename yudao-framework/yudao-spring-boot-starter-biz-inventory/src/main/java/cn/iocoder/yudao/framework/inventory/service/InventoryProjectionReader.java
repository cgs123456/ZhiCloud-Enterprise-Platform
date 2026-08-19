package cn.iocoder.yudao.framework.inventory.service;

import java.util.List;

/**
 * 库存投影读取器（M2 SPI）
 *
 * <p>由 ERP/MES/WMS 模块在 P1-1 阶段 A 各自实现并注册为 Spring Bean，
 * 使共享 Starter 能在不反向 import 业务模块表的前提下读取其投影，严守 P1-3 模块边界纪律。
 *
 * @author 智云库存治理
 */
public interface InventoryProjectionReader {

    /**
     * @return 本模块当前全部库存投影快照
     */
    List<InventoryProjection> readAll();

}
