package cn.iocoder.yudao.module.qms.service.sqm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.sqm.vo.SupplierRatingPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.sqm.vo.SupplierRatingSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.sqm.SupplierRatingDO;
import cn.iocoder.yudao.module.qms.dal.mysql.sqm.SupplierRatingMapper;
import cn.iocoder.yudao.module.qms.enums.qms.SupplierGradeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.SUPPLIER_RATING_NOT_EXISTS;

/**
 * QMS 供应商评级 Service 实现类
 *
 * <p>评级规则（基于 PPM / 交期达成率 / 质量合格率综合判定）：
 * <ul>
 *   <li>A 级（优秀）：PPM ≤ 100 且 交期达成率 ≥ 98% 且 质量合格率 ≥ 99%</li>
 *   <li>B 级（合格）：PPM ≤ 500 且 交期达成率 ≥ 95% 且 质量合格率 ≥ 97%</li>
 *   <li>C 级（待改进）：PPM ≤ 1000 且 交期达成率 ≥ 90%</li>
 *   <li>D 级（不合格）：其余情况</li>
 * </ul>
 *
 * @author yudao
 */
@Service
@Validated
public class SupplierRatingServiceImpl implements SupplierRatingService {

    @Resource
    private SupplierRatingMapper supplierRatingMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSupplierRating(SupplierRatingSaveReqVO createReqVO) {
        SupplierRatingDO supplierRating = BeanUtils.toBean(createReqVO, SupplierRatingDO.class);
        // 若未指定等级，则按指标自动计算
        if (supplierRating.getGrade() == null) {
            supplierRating.setGrade(calculateGrade(supplierRating.getPpm(),
                    supplierRating.getOnTimeRate(), supplierRating.getQualityRate()).getGrade());
        }
        supplierRatingMapper.insert(supplierRating);
        return supplierRating.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSupplierRating(SupplierRatingSaveReqVO updateReqVO) {
        validateSupplierRatingExists(updateReqVO.getId());
        SupplierRatingDO updateObj = BeanUtils.toBean(updateReqVO, SupplierRatingDO.class);
        supplierRatingMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSupplierRating(Long id) {
        validateSupplierRatingExists(id);
        supplierRatingMapper.deleteById(id);
    }

    private void validateSupplierRatingExists(Long id) {
        if (supplierRatingMapper.selectById(id) == null) {
            throw exception(SUPPLIER_RATING_NOT_EXISTS);
        }
    }

    @Override
    public SupplierRatingDO getSupplierRating(Long id) {
        return supplierRatingMapper.selectById(id);
    }

    @Override
    public PageResult<SupplierRatingDO> getSupplierRatingPage(SupplierRatingPageReqVO pageReqVO) {
        return supplierRatingMapper.selectPage(pageReqVO);
    }

    @Override
    public SupplierGradeEnum calculateGrade(int ppm, BigDecimal onTimeRate, BigDecimal qualityRate) {
        // 评级阈值
        BigDecimal onTimeA = new BigDecimal("98");
        BigDecimal onTimeB = new BigDecimal("95");
        BigDecimal onTimeC = new BigDecimal("90");
        BigDecimal qualityA = new BigDecimal("99");
        BigDecimal qualityB = new BigDecimal("97");

        // A 级
        if (ppm <= 100 && ge(onTimeRate, onTimeA) && ge(qualityRate, qualityA)) {
            return SupplierGradeEnum.A;
        }
        // B 级
        if (ppm <= 500 && ge(onTimeRate, onTimeB) && ge(qualityRate, qualityB)) {
            return SupplierGradeEnum.B;
        }
        // C 级
        if (ppm <= 1000 && ge(onTimeRate, onTimeC)) {
            return SupplierGradeEnum.C;
        }
        // D 级
        return SupplierGradeEnum.D;
    }

    /**
     * 判断 rate 是否大于等于阈值（rate 为空时视为不满足）
     */
    private boolean ge(BigDecimal rate, BigDecimal threshold) {
        return rate != null && rate.compareTo(threshold) >= 0;
    }

}