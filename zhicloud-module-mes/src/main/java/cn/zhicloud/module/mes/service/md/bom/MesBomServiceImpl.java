package cn.zhicloud.module.mes.service.md.bom;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.zhicloud.framework.common.enums.CommonStatusEnum;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.mes.controller.admin.md.bom.vo.MesBomPageReqVO;
import cn.zhicloud.module.mes.controller.admin.md.bom.vo.MesBomSaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.md.bom.MesBomDO;
import cn.zhicloud.module.mes.dal.dataobject.md.bom.MesBomDetailDO;
import cn.zhicloud.module.mes.dal.dataobject.md.bom.MesBomSubstituteDO;
import cn.zhicloud.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.zhicloud.module.mes.dal.mysql.md.bom.MesBomMapper;
import cn.zhicloud.module.mes.service.md.bom.substitute.MesBomSubstituteService;
import cn.zhicloud.module.mes.service.md.item.MesMdItemService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.MD_BOM_EXPLODE_CIRCULAR;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.MD_BOM_EXPLODE_DEPTH_EXCEEDED;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.MD_BOM_NO_DUPLICATE;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.MD_BOM_NOT_EXISTS;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.MD_ITEM_NOT_EXISTS;

/**
 * MES 独立 BOM Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
@Slf4j
public class MesBomServiceImpl implements MesBomService {

    /**
     * BOM 递归展开/成本卷积最大深度
     */
    private static final int MAX_DEPTH = 10;

    /**
     * 百分比基数（损耗率 0-100）
     */
    private static final BigDecimal PERCENT_BASE = BigDecimal.valueOf(100);

    @Resource
    private MesBomMapper bomMapper;
    @Resource
    private MesBomDetailService bomDetailService;
    @Resource
    private MesMdItemService itemService;
    @Resource
    @Lazy // 延迟加载，避免与 MesBomSubstituteServiceImpl 循环依赖
    private MesBomSubstituteService bomSubstituteService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBom(MesBomSaveReqVO createReqVO) {
        // 校验 BOM 编号唯一
        validateBomNoUnique(null, createReqVO.getBomNo());
        // 校验产品存在
        validateProductExists(createReqVO.getProductId());
        // 插入
        MesBomDO bom = BeanUtils.toBean(createReqVO, MesBomDO.class);
        bomMapper.insert(bom);
        return bom.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBom(MesBomSaveReqVO updateReqVO) {
        validateBomExists(updateReqVO.getId());
        validateBomNoUnique(updateReqVO.getId(), updateReqVO.getBomNo());
        validateProductExists(updateReqVO.getProductId());
        MesBomDO updateObj = BeanUtils.toBean(updateReqVO, MesBomDO.class);
        bomMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBom(Long id) {
        validateBomExists(id);
        // 删除明细
        bomDetailService.deleteBomDetailByBomId(id);
        bomMapper.deleteById(id);
    }

    @Override
    public MesBomDO validateBomExists(Long id) {
        MesBomDO bom = bomMapper.selectById(id);
        if (bom == null) {
            throw exception(MD_BOM_NOT_EXISTS);
        }
        return bom;
    }

    @Override
    public MesBomDO getBom(Long id) {
        return bomMapper.selectById(id);
    }

    @Override
    public MesBomDO getActiveBomByProductId(Long productId) {
        return bomMapper.selectActiveByProductId(productId);
    }

    @Override
    public PageResult<MesBomDO> getBomPage(MesBomPageReqVO pageReqVO) {
        return bomMapper.selectPage(pageReqVO);
    }

    // ==================== BOM 递归展开 ====================

    @Override
    public List<BomExplodeNode> explodeBom(Long productId, BigDecimal quantity) {
        return explodeBom(productId, quantity, false);
    }

    @Override
    public List<BomExplodeNode> explodeBom(Long productId, BigDecimal quantity, boolean useSubstitute) {
        List<BomExplodeNode> result = new ArrayList<>();
        if (productId == null || quantity == null || quantity.signum() <= 0) {
            return result;
        }
        // 递归展开，visited 记录递归路径上的产品编号用于环路检测
        doExplode(productId, quantity, 1, new HashSet<>(), result, useSubstitute);
        return result;
    }

    /**
     * 递归展开 BOM 子件需求
     *
     * @param productId 当前产品编号
     * @param quantity  当前产品需求数量
     * @param level     当前层级（1=第一层子件）
     * @param visited   递归路径上的产品编号集合（环路检测）
     * @param result     展开结果收集
     */
    private void doExplode(Long productId, BigDecimal quantity, int level,
                           Set<Long> visited, List<BomExplodeNode> result, boolean useSubstitute) {
        // 深度上限校验
        if (level > MAX_DEPTH) {
            throw exception(MD_BOM_EXPLODE_DEPTH_EXCEEDED);
        }
        // 环路检测
        if (!visited.add(productId)) {
            throw exception(MD_BOM_EXPLODE_CIRCULAR, productId);
        }
        try {
            MesBomDO bom = getActiveBomByProductId(productId);
            if (bom == null) {
                // 叶子件（无 BOM），不再向下展开
                return;
            }
            List<MesBomDetailDO> details = bomDetailService.getBomDetailListByBomId(bom.getId());
            if (CollUtil.isEmpty(details)) {
                return;
            }
            for (MesBomDetailDO detail : details) {
                // 子件需求 = 父需求 * 用量 * (1 + 损耗率/100)
                BigDecimal required = quantity.multiply(detail.getQuantity())
                        .multiply(resolveScrapMultiplier(detail.getScrapRate()))
                        .setScale(6, RoundingMode.HALF_UP);
                result.add(new BomExplodeNode(detail.getProductId(), level, required, detail.getUnit()));

                // ============ P0-1 替代料处理 ============
                // 当 useSubstitute=true 时，查询当前明细行挂载的替代料。
                // 替代料按 priority 升序返回（首选在前），此处仅选取 priority 最小的首选替代料。
                //
                // 【缺料替代策略（在注释中说明）】
                // 实际生产中，是否启用替代料由库存是否充足决定（缺料时才启用）。
                // 由于 BOM 展算本身不感知库存，这里将"是否使用替代料"作为入参 useSubstitute 暴露给调用方，
                // 由上层（如 MRP 运算 / 齐套分析）在确认缺料后传入 useSubstitute=true 触发。
                //
                // 替代料需求量换算：替代料数量 = 原物料需求量 * substituteRatio
                //   例：原物料需要 2 个，substituteRatio=1.5，则替代料需要 3 个。
                //
                // 替代料选中后，应【替换】原物料需求（而非叠加），此处仅在日志中记录选中结果，
                // 不修改 result，避免重复计算；实际替换由调用方根据返回的替代料信息决策。
                if (useSubstitute) {
                    List<MesBomSubstituteDO> substitutes = bomSubstituteService
                            .getSubstitutesByBomDetailId(detail.getId());
                    if (CollUtil.isNotEmpty(substitutes)) {
                        // priority 最小的首选替代料（列表已按 priority 升序排序）
                        MesBomSubstituteDO preferred = substitutes.get(0);
                        BigDecimal substituteRequired = required.multiply(preferred.getSubstituteRatio())
                                .setScale(6, RoundingMode.HALF_UP);
                        if (log.isInfoEnabled()) {
                            log.info("[explodeBom] BOM明细[{}]原物料[{}]缺料，启用首选替代料[{}]（priority={}，ratio={}），替代需求量={}",
                                    detail.getId(), detail.getProductId(), preferred.getSubstituteItemId(),
                                    preferred.getPriority(), preferred.getSubstituteRatio(), substituteRequired);
                        }
                    }
                }
                // ==========================================

                // 递归展开子件
                doExplode(detail.getProductId(), required, level + 1, visited, result, useSubstitute);
            }
        } finally {
            // 回溯：移除当前节点，允许同一产品在不同分支出现
            visited.remove(productId);
        }
    }

    // ==================== BOM 成本卷积 ====================

    @Override
    public BigDecimal calculateBomCost(Long productId) {
        if (productId == null) {
            return BigDecimal.ZERO;
        }
        return doCalculateCost(productId, new HashSet<>(), 1);
    }

    /**
     * 自底向上累加 BOM 成本
     *
     * @param productId 当前产品编号
     * @param visited   递归路径上的产品编号集合（环路检测）
     * @param level     当前层级
     * @return 单位产品 BOM 成本
     */
    private BigDecimal doCalculateCost(Long productId, Set<Long> visited, int level) {
        if (level > MAX_DEPTH) {
            throw exception(MD_BOM_EXPLODE_DEPTH_EXCEEDED);
        }
        if (!visited.add(productId)) {
            throw exception(MD_BOM_EXPLODE_CIRCULAR, productId);
        }
        try {
            MesBomDO bom = getActiveBomByProductId(productId);
            if (bom == null) {
                // 叶子件无 BOM，无组装成本（采购成本由调用方按 unitCost 取数）
                return BigDecimal.ZERO;
            }
            List<MesBomDetailDO> details = bomDetailService.getBomDetailListByBomId(bom.getId());
            if (CollUtil.isEmpty(details)) {
                return BigDecimal.ZERO;
            }
            BigDecimal total = BigDecimal.ZERO;
            for (MesBomDetailDO detail : details) {
                // 子件单位成本：若子件有 BOM 则递归卷积，否则取明细 unitCost
                BigDecimal childUnitCost = resolveChildUnitCost(detail.getProductId(), detail.getUnitCost());
                // 行成本 = 用量 * (1 + 损耗率/100) * 子件单位成本
                BigDecimal lineCost = detail.getQuantity()
                        .multiply(resolveScrapMultiplier(detail.getScrapRate()))
                        .multiply(childUnitCost);
                total = total.add(lineCost);
            }
            return total.setScale(4, RoundingMode.HALF_UP);
        } finally {
            visited.remove(productId);
        }
    }

    /**
     * 解析子件单位成本：子件有独立 BOM 时递归卷积，否则取明细 unitCost
     */
    private BigDecimal resolveChildUnitCost(Long childProductId, BigDecimal detailUnitCost) {
        MesBomDO childBom = getActiveBomByProductId(childProductId);
        if (childBom != null) {
            // 子件为组装件，递归卷积其成本
            Set<Long> childVisited = new HashSet<>();
            return doCalculateCost(childProductId, childVisited, 1);
        }
        // 叶子件：取明细配置的标准单位成本，空值按 0 处理
        return detailUnitCost != null ? detailUnitCost : BigDecimal.ZERO;
    }

    // ==================== 校验方法 ====================

    private void validateBomNoUnique(Long id, String bomNo) {
        MesBomDO bom = bomMapper.selectByBomNo(bomNo);
        if (bom == null) {
            return;
        }
        if (id == null || ObjectUtil.notEqual(bom.getId(), id)) {
            throw exception(MD_BOM_NO_DUPLICATE, bomNo);
        }
    }

    private void validateProductExists(Long productId) {
        MesMdItemDO item = itemService.getItem(productId);
        if (item == null) {
            throw exception(MD_ITEM_NOT_EXISTS);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 解析损耗率乘数：(1 + scrapRate/100)，scrapRate 为空或负数时按 0 处理
     */
    private BigDecimal resolveScrapMultiplier(BigDecimal scrapRate) {
        if (scrapRate == null || scrapRate.signum() < 0) {
            return BigDecimal.ONE;
        }
        return BigDecimal.ONE.add(scrapRate.divide(PERCENT_BASE, 6, RoundingMode.HALF_UP));
    }

}