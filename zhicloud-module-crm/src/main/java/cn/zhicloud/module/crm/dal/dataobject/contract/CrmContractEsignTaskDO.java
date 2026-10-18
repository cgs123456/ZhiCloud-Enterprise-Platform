package cn.zhicloud.module.crm.dal.dataobject.contract;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.framework.mybatis.core.type.StringListTypeHandler;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CRM 合同电子签任务 DO（ADR 271 D1）
 *
 * 真值载体：任务状态机（10 待签署 / 20 已签署 / 30 已拒签）；
 * crm_contract.esign_task_id 仅为「最新任务号」缓存指针（ADR 271 D2）。
 *
 * @author dhb52
 */
@TableName(value = "crm_contract_esign_task", autoResultMap = true)
@KeySequence("crm_contract_esign_task_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmContractEsignTaskDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 合同编号
     *
     * 关联 {@link CrmContractDO#getId()}
     */
    private Long contractId;
    /**
     * 电子签任务号
     */
    private String taskNo;
    /**
     * 签署状态
     *
     * 枚举 {@link cn.zhicloud.module.crm.enums.contract.CrmContractEsignStatusEnum}
     */
    private Integer status;
    /**
     * 签署完成时间
     */
    private LocalDateTime signTime;
    /**
     * 签署后文件 URL 列表（回调携带）
     */
    @TableField(typeHandler = StringListTypeHandler.class)
    private List<String> signedFileUrls;
    /**
     * 备注
     */
    private String remark;

}
