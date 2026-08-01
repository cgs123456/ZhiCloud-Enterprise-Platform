package cn.iocoder.yudao.module.wms.job.check;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.module.wms.controller.admin.order.check.vo.detail.WmsCheckOrderDetailSaveReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.order.check.vo.order.WmsCheckOrderSaveReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.item.WmsItemDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import cn.iocoder.yudao.module.wms.controller.admin.inventory.vo.WmsInventoryListReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.check.WmsCheckCyclePlanDO;
import cn.iocoder.yudao.module.wms.dal.mysql.inventory.WmsInventoryMapper;
import cn.iocoder.yudao.module.wms.enums.order.WmsCheckTypeEnum;
import cn.iocoder.yudao.module.wms.service.md.item.WmsItemService;
import cn.iocoder.yudao.module.wms.service.md.item.WmsItemSkuService;
import cn.iocoder.yudao.module.wms.service.order.check.WmsCheckCyclePlanService;
import cn.iocoder.yudao.module.wms.service.order.check.WmsCheckOrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * WMS 循环盘点 Job
 *
 * <p>每日扫描到期的循环盘点计划，自动生成 CYCLE 盘点单。
 * <p>建议调度：每日凌晨执行（cron: 0 0 1 * * ?）
 *
 * @author 芋道源码
 */
@Component
@Slf4j
public class WmsCheckCycleJob implements JobHandler {

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private WmsCheckCyclePlanService checkCyclePlanService;
    @Resource
    private WmsCheckOrderService checkOrderService;
    @Resource
    private WmsInventoryMapper inventoryMapper;
    @Resource
    private WmsItemSkuService itemSkuService;
    @Resource
    private WmsItemService itemService;

    @Override
    public String execute(String param) {
        LocalDate today = LocalDate.now();
        List<WmsCheckCyclePlanDO> duePlans = checkCyclePlanService.getDueCheckCyclePlanList(today);
        if (CollUtil.isEmpty(duePlans)) {
            log.info("[execute][今日无到期循环盘点计划]");
            return "今日无到期循环盘点计划";
        }
        int generated = 0;
        for (WmsCheckCyclePlanDO plan : duePlans) {
            try {
                Long orderId = generateCycleCheckOrder(plan);
                if (orderId != null) {
                    generated++;
                    // 回写下次盘点日期
                    checkCyclePlanService.updateNextCheckDate(plan.getId(),
                            today.plusDays(plan.getCycleDays()));
                }
            } catch (Exception e) {
                log.error("[execute][生成循环盘点单失败 planId={}]", plan.getId(), e);
            }
        }
        log.info("[execute][循环盘点生成 {} 张盘点单]", generated);
        return String.format("循环盘点生成 %s 张盘点单", generated);
    }

    /**
     * 根据计划生成一张循环盘点单
     */
    private Long generateCycleCheckOrder(WmsCheckCyclePlanDO plan) {
        // 1. 查询仓库下所有库存
        WmsInventoryListReqVO listReqVO = new WmsInventoryListReqVO();
        listReqVO.setWarehouseId(plan.getWarehouseId());
        List<WmsInventoryDO> inventoryList = inventoryMapper.selectList(listReqVO);
        if (CollUtil.isEmpty(inventoryList)) {
            log.info("[generateCycleCheckOrder][仓库 {} 无库存，跳过]", plan.getWarehouseId());
            return null;
        }
        // 2. 查询 SKU 与 Item 信息，按 ABC 过滤
        Set<Long> skuIds = inventoryList.stream().map(WmsInventoryDO::getSkuId).collect(Collectors.toSet());
        Map<Long, WmsItemSkuDO> skuMap = itemSkuService.getItemSkuMap(skuIds);
        Set<Long> itemIds = skuMap.values().stream().map(WmsItemSkuDO::getItemId).collect(Collectors.toSet());
        Map<Long, WmsItemDO> itemMap = itemService.getItemMap(itemIds);
        String targetAbc = plan.getAbcClassification() == null ? null : plan.getAbcClassification().toUpperCase();

        // 3. 构造盘点明细
        List<WmsCheckOrderDetailSaveReqVO> details = new ArrayList<>();
        for (WmsInventoryDO inventory : inventoryList) {
            WmsItemSkuDO sku = skuMap.get(inventory.getSkuId());
            if (sku == null) {
                continue;
            }
            WmsItemDO item = itemMap.get(sku.getItemId());
            if (item == null) {
                continue;
            }
            String abc = item.getAbcClassification();
            if (abc == null || !abc.toUpperCase().equals(targetAbc)) {
                continue;
            }
            WmsCheckOrderDetailSaveReqVO detail = new WmsCheckOrderDetailSaveReqVO();
            detail.setSkuId(inventory.getSkuId());
            detail.setInventoryId(inventory.getId());
            detail.setQuantity(inventory.getQuantity() == null ? BigDecimal.ZERO : inventory.getQuantity());
            // 实盘数量初始为 0，由盘点人填入
            detail.setCheckQuantity(BigDecimal.ZERO);
            details.add(detail);
        }
        if (details.isEmpty()) {
            log.info("[generateCycleCheckOrder][仓库 {} ABC={} 无匹配库存]", plan.getWarehouseId(), targetAbc);
            return null;
        }

        // 4. 构造盘点单
        WmsCheckOrderSaveReqVO orderReqVO = new WmsCheckOrderSaveReqVO();
        orderReqVO.setNo(String.format("CYCLE-%d-%s-%s", plan.getWarehouseId(), targetAbc,
                LocalDateTime.now().format(NO_FORMATTER)));
        orderReqVO.setOrderTime(LocalDateTime.now());
        orderReqVO.setWarehouseId(plan.getWarehouseId());
        orderReqVO.setCheckType(WmsCheckTypeEnum.CYCLE.getType());
        orderReqVO.setCycleDays(plan.getCycleDays());
        orderReqVO.setRemark(String.format("循环盘点自动生成（ABC=%s, 周期=%d 天）", targetAbc, plan.getCycleDays()));
        orderReqVO.setDetails(details);
        return checkOrderService.createCheckOrder(orderReqVO);
    }

}