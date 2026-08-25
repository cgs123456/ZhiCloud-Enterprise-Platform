package cn.zhicloud.module.mes.dal.dataobject.dv.tp;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * MES TPM 计划项目 DO
 *
 * @author 智云
 */
@TableName("mes_dv_tp_plan_item")
@KeySequence("mes_dv_tp_plan_item_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesDvTpPlanItemDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * TPM 计划编号
     *
     * 关联 {@link MesDvTpPlanDO#getId()}
     */
    private Long planId;
    /**
     * 项目名称
     */
    private String itemName;
    /**
     * 项目内容
     */
    private String itemContent;
    /**
     * 标准
     */
    private String standard;
    /**
     * 方法
     *
     * 枚举 {@link cn.zhicloud.module.mes.enums.dv.tp.MesDvTpItemMethodEnum}
     */
    private Integer method;
    /**
     * 备注
     */
    private String remark;
    /**
     * 排序
     */
    private Integer sort;

}