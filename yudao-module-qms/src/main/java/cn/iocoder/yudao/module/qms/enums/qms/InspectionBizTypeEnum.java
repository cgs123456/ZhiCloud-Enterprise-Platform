package cn.iocoder.yudao.module.qms.enums.qms;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 检验单业务类型枚举
 *
 * <p>用于把检验单与业务单据（采购入库 / 生产完工入库 / 盘点 / 其他）建立通用关联，
 * 支撑 {@code InspectionOrderApi.isQualified(bizType, bizId)} 的入库前质检卡点查询。
 *
 * @author 智云
 */
@RequiredArgsConstructor
@Getter
public enum InspectionBizTypeEnum implements ArrayValuable<String> {

    PURCHASE_IN("PURCHASE_IN", "采购入库"),
    PRODUCTION_OUT("PRODUCTION_OUT", "生产完工入库"),
    STOCK_COUNT("STOCK_COUNT", "库存盘点"),
    OTHER("OTHER", "其他"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values()).map(InspectionBizTypeEnum::getBizType).toArray(String[]::new);

    /**
     * 业务类型
     */
    private final String bizType;
    /**
     * 名称
     */
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
