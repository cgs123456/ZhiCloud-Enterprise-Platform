package cn.zhicloud.module.erp.dal.dataobject.collaboration.cpfr;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ERP CPFR 协同异常 DO
 *
 * @author 智云
 */
@TableName("erp_cpfr_exception")
@KeySequence("erp_cpfr_exception_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpCpfrExceptionDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 预测编号
     */
    private Long forecastId;
    /**
     * 异常类型
     *
     * 10 预测偏差超限 / 20 库存异常 / 30 补货异常
     */
    private Integer exceptionType;
    /**
     * 异常描述
     */
    private String exceptionDescription;
    /**
     * 处理状态
     *
     * 10 待处理 / 20 处理中 / 30 已解决
     */
    private Integer handlingStatus;
    /**
     * 处理人编号
     */
    private Long handlerUserId;
    /**
     * 处理时间
     */
    private LocalDateTime handlingTime;
    /**
     * 备注
     */
    private String remark;

}
