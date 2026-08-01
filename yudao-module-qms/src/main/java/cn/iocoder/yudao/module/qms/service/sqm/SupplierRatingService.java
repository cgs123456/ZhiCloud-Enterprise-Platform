package cn.iocoder.yudao.module.qms.service.sqm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.qms.controller.admin.sqm.vo.SupplierRatingPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.sqm.vo.SupplierRatingSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.sqm.SupplierRatingDO;
import cn.iocoder.yudao.module.qms.enums.qms.SupplierGradeEnum;
import jakarta.validation.Valid;

import java.math.BigDecimal;

/**
 * QMS 供应商评级 Service 接口
 *
 * @author yudao
 */
public interface SupplierRatingService {

    Long createSupplierRating(@Valid SupplierRatingSaveReqVO createReqVO);

    void updateSupplierRating(@Valid SupplierRatingSaveReqVO updateReqVO);

    void deleteSupplierRating(Long id);

    SupplierRatingDO getSupplierRating(Long id);

    PageResult<SupplierRatingDO> getSupplierRatingPage(SupplierRatingPageReqVO pageReqVO);

    /**
     * 根据指标计算供应商等级
     *
     * @param ppm         PPM 缺陷率（百万分之缺陷数）
     * @param onTimeRate  交期达成率（百分比）
     * @param qualityRate 质量合格率（百分比）
     * @return 供应商等级
     */
    SupplierGradeEnum calculateGrade(int ppm, BigDecimal onTimeRate, BigDecimal qualityRate);

}