package cn.iocoder.yudao.module.qms.dal.dataobject.instrument;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.qms.enums.instrument.QmsInstrumentCategoryEnum;
import cn.iocoder.yudao.module.qms.enums.instrument.QmsInstrumentStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * QMS 计量器具台账 DO
 *
 * @author 芋道源码
 */
@TableName("qms_instrument")
@KeySequence("qms_instrument_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsInstrumentDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 器具编号
     */
    private String code;
    /**
     * 器具名称
     */
    private String name;
    /**
     * 型号规格
     */
    private String model;
    /**
     * 生产厂家
     */
    private String manufacturer;
    /**
     * 出厂编号
     */
    private String serialNo;
    /**
     * 类别
     *
     * 枚举 {@link QmsInstrumentCategoryEnum}
     */
    private Integer category;
    /**
     * 精度等级
     */
    private String accuracy;
    /**
     * 测量范围
     */
    private String measureRange;
    /**
     * 计量单位
     */
    private String unit;
    /**
     * 状态
     *
     * 枚举 {@link QmsInstrumentStatusEnum}
     */
    private Integer status;
    /**
     * 使用地点
     */
    private String location;
    /**
     * 负责人
     */
    private String responsiblePerson;
    /**
     * 校准周期天数
     */
    private Integer calibrationCycleDays;
    /**
     * 上次校准日期
     */
    private LocalDate lastCalibrationDate;
    /**
     * 下次校准日期
     */
    private LocalDate nextCalibrationDate;
    /**
     * 备注
     */
    private String remark;
    /**
     * 排序
     */
    private Integer sort;

}
