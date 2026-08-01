package cn.iocoder.yudao.module.crm.dal.dataobject.visit;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.mybatis.core.type.StringListTypeHandler;
import cn.iocoder.yudao.module.crm.dal.dataobject.contact.CrmContactDO;
import cn.iocoder.yudao.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * CRM 拜访签到记录 DO
 *
 * @author 芋道源码
 */
@TableName(value = "crm_visit_record", autoResultMap = true)
@KeySequence("crm_visit_record_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmVisitRecordDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 客户编号
     *
     * 关联 {@link CrmCustomerDO#getId()}
     */
    private Long customerId;
    /**
     * 联系人编号
     *
     * 关联 {@link CrmContactDO#getId()}
     */
    private Long contactId;
    /**
     * 负责人的用户编号
     *
     * 关联 AdminUserDO 的 id 字段
     */
    private Long ownerUserId;
    /**
     * 拜访时间
     */
    private LocalDateTime visitTime;
    /**
     * 签到时间
     */
    private LocalDateTime signInTime;
    /**
     * 签到纬度
     */
    private BigDecimal signInLatitude;
    /**
     * 签到经度
     */
    private BigDecimal signInLongitude;
    /**
     * 签到地址
     */
    private String signInAddress;
    /**
     * 拜访类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.crm.enums.visit.CrmVisitTypeEnum}
     */
    private Integer visitType;
    /**
     * 拜访内容
     */
    private String content;
    /**
     * 图片 URL 列表
     */
    @TableField(typeHandler = StringListTypeHandler.class)
    private List<String> picUrls;
    /**
     * 附件 URL 列表
     */
    @TableField(typeHandler = StringListTypeHandler.class)
    private List<String> fileUrls;

}
