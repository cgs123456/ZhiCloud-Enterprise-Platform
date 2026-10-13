package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherRespVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherDO;
import cn.zhicloud.module.erp.enums.finance.ErpGlVoucherStatusEnum;

import java.util.List;

/**
 * ERP 会计凭证 DO → RespVO 转换器
 *
 * <p>控制器（ERP 侧凭证分页/详情）与 {@link ErpGlVoucherApiImpl}（FMS 侧按账套查凭证流水）
 * 都要做同样的「状态名称 / 凭证类型名称」填充，抽到一处避免两处逻辑漂移（CLAUDE.md：边界处独立转换，
 * 转换逻辑本身不重复两遍）。
 *
 * @author 智云
 */
public final class ErpGlVoucherConvert {

    private ErpGlVoucherConvert() {
    }

    /**
     * 凭证类型名称映射：10 收款 / 20 付款 / 30 转账 / 40 记账
     */
    public static String getVoucherTypeName(Integer voucherType) {
        return switch (voucherType) {
            case 10 -> "收款";
            case 20 -> "付款";
            case 30 -> "转账";
            case 40 -> "记账";
            default -> String.valueOf(voucherType);
        };
    }

    /** 单个转换 */
    public static ErpGlVoucherRespVO convert(ErpGlVoucherDO voucher) {
        ErpGlVoucherRespVO respVO = BeanUtils.toBean(voucher, ErpGlVoucherRespVO.class);
        if (respVO.getStatus() != null) {
            for (ErpGlVoucherStatusEnum statusEnum : ErpGlVoucherStatusEnum.values()) {
                if (statusEnum.getStatus().equals(respVO.getStatus())) {
                    respVO.setStatusName(statusEnum.getName());
                    break;
                }
            }
        }
        if (respVO.getVoucherType() != null) {
            respVO.setVoucherTypeName(getVoucherTypeName(respVO.getVoucherType()));
        }
        return respVO;
    }

    /** 批量转换 */
    public static List<ErpGlVoucherRespVO> convertList(List<ErpGlVoucherDO> list) {
        if (list == null) {
            return List.of();
        }
        return list.stream().map(ErpGlVoucherConvert::convert).toList();
    }

}
