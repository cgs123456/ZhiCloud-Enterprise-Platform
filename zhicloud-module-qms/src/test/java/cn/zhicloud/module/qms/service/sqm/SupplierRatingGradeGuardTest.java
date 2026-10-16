package cn.zhicloud.module.qms.service.sqm;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.controller.admin.sqm.vo.SupplierRatingSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.sqm.SupplierRatingDO;
import cn.zhicloud.module.qms.dal.mysql.sqm.SupplierRatingMapper;
import cn.zhicloud.module.qms.enums.qms.SupplierGradeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import java.math.BigDecimal;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static org.junit.jupiter.api.Assertions.*;

/**
 * §12.235.5（critical）：供应商评级等级护栏测试。
 *
 * <p>攻击形态：指标很差（ppm=5000、准时率 60%、合格率 50%，按阈值本应为 D）
 * 却直接传 grade=A——评级结论与同行指标自相矛盾，供应商准入/淘汰决策失去可信度。
 * 旧实现「未指定才自动算」，即只要传了 grade 就照单全收。
 *
 * @author 智云
 */
@Import(SupplierRatingServiceImpl.class)
public class SupplierRatingGradeGuardTest extends BaseDbUnitTest {

    @Resource
    private SupplierRatingServiceImpl supplierRatingService;

    @Resource
    private SupplierRatingMapper supplierRatingMapper;

    @Test
    public void test_createSupplierRating_ignoresSuppliedGrade() {
        // 攻击形态：指标很差却传 grade=A（§12.235.5）
        SupplierRatingSaveReqVO req = new SupplierRatingSaveReqVO();
        req.setRatingNo("SR-BYPASS-GRADE-001");
        req.setSupplierId(randomLongId());
        req.setPpm(5000);
        req.setOnTimeRate(new BigDecimal("60.00"));
        req.setQualityRate(new BigDecimal("50.00"));
        req.setGrade(SupplierGradeEnum.A.getGrade());

        Long id = supplierRatingService.createSupplierRating(req);

        SupplierRatingDO saved = supplierRatingMapper.selectById(id);
        // 必须是指标算出来的 D，而不是调用方传的 A
        assertEquals(SupplierGradeEnum.D.getGrade(), saved.getGrade());
    }

    @Test
    public void test_createSupplierRating_recomputesGradeFromMetrics() {
        // 反向检查：指标优秀时传 D 也会被改回 A，证明是「无条件重算」而非「只挡 A」（§12.235.5）
        SupplierRatingSaveReqVO req = new SupplierRatingSaveReqVO();
        req.setRatingNo("SR-BYPASS-GRADE-002");
        req.setSupplierId(randomLongId());
        req.setPpm(50);
        req.setOnTimeRate(new BigDecimal("99.00"));
        req.setQualityRate(new BigDecimal("99.50"));
        req.setGrade(SupplierGradeEnum.D.getGrade());

        Long id = supplierRatingService.createSupplierRating(req);

        SupplierRatingDO saved = supplierRatingMapper.selectById(id);
        assertEquals(SupplierGradeEnum.A.getGrade(), saved.getGrade());
    }
}
