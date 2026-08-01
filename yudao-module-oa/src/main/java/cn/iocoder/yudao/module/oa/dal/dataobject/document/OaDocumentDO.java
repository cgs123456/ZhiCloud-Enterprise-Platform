package cn.iocoder.yudao.module.oa.dal.dataobject.document;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * OA 公文 DO
 *
 * @author yudao
 */
@TableName("oa_document")
@KeySequence("oa_document_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OaDocumentDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 公文编号
     */
    private String no;
    /**
     * 标题
     */
    private String title;
    /**
     * 公文类型
     * <p>
     * 10 通知 20 通报 30 报告 40 请示 50 决定 60 批复
     */
    private Integer documentType;
    /**
     * 紧急程度
     * <p>
     * 10 普通 20 紧急 30 特急
     */
    private Integer urgency;
    /**
     * 保密级别
     * <p>
     * 10 公开 20 内部 30 秘密
     */
    private Integer confidentiality;
    /**
     * 发文人 ID
     */
    private Long issuerUserId;
    /**
     * 发文部门 ID
     */
    private Long issueDeptId;
    /**
     * 发文日期
     */
    private LocalDate issueDate;
    /**
     * 正文内容
     */
    private String content;
    /**
     * 工作流编号
     */
    private String processInstanceId;
    /**
     * 状态
     * <p>
     * 10 草稿 20 审核中 30 已发布 40 已废止
     */
    private Integer status;
    /**
     * 核稿人 ID
     */
    private Long reviewerUserId;
    /**
     * 核稿人姓名
     */
    private String reviewerName;
    /**
     * 核稿时间
     */
    private LocalDateTime reviewTime;
    /**
     * 核稿意见
     */
    private String reviewOpinion;
    /**
     * 签发人 ID
     */
    private Long signerUserId;
    /**
     * 签发人姓名
     */
    private String signerName;
    /**
     * 签发时间
     */
    private LocalDateTime signTime;
    /**
     * 签发意见
     */
    private String signOpinion;
    /**
     * 归档人 ID
     */
    private Long archiverUserId;
    /**
     * 归档人姓名
     */
    private String archiverName;
    /**
     * 归档时间
     */
    private LocalDateTime archiveTime;
    /**
     * 归档编号
     */
    private String archiveNo;
    /**
     * 主送部门（逗号分隔 ID）
     */
    private String mainSendDepts;
    /**
     * 抄送部门（逗号分隔 ID）
     */
    private String copySendDepts;
    /**
     * 阅读量
     */
    private Integer readCount;
    /**
     * 备注
     */
    private String remark;

}
