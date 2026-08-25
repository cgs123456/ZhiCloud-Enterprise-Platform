package cn.zhicloud.module.mes.service.pro.workorder;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.mes.controller.admin.pro.workorder.vo.kitting.MesProWorkOrderKittingLineRespVO;
import cn.zhicloud.module.mes.controller.admin.pro.workorder.vo.kitting.MesProWorkOrderKittingSummaryRespVO;
import cn.zhicloud.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderBomDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.itemreceipt.MesWmItemReceiptDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.itemreceipt.MesWmItemReceiptLineDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.materialstock.MesWmMaterialStockDO;
import cn.zhicloud.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.zhicloud.module.mes.dal.mysql.pro.workorder.MesProWorkOrderBomMapper;
import cn.zhicloud.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.zhicloud.module.mes.dal.mysql.wm.itemreceipt.MesWmItemReceiptLineMapper;
import cn.zhicloud.module.mes.dal.mysql.wm.itemreceipt.MesWmItemReceiptMapper;
import cn.zhicloud.module.mes.dal.mysql.wm.materialstock.MesWmMaterialStockMapper;
import cn.zhicloud.module.mes.enums.MesOrderStatusConstants;
import cn.zhicloud.module.mes.enums.pro.MesProWorkOrderKittingStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_KITTING_ANALYSIS_WORK_ORDER_BOM_EMPTY;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_NOT_EXISTS;

/**
 * MES 工单齐套分析 Service 实现类（P0-5）
 *
 * <p>基于工单 BOM、当前库存、在途采购入库单计算齐套情况。
 *
 * <h3>数据来源</h3>
 * <ul>
 *   <li>物料需求：{@link MesProWorkOrderBomDO} × {@link MesProWorkOrderDO#getQuantity()}</li>
 *   <li>当前库存：{@link MesWmMaterialStockDO}（frozen=false 的所有记录，按 itemId 求和）</li>
 *   <li>在途数量：{@link MesWmItemReceiptDO}（status=待入库）+ {@link MesWmItemReceiptLineDO}（按 itemId 求和）</li>
 * </ul>
 *
 * @author zhicloud
 */
@Service
@Slf4j
public class MesProWorkOrderKittingAnalysisServiceImpl implements MesProWorkOrderKittingAnalysisService {

    @Resource
    private MesProWorkOrderMapper workOrderMapper;
    @Resource
    private MesProWorkOrderBomMapper workOrderBomMapper;
    @Resource
    private MesWmMaterialStockMapper materialStockMapper;
    @Resource
    private MesWmItemReceiptMapper itemReceiptMapper;
    @Resource
    private MesWmItemReceiptLineMapper itemReceiptLineMapper;
    @Resource
    private MesMdItemMapper itemMapper;

    @Override
    public MesProWorkOrderKittingSummaryRespVO analyzeKitting(Long workOrderId) {
        // 1. 校验工单存在
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw exception(PRO_WORK_ORDER_NOT_EXISTS);
        }

        // 2. 获取工单 BOM 列表
        List<MesProWorkOrderBomDO> bomList = workOrderBomMapper.selectListByWorkOrderId(workOrderId);
        if (CollUtil.isEmpty(bomList)) {
            throw exception(PRO_KITTING_ANALYSIS_WORK_ORDER_BOM_EMPTY);
        }

        // 3. 批量查询物料信息（用于展示 itemCode/itemName/unitOfMeasure）
        Set<Long> itemIds = bomList.stream().map(MesProWorkOrderBomDO::getItemId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, MesMdItemDO> itemMap = queryItemMap(itemIds);
        // 把产品 ID 也加入查询
        MesMdItemDO productItem = itemMapper.selectById(workOrder.getProductId());

        // 4. 批量查询库存：按 itemId 分组求和（仅未冻结）
        Map<Long, BigDecimal> stockMap = queryStockQuantityByItemIds(itemIds);

        // 5. 批量查询在途：从 MesWmItemReceipt（status=APPROVED 待入库）+ 行表，按 itemId 求和
        Map<Long, BigDecimal> inTransitMap = queryInTransitQuantityByItemIds(itemIds);

        // 6. 计算每条 BOM 行的齐套状态
        BigDecimal workOrderQty = workOrder.getQuantity() == null ? BigDecimal.ZERO : workOrder.getQuantity();
        List<MesProWorkOrderKittingLineRespVO> lines = new ArrayList<>(bomList.size());
        int fullyKittedCount = 0;
        int partialCount = 0;
        int shortageCount = 0;
        for (MesProWorkOrderBomDO bom : bomList) {
            MesProWorkOrderKittingLineRespVO line = buildKittingLine(bom, workOrder, workOrderQty,
                    itemMap, stockMap, inTransitMap);
            lines.add(line);
            // 统计
            if (MesProWorkOrderKittingStatusEnum.FULLY_KITTED.getStatus().equals(line.getKittingStatus())) {
                fullyKittedCount++;
            } else if (MesProWorkOrderKittingStatusEnum.PARTIAL.getStatus().equals(line.getKittingStatus())) {
                partialCount++;
            } else {
                shortageCount++;
            }
        }

        // 7. 构造汇总 VO
        return buildSummary(workOrder, productItem, lines,
                fullyKittedCount, partialCount, shortageCount);
    }

