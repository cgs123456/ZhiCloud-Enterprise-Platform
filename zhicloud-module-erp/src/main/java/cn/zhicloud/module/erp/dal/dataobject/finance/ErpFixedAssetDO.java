package cn.zhicloud.module.erp.dal.dataobject.finance;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlAccountDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 固定资产 DO
 *
 * <p>记录固定资产主数据，支持直线法折旧。
 *
 * <p>折旧公式（直线法）：
 * <pre>
 *   月折旧额 = (原值 - 残值) / 使用年限月数
 *   累计折旧 = 月折旧额 × 已折旧月数
 *   账面净值 = 原值 - 累计折旧
 * </pre>
 *
 * @author 智云
 */
@TableName("erp_fixed_asset")
@KeySequence("erp_fixed_asset_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpFixedAssetDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 资产编码
     */
    private String code;
    /**
     * 资产名称
     */
    private String name;
    /**
     * 资产类别（如：机器设备/办公设备/车辆/房屋建筑物）
     */
    private String category;
    /**
     * 规格型号
     */
    private String specification;
    /**
     * 部门编号（使用部门）
     */
    private Long departmentId;
    /**
     * 存放地点
     */
    private String location;
    /**
     * 责任人
     */
    private String responsiblePerson;
    /**
     * 资产原值
     */
    private BigDecimal originalValue;
    /**
     * 预计残值
     */
    private BigDecimal salvageValue;
    /**
     * 预计使用年限（月数）
     */
    private Integer usefulLifeMonths;
    /**
     * 折旧方法（10=直线法/Straight-Line；20=双倍余额递减法 DDB；30=年数总和法 SYD）
     *
     * P0-14 当前仅实现直线法（10），其他枚举为预留
     */
    private Integer depreciationMethod;
    /**
     * 入账日期（开始折旧日期）
     */
    private LocalDate capitalizationDate;
    /**
     * 对应资产科目编号（关联 {@link ErpGlAccountDO#getId()}）
     */
    private Long assetAccountId;
    /**
     * 对应累计折旧科目编号（关联 {@link ErpGlAccountDO#getId()}）
     */
    private Long accumulatedDepreciationAccountId;
    /**
     * 对应折旧费用科目编号（关联 {@link ErpGlAccountDO#getId()}）
     */
    private Long depreciationExpenseAccountId;
    /**
     * 已折旧月数
     */
    private Integer depreciatedMonths;
    /**
     * 累计折旧金额
     */
    private BigDecimal accumulatedDepreciation;
    /**
     * 账面净值（原值 - 累计折旧）
     */
    private BigDecimal netBookValue;
    /**
     * 资产状态（10=在用 IN_USE；20=闲置 IDLE；30=处置 DISPOSED；40=报废 SCRAPPED）
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
