package cn.iocoder.yudao.module.erp.dal.dataobject.collaboration.cpfr;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP CPFR 联合计划预测补货 DO
 *
 * @author 芋道源码
 */
@TableName("erp_cpfr_forecast")
@KeySequence("erp_cpfr_forecast_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpCpfrForecastDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 预测单号
     */
    private String no;
    /**
     * 合作伙伴类型
     *
     * 10 供应商 / 20 客户
     */
    private Integer partnerType;
    /**
     * 合作伙伴编号
     */
    private Long partnerId;
    /**
     * 产品编号
     */
    private Long productId;
    /**
     * 产品名称（冗余）
     */
    private String productName;
    /**
     * 预测周期（yyyyMM）
     */
    private String forecastPeriod;
    /**
     * 预测数量
     */
    private BigDecimal forecastQuantity;
    /**
     * 实际数量
     */
    private BigDecimal actualQuantity;
    /**
     * 偏差率
     *
     * deviationRate = (forecastQuantity - actualQuantity) / forecastQuantity
     */
    private BigDecimal deviationRate;
    /**
     * 备注
     */
    private String remark;

}
