package cn.zhicloud.module.qms.service.sqm;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.sqm.vo.SupplierRatingPageReqVO;
import cn.zhicloud.module.qms.controller.admin.sqm.vo.SupplierRatingSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.sqm.SupplierRatingDO;
import cn.zhicloud.module.qms.dal.mysql.sqm.SupplierRatingMapper;
import cn.zhicloud.module.qms.enums.qms.SupplierGradeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.SUPPLIER_RATING_NOT_EXISTS;

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
 * @author zhicloud
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
        // 等级无条件按指标计算，忽略调用方传入的 grade。
        // §12.235.5：此前「未指定才自动算」，于是可以写入一个与同行 ppm/准时率/质量率
        // 自相矛盾的等级（指标很差却 grade=A），评级结论失去可信度。
        supplierRating.setGrade(calculateGrade(supplierRating.getPpm(),
                supplierRating.getOnTimeRate(), supplierRating.getQualityRate()).getGrade());
        supplierRatingMapper.insert(supplierRating);
        return supplierRating.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSupplierRating(SupplierRatingSaveReqVO updateReqVO) {
        validateSupplierRatingExists(updateReqVO.getId());
        SupplierRatingDO updateObj = BeanUtils.toBean(updateReqVO, SupplierRatingDO.class);
        // 等级不接受外部写入（§12.236.7）：必须与同行指标一致，
        // 只能由 calculateGrade 依据 ppm/准时率/质量率算出。
        // updateById 跳过 null 字段，故置 null 即「无法被改写」。
        updateObj.setGrade(null);
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