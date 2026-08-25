package cn.zhicloud.module.oa.dal.dataobject.meeting;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * OA 会议室 DO
 *
 * @author zhicloud
 */
@TableName("oa_meeting_room")
@KeySequence("oa_meeting_room_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OaMeetingRoomDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 会议室名称
     */
    private String name;
    /**
     * 位置
     */
    private String location;
    /**
     * 容纳人数
     */
    private Integer capacity;
    /**
     * 设备配置（如投影/白板/视频会议，逗号分隔）
     */
    private String equipment;
    /**
     * 状态
     * <p>
     * 10 可用 20 维修中 30 已停用
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
