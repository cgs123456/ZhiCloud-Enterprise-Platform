package cn.iocoder.yudao.module.tms.dal.dataobject.carrier;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * TMS 承运商 DO
 *
 * @author yudao
 */
@TableName("tms_carrier")
@KeySequence("tms_carrier_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmsCarrierDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 承运商名称
     */
    private String name;
    /**
     * 承运商编码
     */
    private String code;
    /**
     * 服务类型
     *
     * 10 快递 / 20 零担 / 30 整车 / 40 空运 / 50 海运
     */
    private Integer serviceType;
    /**
     * 联系人
     */
    private String contactPerson;
    /**
     * 联系电话
     */
    private String contactPhone;
    /**
     * 资质编号
     */
    private String qualificationNo;
    /**
     * 评分（1-5）
     */
    private Integer rating;
    /**
     * 状态
     *
     * 10 启用 / 20 停用
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
