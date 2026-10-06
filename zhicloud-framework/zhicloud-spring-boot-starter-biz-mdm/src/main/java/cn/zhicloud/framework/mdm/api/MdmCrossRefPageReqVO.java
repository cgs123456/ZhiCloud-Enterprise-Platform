package cn.zhicloud.framework.mdm.api;

import cn.zhicloud.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 主数据跨域对照分页查询请求（R-014）
 *
 * <p>所有字段都是「可选过滤」：空则不参与条件（由 Mapper 的 eqIfPresent 处理）。
 * 不带租户字段——分页查询由租户拦截器按当前上下文过滤，运维看的就是本租户的映射。
 *
 * @author 智云集成治理
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MdmCrossRefPageReqVO extends PageParam {

    /** 源系统，如 ERP（精确匹配，可选） */
    private String sourceSystem;

    /** 源实体类型，如 PRODUCT（精确匹配，可选） */
    private String sourceEntity;

    /** 目标系统，如 WMS（精确匹配，可选） */
    private String targetSystem;

    /** 目标实体类型，如 SKU（精确匹配，可选） */
    private String targetEntity;

    /** 状态 VALID/INVALID（精确匹配，可选） */
    private String status;

}
