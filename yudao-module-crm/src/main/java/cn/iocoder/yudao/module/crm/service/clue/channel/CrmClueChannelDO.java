package cn.iocoder.yudao.module.crm.service.clue.channel;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * CRM 线索渠道 DO
 *
 * @author dhb52
 */
@TableName(value = "crm_clue_channel")
@KeySequence("crm_clue_channel_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmClueChannelDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 渠道名称
     */
    private String channelName;
    /**
     * 渠道类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.crm.enums.clue.CrmClueChannelTypeEnum}
     */
    private Integer channelType;
    /**
     * 渠道编码
     */
    private String channelCode;
    /**
     * API 地址，若为 API 渠道
     */
    private String apiUrl;
    /**
     * API 密钥
     */
    private String apiKey;
    /**
     * 自动分配人
     *
     * 关联 AdminUserDO 的 id 字段
     */
    private Long autoAssignUserId;
    /**
     * 状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.crm.enums.clue.CrmClueChannelStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
