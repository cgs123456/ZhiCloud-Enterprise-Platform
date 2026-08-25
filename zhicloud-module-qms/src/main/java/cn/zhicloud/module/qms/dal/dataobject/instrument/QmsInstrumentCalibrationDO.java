package cn.zhicloud.module.qms.dal.dataobject.instrument;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import cn.zhicloud.module.qms.enums.instrument.QmsCalibrationResultEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * QMS 计量器具校准记录 DO
 *
 * @author 智云
 */
@TableName("qms_instrument_calibration")
@KeySequence("qms_instrument_calibration_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsInstrumentCalibrationDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 器具 ID
     *
     * 关联 {@link QmsInstrumentDO#getId()}
     */
    private Long instrumentId;
    /**
     * 校准证书编号
     */
    private String calibrationNo;
    /**
     * 校准日期
     */
    private LocalDate calibrationDate;
    /**
     * 校准机构
     */
    private String calibrationOrganization;
    /**
     * 校准结果
     *
     * 枚举 {@link QmsCalibrationResultEnum}
     */
    private Integer calibrationResult;
    /**
     * 校准证书附件 URL
     */
    private String calibrationCertificateUrl;
    /**
     * 偏差值
     */
    private BigDecimal deviation;
    /**
     * 不确定度
     */
    private String uncertainty;
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
