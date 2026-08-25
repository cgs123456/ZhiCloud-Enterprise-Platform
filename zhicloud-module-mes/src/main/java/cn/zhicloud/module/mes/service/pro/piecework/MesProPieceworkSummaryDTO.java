package cn.zhicloud.module.mes.service.pro.piecework;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * MES 计件工资汇总 DTO（按员工 + 月份维度）
 *
 * @author 智云
 */
@Data
public class MesProPieceworkSummaryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 员工编号（报工用户编号）
     */
    private Long employeeId;
    /**
     * 员工姓名
     */
    private String employeeName;
    /**
     * 所属月份（yyyyMM）
     */
    private String periodMonth;
    /**
     * 合格品数量合计
     */
    private BigDecimal totalQualifiedQty;
    /**
     * 废品数量合计
     */
    private BigDecimal totalScrapQty;
    /**
     * 工资金额合计
     */
    private BigDecimal totalAmount;

}