    /**
     * 批量查询物料信息，构造 itemId → MesMdItemDO 映射
     */
    private Map<Long, MesMdItemDO> queryItemMap(Set<Long> itemIds) {
        if (CollUtil.isEmpty(itemIds)) {
            return Collections.emptyMap();
        }
        List<MesMdItemDO> items = itemMapper.selectByIds(itemIds);
        return items.stream().collect(Collectors.toMap(MesMdItemDO::getId, i -> i, (a, b) -> a));
    }

    /**
     * 批量查询库存：按 itemId 分组求和（仅未冻结记录）
     *
     * <p>注：不过滤 quantity=0 的记录，因为 SUM 时 0 不影响结果。
     */
    private Map<Long, BigDecimal> queryStockQuantityByItemIds(Set<Long> itemIds) {
        if (CollUtil.isEmpty(itemIds)) {
            return Collections.emptyMap();
        }
        List<MesWmMaterialStockDO> stocks = materialStockMapper.selectList(
                new LambdaQueryWrapperX<MesWmMaterialStockDO>()
                        .in(MesWmMaterialStockDO::getItemId, itemIds)
                        .eq(MesWmMaterialStockDO::getFrozen, false));
        Map<Long, BigDecimal> result = new HashMap<>();
        for (MesWmMaterialStockDO stock : stocks) {
            BigDecimal qty = stock.getQuantity() == null ? BigDecimal.ZERO : stock.getQuantity();
            result.merge(stock.getItemId(), qty, BigDecimal::add);
        }
        return result;
    }

    /**
     * 批量查询在途：从采购入库单（status=待入库 APPROVED）+ 行表，按 itemId 求和 receivedQuantity
     *
     * <p>"在途"定义：已到货但尚未正式入库的采购物料（待执行入库状态）。
     * <p>实际业务中，APPROVED 状态的入库单表示已审批待上架，物料已到货但库存台账尚未增加。
     */
    private Map<Long, BigDecimal> queryInTransitQuantityByItemIds(Set<Long> itemIds) {
        if (CollUtil.isEmpty(itemIds)) {
            return Collections.emptyMap();
        }
        // 1. 查询所有 status=APPROVED 的入库单（待入库）
        List<MesWmItemReceiptDO> receipts = itemReceiptMapper.selectList(
                new LambdaQueryWrapperX<MesWmItemReceiptDO>()
                        .eq(MesWmItemReceiptDO::getStatus, MesOrderStatusConstants.APPROVED));
        if (CollUtil.isEmpty(receipts)) {
            return Collections.emptyMap();
        }
        List<Long> receiptIds = receipts.stream().map(MesWmItemReceiptDO::getId).collect(Collectors.toList());
        // 2. 查询这些入库单的行表，按 itemId 过滤 + 求和
        List<MesWmItemReceiptLineDO> lines = itemReceiptLineMapper.selectList(
                new LambdaQueryWrapperX<MesWmItemReceiptLineDO>()
                        .in(MesWmItemReceiptLineDO::getReceiptId, receiptIds)
                        .in(MesWmItemReceiptLineDO::getItemId, itemIds));
        Map<Long, BigDecimal> result = new HashMap<>();
        for (MesWmItemReceiptLineDO line : lines) {
            BigDecimal qty = line.getReceivedQuantity() == null ? BigDecimal.ZERO : line.getReceivedQuantity();
            result.merge(line.getItemId(), qty, BigDecimal::add);
        }
        return result;
    }

