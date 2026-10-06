package cn.zhicloud.framework.mdm.service;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mdm.api.MdmCrossRefPageReqVO;
import cn.zhicloud.framework.mdm.api.MdmCrossRefUpsertReq;
import cn.zhicloud.framework.mdm.dal.dataobject.MdmCrossRefDO;

/**
 * 主数据跨域对照 Service（V101）
 *
 * <p>解析失败必须抛异常而不是返回 null 兜底：跨模块集成里「映射缺失」是数据问题，
 * 用默认值或跳过会让下游单据建到错误的 SKU / 仓库上，且事后几乎无法发现。
 * 抛异常后由可靠集成数据面（V100）重试，重试仍然失败则进入死信并告警，问题可见。
 *
 * <p>所有操作都绑定当前租户上下文：上下文缺失时拒绝；显式传入的租户编号必须与上下文一致。
 * 禁止在没有上下文或租户不一致时静默回退到默认租户 0。
 *
 * @author 智云集成治理
 */
public interface MdmCrossRefService {

    /**
     * 登记（幂等）一条对照关系
     *
     * @return 对照主键
     */
    Long upsert(MdmCrossRefUpsertReq req);

    /**
     * 解析目标编号：找不到生效映射时抛 {@code MDM_CROSS_REF_NOT_FOUND}
     */
    String mapTargetId(Long tenantId, String sourceSystem, String sourceEntity, String sourceId,
                       String targetSystem, String targetEntity);

    /**
     * 解析目标编号并转成 Long：目标编号不是数字时抛 {@code MDM_CROSS_REF_TARGET_ID_INVALID}
     */
    Long mapTargetIdAsLong(Long tenantId, String sourceSystem, String sourceEntity, String sourceId,
                           String targetSystem, String targetEntity);

    /**
     * 解析目标编号：找不到生效映射时返回 null。
     * <p>只用于「有没有映射都不影响业务正确性」的可选字段（如 ERP 客户 -> WMS 往来单位）；
     * 决定下游单据建到哪的关键字段禁止使用本方法。
     */
    String findTargetId(Long tenantId, String sourceSystem, String sourceEntity, String sourceId,
                        String targetSystem, String targetEntity);

    /**
     * 失效一条对照关系（软删除语义：保留历史，不再参与解析）
     *
     * @return 是否命中并失效
     */
    boolean invalidate(Long tenantId, String sourceSystem, String sourceEntity, String sourceId,
                       String targetSystem, String targetEntity);

    /**
     * 运维分页查询本租户的对照关系（R-014）：
     * 之前映射只能靠 SQL 手工种，没有可读的维护入口——集成断链时运营无法自查缺哪条映射。
     * 由租户拦截器按当前上下文过滤，只看本租户。
     */
    PageResult<MdmCrossRefDO> page(MdmCrossRefPageReqVO reqVO);

}