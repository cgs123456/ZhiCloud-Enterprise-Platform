package cn.zhicloud.module.mes.service.md.bom;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.mes.controller.admin.md.bom.vo.MesBomPageReqVO;
import cn.zhicloud.module.mes.controller.admin.md.bom.vo.MesBomSaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.md.bom.MesBomDO;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;

/**
 * MES 独立 BOM Service 接口
 *
 * @author 智云
 */
public interface MesBomService {

    /**
     * 创建 BOM 主数据
     */
    Long createBom(@Valid MesBomSaveReqVO createReqVO);

    /**
     * 更新 BOM 主数据
     */
    void updateBom(@Valid MesBomSaveReqVO updateReqVO);

    /**
     * 删除 BOM 主数据（同时删除明细）
     */
    void deleteBom(Long id);

    /**
     * 校验 BOM 存在
     */
    MesBomDO validateBomExists(Long id);

    /**
     * 获得 BOM
     */
    MesBomDO getBom(Long id);

    /**
     * 获取产品生效的 BOM（启用状态最新版本）
     */
    MesBomDO getActiveBomByProductId(Long productId);

    /**
     * 获得 BOM 分页
     */
    PageResult<MesBomDO> getBomPage(MesBomPageReqVO pageReqVO);

    /**
     * BOM 递归展开：自顶向下展开所有层级的子件需求
     *
     * @param productId 根产品编号
     * @param quantity  根产品需求数量
     * @return 展开后的全部子件需求（已按损耗率放大）
     */
    List<BomExplodeNode> explodeBom(Long productId, BigDecimal quantity);

    /**
     * BOM 递归展开（支持替代料）：自顶向下展开所有层级的子件需求
     *
     * 当 useSubstitute=true 时，对每条 BOM 明细查询替代料并按 priority 升序选取首选替代料，
     * 用于缺料场景下的替代料需求换算。实际库存校验由调用方（MRP / 齐套分析）在上层完成。
     *
     * @param productId     根产品编号
     * @param quantity      根产品需求数量
     * @param useSubstitute 是否启用替代料（true=缺料时按 priority 选取首选替代料）
     * @return 展开后的全部子件需求（已按损耗率放大）
     */
    List<BomExplodeNode> explodeBom(Long productId, BigDecimal quantity, boolean useSubstitute);

    /**
     * BOM 成本卷积：自底向上累加成本（供 ERP 成本核算调用）
     *
     * @param productId 产品编号
     * @return 单位产品 BOM 成本
     */
    BigDecimal calculateBomCost(Long productId);

    /**
     * BOM 递归展开节点
     */
    class BomExplodeNode {
        private final Long productId;
        private final Integer level;
        private final BigDecimal quantity;
        private final String unit;

        public BomExplodeNode(Long productId, Integer level, BigDecimal quantity, String unit) {
            this.productId = productId;
            this.level = level;
            this.quantity = quantity;
            this.unit = unit;
        }

        public Long getProductId() { return productId; }
        public Integer getLevel() { return level; }
        public BigDecimal getQuantity() { return quantity; }
        public String getUnit() { return unit; }
    }

}