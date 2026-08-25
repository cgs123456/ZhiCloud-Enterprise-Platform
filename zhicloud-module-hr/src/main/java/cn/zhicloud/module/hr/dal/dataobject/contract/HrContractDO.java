package cn.zhicloud.module.hr.dal.dataobject.contract;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import cn.zhicloud.module.hr.enums.contract.HrContractStatusEnum;
import cn.zhicloud.module.hr.enums.contract.HrContractTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@TableName("hr_contract")
@KeySequence("hr_contract_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrContractDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long employeeId;
    private String contractNo;
    private Integer contractType;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate signDate;
    private LocalDate probationEndDate;
    private Long positionId;
    private Long departmentId;
    private BigDecimal salary;
    private Integer status;
    private String fileUrl;
    private String remark;

}