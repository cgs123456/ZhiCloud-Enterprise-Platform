package cn.iocoder.yudao.module.erp.dal.dataobject.stock;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 产品库存 DO
 *
 * @author 芋道源码
 */
@TableName("erp_stock")
@KeySequence("erp_stock_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpStockDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 产品编号
     *
     * 关联 {@link ErpProductDO#getId()}
     */
    private Long productId;
    /**
     * 仓库编号
     *
     * 关联 {@link ErpWarehouseDO#getId()}
     */
    private Long warehouseId;
    /**
     * 库存数量
     */
    private BigDecimal count;
    /**
     * 锁定数量（已预留未出库的库存，例如销售订单确认后锁定）
     *
     * <p>可用库存 = count - lockedCount
     *
     * <p>TODO 配套 DDL：{@code ALTER TABLE erp_stock ADD COLUMN locked_count DECIMAL(20,4) DEFAULT 0 COMMENT '锁定数量';}
     */
    private BigDecimal lockedCount;

}