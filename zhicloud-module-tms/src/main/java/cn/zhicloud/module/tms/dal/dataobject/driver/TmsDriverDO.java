package cn.zhicloud.module.tms.dal.dataobject.driver;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * TMS 司机 DO
 *
 * @author zhicloud
 */
@TableName("tms_driver")
@KeySequence("tms_driver_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmsDriverDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 司机姓名
     */
    private String name;
    /**
     * 手机号
     */
    private String phone;
    /**
     * 驾照号
     */
    private String licenseNo;
    /**
     * 准驾车型
     */
    private String licenseType;
    /**
     * 承运商编号
     */
    private Long carrierId;
    /**
     * 状态
     *
     * 10 可用 / 20 运输中 / 30 休假
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
