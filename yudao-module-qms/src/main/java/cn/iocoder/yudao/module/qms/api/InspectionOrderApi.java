package cn.iocoder.yudao.module.qms.api;

/**
 * QMS 检验单 API
 *
 * <p>对外暴露质检结论查询能力，供 WMS/MES 等模块在入库前做质检卡点（不合格拒绝入库）。
 * 属于 P1「模块边界治理（api 包）」的 QMS 一侧落地，后续跨模块消费方应通过本 API 而非直接 import dal/service。
 *
 * @author 智云
 */
public interface InspectionOrderApi {

    /**
     * 判断指定业务单据是否质检合格（入库前卡点）
     *
     * @param bizType 业务类型（枚举 {@link cn.iocoder.yudao.module.qms.enums.qms.InspectionBizTypeEnum}）
     * @param bizId   业务单据 ID
     * @return true=最新检验单为「检验通过」；无检验单或非通过均返回 false（fail-closed，不得放行）
     */
    boolean isQualified(String bizType, Long bizId);

    /**
     * 判断指定业务单据是否存在「检验不通过」的最新检验单（MES 完工入库卡点用，宽松语义）。
     *
     * <p>与 {@link #isQualified} 的 fail-closed 不同：本方法仅在「存在检验单且状态为检验不通过」时返回 true；
     * 无检验单或检验通过均返回 false。用于 MES 等尚未强制全量质检的场景——只拦截明确判废的工单，
     * 未做质检的工单不阻断，避免一次性 fail-closed 破坏既有生产入库流程。
     *
     * @param bizType 业务类型（枚举 {@link cn.iocoder.yudao.module.qms.enums.qms.InspectionBizTypeEnum}）
     * @param bizId   业务单据 ID
     * @return true=最新检验单为「检验不通过」
     */
    boolean hasFailedInspection(String bizType, Long bizId);

}
