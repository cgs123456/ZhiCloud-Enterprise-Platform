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

}
