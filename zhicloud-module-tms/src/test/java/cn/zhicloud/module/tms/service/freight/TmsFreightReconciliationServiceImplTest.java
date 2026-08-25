package cn.zhicloud.module.tms.service.freight;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.tms.controller.admin.freight.vo.TmsFreightReconciliationPageReqVO;
import cn.zhicloud.module.tms.controller.admin.freight.vo.TmsFreightReconciliationSaveReqVO;
import cn.zhicloud.module.tms.dal.dataobject.freight.TmsFreightReconciliationDO;
import cn.zhicloud.module.tms.dal.mysql.freight.TmsFreightReconciliationMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link TmsFreightReconciliationServiceImpl} 的单元测试
 *
 * @author 智云
 */
@Import(TmsFreightReconciliationServiceImpl.class)
public class TmsFreightReconciliationServiceImplTest extends BaseDbUnitTest {

    @Resource
    private TmsFreightReconciliationServiceImpl reconciliationService;

    @Resource
    private TmsFreightReconciliationMapper reconciliationMapper;

    @Test
    public void test_createReconciliation_success() {
        // 准备参数
        TmsFreightReconciliationSaveReqVO reqVO = new TmsFreightReconciliationSaveReqVO();
        reqVO.setNo("FR001");
        reqVO.setCarrierId(1L);
        reqVO.setPeriodStart(LocalDate.of(2026, 8, 1));
        reqVO.setPeriodEnd(LocalDate.of(2026, 8, 31));
        reqVO.setSystemAmount(new BigDecimal("15000.00"));
        reqVO.setCarrierAmount(new BigDecimal("15200.00"));

        // 调用
        Long id = reconciliationService.createReconciliation(reqVO);

        // 校验
        TmsFreightReconciliationDO result = reconciliationMapper.selectById(id);
        assertNotNull(result);
        assertEquals("FR001", result.getNo());
        assertEquals(0, result.getStatus()); // 待对账
    }

    @Test
    public void test_createReconciliation_duplicateNo() {
        // mock 数据
        TmsFreightReconciliationDO existing = randomPojo(TmsFreightReconciliationDO.class, o -> o.setNo("FR002"));
        reconciliationMapper.insert(existing);

        // 准备参数
        TmsFreightReconciliationSaveReqVO reqVO = new TmsFreightReconciliationSaveReqVO();
        reqVO.setNo("FR002");
        reqVO.setCarrierId(1L);
        reqVO.setPeriodStart(LocalDate.of(2026, 8, 1));
        reqVO.setPeriodEnd(LocalDate.of(2026, 8, 31));

        // 调用并校验异常
        assertThrows(Exception.class, () -> reconciliationService.createReconciliation(reqVO));
    }

    @Test
    public void test_doReconcile_noDiff() {
        // mock 数据（系统金额 = 承运商金额）
        TmsFreightReconciliationDO reconciliation = randomPojo(TmsFreightReconciliationDO.class, o -> {
            o.setStatus(0);
            o.setSystemAmount(new BigDecimal("10000.00"));
            o.setCarrierAmount(new BigDecimal("10000.00"));
        });
        reconciliationMapper.insert(reconciliation);

        // 调用
        reconciliationService.doReconcile(reconciliation.getId());

        // 校验：差异为 0，状态 → 已对账(10)
        TmsFreightReconciliationDO result = reconciliationMapper.selectById(reconciliation.getId());
        assertEquals(0, result.getDiffAmount().compareTo(BigDecimal.ZERO));
        assertEquals(10, result.getStatus());
    }

    @Test
    public void test_doReconcile_hasDiff() {
        // mock 数据（承运商 > 系统）
        TmsFreightReconciliationDO reconciliation = randomPojo(TmsFreightReconciliationDO.class, o -> {
            o.setStatus(0);
            o.setSystemAmount(new BigDecimal("10000.00"));
            o.setCarrierAmount(new BigDecimal("10500.00"));
        });
        reconciliationMapper.insert(reconciliation);

        // 调用
        reconciliationService.doReconcile(reconciliation.getId());

        // 校验：差异 500，状态 → 有差异(20)
        TmsFreightReconciliationDO result = reconciliationMapper.selectById(reconciliation.getId());
        assertEquals(0, result.getDiffAmount().compareTo(new BigDecimal("500.00")));
        assertEquals(20, result.getStatus());
    }

    @Test
    public void test_confirmReconciliation_success() {
        // mock 数据（已对账状态）
        TmsFreightReconciliationDO reconciliation = randomPojo(TmsFreightReconciliationDO.class, o -> {
            o.setStatus(10);
        });
        reconciliationMapper.insert(reconciliation);

        // 调用
        reconciliationService.confirmReconciliation(reconciliation.getId());

        // 校验：状态 → 已确认(30)
        TmsFreightReconciliationDO result = reconciliationMapper.selectById(reconciliation.getId());
        assertEquals(30, result.getStatus());
    }

    @Test
    public void test_confirmReconciliation_notReconciled() {
        // mock 数据（待对账状态）
        TmsFreightReconciliationDO reconciliation = randomPojo(TmsFreightReconciliationDO.class, o -> {
            o.setStatus(0);
        });
        reconciliationMapper.insert(reconciliation);

        // 调用并校验异常
        assertThrows(Exception.class, () -> reconciliationService.confirmReconciliation(reconciliation.getId()));
    }

    @Test
    public void test_rejectReconciliation_success() {
        // mock 数据
        TmsFreightReconciliationDO reconciliation = randomPojo(TmsFreightReconciliationDO.class, o -> {
            o.setStatus(20);
        });
        reconciliationMapper.insert(reconciliation);

        // 调用
        reconciliationService.rejectReconciliation(reconciliation.getId(), "金额不符");

        // 校验：状态 → 已驳回(40)
        TmsFreightReconciliationDO result = reconciliationMapper.selectById(reconciliation.getId());
        assertEquals(40, result.getStatus());
        assertEquals("金额不符", result.getRemark());
    }

    @Test
    public void test_deleteReconciliation_success() {
        // mock 数据
        TmsFreightReconciliationDO reconciliation = randomPojo(TmsFreightReconciliationDO.class);
        reconciliationMapper.insert(reconciliation);

        // 调用
        reconciliationService.deleteReconciliation(reconciliation.getId());

        // 校验
        assertNull(reconciliationMapper.selectById(reconciliation.getId()));
    }

    @Test
    public void test_deleteReconciliation_notExists() {
        assertThrows(Exception.class, () -> reconciliationService.deleteReconciliation(randomLongId()));
    }

}
