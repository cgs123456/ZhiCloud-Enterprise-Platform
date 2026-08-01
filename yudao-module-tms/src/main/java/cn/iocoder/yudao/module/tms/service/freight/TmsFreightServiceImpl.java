package cn.iocoder.yudao.module.tms.service.freight;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.tms.controller.admin.freight.vo.TmsFreightCalculateReqVO;
import cn.iocoder.yudao.module.tms.controller.admin.freight.vo.TmsFreightCalculateRespVO;
import cn.iocoder.yudao.module.tms.controller.admin.freight.vo.TmsFreightPageReqVO;
import cn.iocoder.yudao.module.tms.controller.admin.freight.vo.TmsFreightSaveReqVO;
import cn.iocoder.yudao.module.tms.dal.dataobject.freight.TmsFreightDO;
import cn.iocoder.yudao.module.tms.dal.dataobject.shipment.TmsShipmentDO;
import cn.iocoder.yudao.module.tms.dal.mysql.freight.TmsFreightMapper;
import cn.iocoder.yudao.module.tms.dal.mysql.shipment.TmsShipmentMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.tms.enums.ErrorCodeConstants.*;

/**
 * TMS 运费结算 Service 实现类
 *
 * @author yudao
 */
@Service
@Validated
public class TmsFreightServiceImpl implements TmsFreightService {

    /**
     * 结算状态：待审核
     */
    private static final int STATUS_PENDING_AUDIT = 10;
    /**
     * 结算状态：已审核
     */
    private static final int STATUS_AUDITED = 20;
    /**
     * 结算状态：已结算
     */
    private static final int STATUS_SETTLED = 30;
    /**
     * 结算状态：已驳回
     */
    private static final int STATUS_REJECTED = 40;

    /**
     * 计费方式：按重量
     */
    private static final int BILLING_BY_WEIGHT = 10;
    /**
     * 计费方式：按体积
     */
    private static final int BILLING_BY_VOLUME = 20;
    /**
     * 计费方式：按件数
     */
    private static final int BILLING_BY_PIECE = 30;
    /**
     * 计费方式：整车一口价
     */
    private static final int BILLING_FLAT_RATE = 40;
    /**
     * 计费方式：里程计费
     */
    private static final int BILLING_BY_MILEAGE = 50;

    /**
     * 运单状态：已签收
     */
    private static final int SHIPMENT_STATUS_SIGNED = 40;

