package cn.zhicloud.module.mes.dal.dataobject.pro.route;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * MES 工艺路线产能 DO
 *
 * 工艺路线维度产能输入（件/小时）+ 效率系数：APS 时长估算优先取本表产能，
 * 未配置（无行 / capacity ≤ 0）回退工位平均产能（ADR 270 D3，§12.270 D4-A）。
 *
 * @author 智云
 */
@TableName("mes_pro_route_capacity")
@KeySequence("mes_pro_route_capacity_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProRouteCapacityDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 工艺路线编号
     *
     * 关联 {@link MesProRouteDO#getId()}
     */
    private Long routeId;
    /**
     * 产能（件/小时）
     *
     * 0 = 未配置（估算回退工位平均产能）
     */
    private BigDecimal capacity;
    /**
     * 效率系数
     *
     * ＞0；≤0/未配置按 1.0 处理
     */
    private BigDecimal efficiency;
    /**
     * 备注
     */
    private String remark;

}