    /**
     * 构造单条 BOM 行的齐套明细
     */
    private MesProWorkOrderKittingLineRespVO buildKittingLine(MesProWorkOrderBomDO bom,
                                                               MesProWorkOrderDO workOrder,
                                                               BigDecimal workOrderQty,
                                                               Map<Long, MesMdItemDO> itemMap,
                                                               Map<Long, BigDecimal> stockMap,
                                                               Map<Long, BigDecimal> inTransitMap) {
        MesProWorkOrderKittingLineRespVO line = new MesProWorkOrderKittingLineRespVO();
        line.setId(bom.getId());
        line.setWorkOrderId(bom.getWorkOrderId());
        line.setItemId(bom.getItemId());
        // 物料基础信息
        MesMdItemDO item = itemMap.get(bom.getItemId());
        if (item != null) {
            line.setItemCode(item.getCode());
            line.setItemName(item.getName());
            line.setSpecification(item.getSpecification());
            line.setUnitMeasureId(item.getUnitMeasureId());
        }
        // 数量计算
        BigDecimal bomQty = bom.getQuantity() == null ? BigDecimal.ZERO : bom.getQuantity();
        line.setBomQuantity(bomQty);
        line.setWorkOrderQuantity(workOrderQty);
        BigDecimal requiredQty = bomQty.multiply(workOrderQty).setScale(4, RoundingMode.HALF_UP);
        line.setRequiredQuantity(requiredQty);
        // 库存 + 在途
        BigDecimal stockQty = stockMap.getOrDefault(bom.getItemId(), BigDecimal.ZERO);
        BigDecimal inTransitQty = inTransitMap.getOrDefault(bom.getItemId(), BigDecimal.ZERO);
        line.setStockQuantity(stockQty);
        line.setInTransitQuantity(inTransitQty);
        BigDecimal availableQty = stockQty.add(inTransitQty);
        line.setAvailableQuantity(availableQty);
        // 缺口
        BigDecimal shortageQty = requiredQty.subtract(availableQty);
        if (shortageQty.compareTo(BigDecimal.ZERO) < 0) {
            shortageQty = BigDecimal.ZERO;
        }
        line.setShortageQuantity(shortageQty);
        // 齐套状态
        MesProWorkOrderKittingStatusEnum status = determineKittingStatus(requiredQty, stockQty, availableQty);
        line.setKittingStatus(status.getStatus());
        line.setKittingStatusName(status.getName());
        line.setRemark(bom.getRemark());
        return line;
    }

    /**
     * 判定齐套状态
     *
     * <ul>
     *   <li>齐套：库存 ≥ 需求量</li>
     *   <li>部分齐套：库存 &lt; 需求量 ≤ 可用量（库存 + 在途）</li>
     *   <li>短缺：可用量 &lt; 需求量</li>
     * </ul>
     */
    private MesProWorkOrderKittingStatusEnum determineKittingStatus(BigDecimal requiredQty,
                                                                      BigDecimal stockQty,
                                                                      BigDecimal availableQty) {
        if (stockQty.compareTo(requiredQty) >= 0) {
            return MesProWorkOrderKittingStatusEnum.FULLY_KITTED;
        }
        if (availableQty.compareTo(requiredQty) >= 0) {
            return MesProWorkOrderKittingStatusEnum.PARTIAL;
        }
        return MesProWorkOrderKittingStatusEnum.SHORTAGE;
    }

    /**
     * 构造工单整体汇总 VO
     */
    private MesProWorkOrderKittingSummaryRespVO buildSummary(MesProWorkOrderDO workOrder,
                                                              MesMdItemDO productItem,
                                                              List<MesProWorkOrderKittingLineRespVO> lines,
                                                              int fullyKittedCount,
                                                              int partialCount,
                                                              int shortageCount) {
        MesProWorkOrderKittingSummaryRespVO summary = new MesProWorkOrderKittingSummaryRespVO();
        summary.setWorkOrderId(workOrder.getId());
        summary.setWorkOrderCode(workOrder.getCode());
        summary.setWorkOrderName(workOrder.getName());
        summary.setProductId(workOrder.getProductId());
        if (productItem != null) {
            summary.setProductCode(productItem.getCode());
            summary.setProductName(productItem.getName());
        }
        summary.setWorkOrderQuantity(workOrder.getQuantity());
        summary.setWorkOrderStatus(workOrder.getStatus());
        summary.setRequestDate(workOrder.getRequestDate());
        // 行数统计
        int total = lines.size();
        summary.setTotalLineCount(total);
        summary.setFullyKittedCount(fullyKittedCount);
        summary.setPartialCount(partialCount);
        summary.setShortageCount(shortageCount);
        // 齐套率 = (齐套 + 部分齐套) / 总数 × 100
        if (total > 0) {
            BigDecimal rate = BigDecimal.valueOf(fullyKittedCount + partialCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
            summary.setKittingRate(rate);
        } else {
            summary.setKittingRate(BigDecimal.ZERO);
        }
        // 整体齐套 = 短缺数为 0
        summary.setFullyKitted(shortageCount == 0);
        summary.setLines(lines);
        log.info("[analyzeKitting][工单 {} 齐套分析完成：总 {} 行，齐套 {}，部分齐套 {}，短缺 {}，齐套率 {}%]",
                workOrder.getId(), total, fullyKittedCount, partialCount, shortageCount, summary.getKittingRate());
        return summary;
    }

}
