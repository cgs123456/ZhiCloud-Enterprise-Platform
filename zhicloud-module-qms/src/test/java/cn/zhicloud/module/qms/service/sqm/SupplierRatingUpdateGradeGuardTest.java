package cn.zhicloud.module.qms.service.sqm;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.controller.admin.sqm.vo.SupplierRatingSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.sqm.SupplierRatingDO;
import cn.zhicloud.module.qms.dal.mysql.sqm.SupplierRatingMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * §12.236.7：{@link SupplierRatingServiceImpl} **修改**路径的等级护栏测试。
 *
 * <p>等级必须与同行指标（ppm / 准时率 / 质量率）一致，只能由 calculateGrade 算出。
 * 若允许通用修改直接写 grade，就能得到「指标很差却等级 A」这种自相矛盾的评级。
 *
 * <p><b>断言语义</b>：MyBatis-Plus 的 updateById 跳过 null 字段，
 * 故服务层 setGrade(null) 的效果是「该列不进 UPDATE」= 保持原值。
 * 因此断言「原值未被改写」，而不是「变成 null」。
 *
 * @author 智云
 */
@Import(SupplierRatingServiceImpl.class)
public class SupplierRatingUpdateGradeGuardTest extends BaseDbUnitTest {

    @Resource
    private SupplierRatingServiceImpl service;

    @Resource
    private SupplierRatingMapper mapper;

    @Test
    public void test_updateSupplierRating_ignoresSuppliedGrade() {
        // 不能裸用 randomPojo：曾撞到 "Value too long for column on_time_rate DECIMAL(6,2)"。
        // 这里只钉 grade 这一个字段，指标列给一组真实量程的值。
        SupplierRatingDO existing = randomPojo(SupplierRatingDO.class, o -> {
            o.setGrade("C");
            o.setPpm(120);
            // 这两个是 BigDecimal 且 DB 列宽只有 DECIMAL(6,2)，
            // randomPojo 会生成 33208.61 这种超长值（实测报错），必须给真实量程
            o.setOnTimeRate(new java.math.BigDecimal("95.00"));
            o.setQualityRate(new java.math.BigDecimal("97.00"));
        });
        mapper.insert(existing);

        SupplierRatingSaveReqVO req = new SupplierRatingSaveReqVO();
        req.setId(existing.getId());
        req.setGrade("A"); // 与指标无关的"刷评级"

        service.updateSupplierRating(req);

        SupplierRatingDO after = mapper.selectById(existing.getId());
        assertEquals("C", after.getGrade(),
                "等级不接受外部写入：只能由 calculateGrade 依据指标算出（§12.236.7）");
    }
}