    @Resource
    private TmsFreightMapper freightMapper;
    @Resource
    private TmsShipmentMapper shipmentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFreight(TmsFreightSaveReqVO createReqVO) {
        // 校验结算单号唯一
        if (freightMapper.selectByNo(createReqVO.getNo()) != null) {
            throw exception(TMS_FREIGHT_NO_DUPLICATE);
        }
        TmsFreightDO freight = BeanUtils.toBean(createReqVO, TmsFreightDO.class);
        if (freight.getStatus() == null) {
            freight.setStatus(STATUS_PENDING_AUDIT);
        }
        freightMapper.insert(freight);
        return freight.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFreight(TmsFreightSaveReqVO updateReqVO) {
        validateFreightExists(updateReqVO.getId());
        TmsFreightDO updateObj = BeanUtils.toBean(updateReqVO, TmsFreightDO.class);
        freightMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFreight(Long id) {
        validateFreightExists(id);
        freightMapper.deleteById(id);
    }

    @Override
    public TmsFreightDO getFreight(Long id) {
        return freightMapper.selectById(id);
    }

    @Override
    public PageResult<TmsFreightDO> getFreightPage(TmsFreightPageReqVO pageReqVO) {
        return freightMapper.selectPage(pageReqVO);
    }

    @Override
    public TmsFreightCalculateRespVO calculateFreight(TmsFreightCalculateReqVO reqVO) {
        // 1. 获取运单信息
        TmsShipmentDO shipment = shipmentMapper.selectById(reqVO.getShipmentId());
        if (shipment == null) {
            throw exception(TMS_SHIPMENT_NOT_EXISTS);
        }

        // 2. 根据计费方式确定计费数量
        BigDecimal billingQuantity = BigDecimal.ZERO;
        BigDecimal unitPrice = reqVO.getUnitPrice() != null ? reqVO.getUnitPrice() : BigDecimal.ZERO;

        switch (reqVO.getBillingMethod()) {
            case BILLING_BY_WEIGHT:
                billingQuantity = shipment.getTotalWeight() != null ? shipment.getTotalWeight() : BigDecimal.ZERO;
                break;
            case BILLING_BY_VOLUME:
                billingQuantity = shipment.getTotalVolume() != null ? shipment.getTotalVolume() : BigDecimal.ZERO;
                break;
            case BILLING_BY_PIECE:
                billingQuantity = shipment.getTotalQuantity() != null ? shipment.getTotalQuantity() : BigDecimal.ZERO;
                break;
            case BILLING_FLAT_RATE:
                // 整车一口价：计费数量为1，单价即总运费
                billingQuantity = BigDecimal.ONE;
                break;
            case BILLING_BY_MILEAGE:
                // 里程计费：使用运单已录入的运费金额（来源为外部里程系统或地图API估算）
                billingQuantity = shipment.getFreightAmount() != null ? shipment.getFreightAmount() : BigDecimal.ZERO;
                unitPrice = BigDecimal.ONE; // 单价1元/公里，billingQuantity即里程数
                break;
            default:
                throw exception(TMS_FREIGHT_CALC_FAIL, reqVO.getShipmentId(), "不支持的计费方式");
        }

        // 3. 计算运费
        BigDecimal baseAmount = billingQuantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
        BigDecimal surcharge = reqVO.getSurcharge() != null ? reqVO.getSurcharge() : BigDecimal.ZERO;
        BigDecimal discount = reqVO.getDiscountAmount() != null ? reqVO.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = baseAmount.add(surcharge).subtract(discount).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        // 4. 返回计算结果
        TmsFreightCalculateRespVO respVO = new TmsFreightCalculateRespVO();
        respVO.setBillingMethod(reqVO.getBillingMethod());
        respVO.setBillingQuantity(billingQuantity);
        respVO.setUnitPrice(unitPrice);
        respVO.setBaseAmount(baseAmount);
        respVO.setSurcharge(surcharge);
        respVO.setDiscountAmount(discount);
        respVO.setTotalAmount(totalAmount);
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditFreight(Long id, Boolean pass, String reason) {
        TmsFreightDO freight = validateFreightExists(id);
        // 只有待审核状态可审核
        if (!Objects.equals(freight.getStatus(), STATUS_PENDING_AUDIT)) {
            throw exception(TMS_FREIGHT_STATUS_INVALID, id);
        }
        TmsFreightDO updateObj = new TmsFreightDO();
        updateObj.setId(id);
        updateObj.setAuditor(""); // 由上层 SecurityFrameworkUtils 填入
        updateObj.setAuditTime(LocalDateTime.now());
        if (Boolean.TRUE.equals(pass)) {
            updateObj.setStatus(STATUS_AUDITED);
        } else {
            updateObj.setStatus(STATUS_REJECTED);
            updateObj.setRejectReason(reason);
        }
        freightMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleFreight(Long id) {
        TmsFreightDO freight = validateFreightExists(id);
        // 只有已审核状态可结算
        if (!Objects.equals(freight.getStatus(), STATUS_AUDITED)) {
            throw exception(TMS_FREIGHT_STATUS_INVALID, id);
        }
        // 校验运单已签收
        TmsShipmentDO shipment = shipmentMapper.selectById(freight.getShipmentId());
        if (shipment == null) {
            throw exception(TMS_SHIPMENT_NOT_EXISTS);
        }
        if (!Objects.equals(shipment.getStatus(), SHIPMENT_STATUS_SIGNED)) {
            throw exception(TMS_FREIGHT_SHIPMENT_NOT_SIGNED, freight.getShipmentId());
        }
        TmsFreightDO updateObj = new TmsFreightDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_SETTLED);
        updateObj.setSettleTime(LocalDateTime.now());
        freightMapper.updateById(updateObj);
    }

    private TmsFreightDO validateFreightExists(Long id) {
        TmsFreightDO freight = freightMapper.selectById(id);
        if (freight == null) {
            throw exception(TMS_FREIGHT_NOT_EXISTS);
        }
        return freight;
    }

}
