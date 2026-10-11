package cn.zhicloud.module.hr.dal.dataobject.hrm;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HRM 员工合同 DO（区别于旧 hr_contract 组织合同，本表按员工维度）
 *
 * @author zhicloud
 */
@TableName(value = "hr_employee_contract", autoResultMap = true)
@KeySequence("hr_employee_contract_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmEmployeeContractDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 员工编号
     */
    private Long employeeId;
    /**
     * 合同编码
     */
    private String no;
    /**
     * 合同类型：1劳动合同 2保密协议 3竞业限制 4其他
     */
    private Integer type;
    /**
     * 合同开始日期
     */
    private LocalDateTime startTime;
    /**
     * 合同结束日期
     */
    private LocalDateTime endTime;
    /**
     * 合同期限（月）
     */
    private Integer term;
    /**
     * 合同状态：0未生效 1已生效 2已到期 3已解除
     */
    private Integer status;
    /**
     * 签约公司
     */
    private String signCompany;
    /**
     * 合同签订日期
     */
    private LocalDateTime signTime;
    /**
     * 是否到期提醒
     */
    private Boolean expireRemind;
    /**
     * 附件地址数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> fileUrls;
    /**
     * 备注
     */
    private String remark;
    /**
     * 排序
     */
    private Integer sort;

}
