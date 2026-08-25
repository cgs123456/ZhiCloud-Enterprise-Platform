package cn.zhicloud.module.qms.dal.dataobject.training;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import cn.zhicloud.module.qms.enums.qms.QualificationStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * QMS 岗位资格 DO
 *
 * <p>岗位资格矩阵，记录人员资格与到期预警。
 *
 * @author zhicloud
 */
@TableName("qms_qualification")
@KeySequence("qms_qualification_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualificationDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 用户 ID
     */
    private Long userId;
    /**
     * 用户姓名
     */
    private String userName;
    /**
     * 岗位 ID
     */
    private Long postId;
    /**
     * 岗位名称
     */
    private String postName;
    /**
     * 资格名称
     */
    private String qualificationName;
    /**
     * 取得日期
     */
    private LocalDate qualifyDate;
    /**
     * 到期日期
     */
    private LocalDate expireDate;
    /**
     * 状态
     *
     * 枚举 {@link QualificationStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}