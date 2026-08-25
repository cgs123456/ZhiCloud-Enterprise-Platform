package cn.zhicloud.module.wms.service.billing;

import cn.zhicloud.module.wms.dal.dataobject.billing.WmsBillingBillDO;
import cn.zhicloud.module.wms.dal.dataobject.billing.WmsBillingBillLineDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * WMS 3PL 计费引擎 Service 接口
 *
 * 按入库/出库/移库/越库/库存按天计费，产出账单及明细。
 *
 * @author 智云
 */
public interface WmsBillingCalculatorService {

    /**
     * 计算指定货主在计费周期内的费用
     *
     * @param ownerId 货主编号
     * @param start 计费周期开始时间
     * @param end 计费周期结束时间
     * @return 计费结果（账单 + 明细列表）
     */
    BillingCalculationResult calculateBilling(Long ownerId, LocalDateTime start, LocalDateTime end);

    /**
     * 计费结果
     */
    class BillingCalculationResult {

        private final WmsBillingBillDO bill;
        private final List<WmsBillingBillLineDO> lines;

        public BillingCalculationResult(WmsBillingBillDO bill, List<WmsBillingBillLineDO> lines) {
            this.bill = bill;
            this.lines = lines;
        }

        public WmsBillingBillDO getBill() {
            return bill;
        }

        public List<WmsBillingBillLineDO> getLines() {
            return lines;
        }

    }

}
