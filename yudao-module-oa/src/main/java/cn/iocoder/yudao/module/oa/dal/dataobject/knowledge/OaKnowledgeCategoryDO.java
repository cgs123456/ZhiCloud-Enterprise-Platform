package cn.iocoder.yudao.module.oa.dal.dataobject.knowledge;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * OA 知识库分类 DO
 *
 * @author yudao
 */
@TableName("oa_knowledge_category")
@KeySequence("oa_knowledge_category_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OaKnowledgeCategoryDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 父分类 ID（0 为根）
     */
    private Long parentId;
    /**
     * 分类名称
     */
    private String name;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 状态
     * <p>
     * 0 启用 1 停用
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
