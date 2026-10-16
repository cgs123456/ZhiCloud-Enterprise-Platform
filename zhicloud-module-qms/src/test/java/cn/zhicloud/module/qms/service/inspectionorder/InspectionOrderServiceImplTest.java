package cn.zhicloud.module.qms.service.inspectionorder;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.controller.admin.inspectionorder.vo.InspectionOrderSaveReqVO;
import cn.zhicloud.module.qms.controller.admin.inspectionrecord.vo.InspectionRecordSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.inspectionorder.InspectionOrderDO;
import cn.zhicloud.module.qms.dal.dataobject.inspectionrecord.InspectionRecordDO;
import cn.zhicloud.module.qms.dal.mysql.inspectionorder.InspectionOrderMapper;
import cn.zhicloud.module.qms.dal.mysql.inspectionrecord.InspectionRecordMapper;
import cn.zhicloud.module.qms.enums.qms.InspectionOrderStatusEnum;
import cn.zhicloud.module.qms.enums.qms.InspectionResultEnum;
import cn.zhicloud.module.qms.enums.qms.InspectionSeverityEnum;
import cn.zhicloud.module.qms.enums.qms.InspectionTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import java.util.List;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link InspectionOrderServiceImpl} 的单元测试
 *
 * @author 智云
 */
@Import(InspectionOrderServiceImpl.class)
public class InspectionOrderServiceImplTest extends BaseDbUnitTest {

    @Resource
    private InspectionOrderServiceImpl inspectionOrderService;

    @Resource
    private InspectionOrderMapper inspectionOrderMapper;

    @Resource
    private InspectionRecordMapper inspectionRecordMapper;

    @Test
    public void test_submitInspection_allPass() {
        // 准备数据：插入一条待检验状态的检验单
        InspectionOrderDO order = randomPojo(InspectionOrderDO.class, o -> {
            o.setStatus(InspectionOrderStatusEnum.PENDING.getStatus());
        });
        inspectionOrderMapper.insert(order);

        // 构造检验记录：全部合格
        InspectionRecordSaveReqVO record1 = new InspectionRecordSaveReqVO();
        record1.setOrderId(order.getId());
        record1.setItemId(randomLongId());
        record1.setResult(InspectionResultEnum.PASS.getResult());
        record1.setMeasuredValue("10.01");

        InspectionRecordSaveReqVO record2 = new InspectionRecordSaveReqVO();
        record2.setOrderId(order.getId());
        record2.setItemId(randomLongId());
        record2.setResult(InspectionResultEnum.PASS.getResult());
        record2.setMeasuredValue("10.02");

        // 调用
        inspectionOrderService.submitInspection(order.getId(), List.of(record1, record2));

        // 断言：检验单状态应为"检验通过"
        InspectionOrderDO updatedOrder = inspectionOrderMapper.selectById(order.getId());
        assertEquals(InspectionOrderStatusEnum.PASSED.getStatus(), updatedOrder.getStatus());
        assertNotNull(updatedOrder.getInspectTime());

        // 断言：检验记录已插入
        List<InspectionRecordDO> records = inspectionRecordMapper.selectListByOrderId(order.getId());
        assertEquals(2, records.size());
    }

    @Test
    public void test_submitInspection_majorityFail() {
        // 准备数据：插入一条待检验状态的检验单（未配置 AQL，fail-closed）
        InspectionOrderDO order = randomPojo(InspectionOrderDO.class, o -> {
            o.setStatus(InspectionOrderStatusEnum.PENDING.getStatus());
            o.setAcceptanceQuantity(null); // 未配置 Ac：任何非致命缺陷即拒收
        });
        inspectionOrderMapper.insert(order);

        // 构造检验记录：3条中2条不合格（严重度 MAJOR）
        InspectionRecordSaveReqVO passRecord = new InspectionRecordSaveReqVO();
        passRecord.setOrderId(order.getId());
        passRecord.setItemId(randomLongId());
        passRecord.setResult(InspectionResultEnum.PASS.getResult());
        passRecord.setSeverity(InspectionSeverityEnum.MAJOR.getSeverity());

        InspectionRecordSaveReqVO failRecord1 = new InspectionRecordSaveReqVO();
        failRecord1.setOrderId(order.getId());
        failRecord1.setItemId(randomLongId());
        failRecord1.setResult(InspectionResultEnum.FAIL.getResult());
        failRecord1.setSeverity(InspectionSeverityEnum.MAJOR.getSeverity());

        InspectionRecordSaveReqVO failRecord2 = new InspectionRecordSaveReqVO();
        failRecord2.setOrderId(order.getId());
        failRecord2.setItemId(randomLongId());
        failRecord2.setResult(InspectionResultEnum.FAIL.getResult());
        failRecord2.setSeverity(InspectionSeverityEnum.MAJOR.getSeverity());

        // 调用
        inspectionOrderService.submitInspection(order.getId(), List.of(passRecord, failRecord1, failRecord2));

        // 断言：存在不合格记录且未配置 AQL，检验单状态应为"检验不通过"
        InspectionOrderDO updatedOrder = inspectionOrderMapper.selectById(order.getId());
        assertEquals(InspectionOrderStatusEnum.FAILED.getStatus(), updatedOrder.getStatus());
    }

    @Test
    public void test_submitInspection_minorityFail() {
        // 准备数据：插入一条待检验状态的检验单（配置 AQL：Ac=1, Re=2）
        InspectionOrderDO order = randomPojo(InspectionOrderDO.class, o -> {
            o.setStatus(InspectionOrderStatusEnum.PENDING.getStatus());
            o.setAcceptanceQuantity(1); // Ac=1：缺陷数 <= 1 判合格
            o.setRejectQuantity(2);     // Re=2
        });
        inspectionOrderMapper.insert(order);

        // 构造检验记录：3条中1条不合格（严重度 MINOR）
        InspectionRecordSaveReqVO passRecord1 = new InspectionRecordSaveReqVO();
        passRecord1.setOrderId(order.getId());
        passRecord1.setItemId(randomLongId());
        passRecord1.setResult(InspectionResultEnum.PASS.getResult());
        passRecord1.setSeverity(InspectionSeverityEnum.MINOR.getSeverity());

        InspectionRecordSaveReqVO passRecord2 = new InspectionRecordSaveReqVO();
        passRecord2.setOrderId(order.getId());
        passRecord2.setItemId(randomLongId());
        passRecord2.setResult(InspectionResultEnum.PASS.getResult());
        passRecord2.setSeverity(InspectionSeverityEnum.MINOR.getSeverity());

        InspectionRecordSaveReqVO failRecord = new InspectionRecordSaveReqVO();
        failRecord.setOrderId(order.getId());
        failRecord.setItemId(randomLongId());
        failRecord.setResult(InspectionResultEnum.FAIL.getResult());
        failRecord.setSeverity(InspectionSeverityEnum.MINOR.getSeverity());

        // 调用
        inspectionOrderService.submitInspection(order.getId(), List.of(passRecord1, passRecord2, failRecord));

        // 断言：缺陷数 1 <= Ac(1)，按 AQL 判合格
        InspectionOrderDO updatedOrder = inspectionOrderMapper.selectById(order.getId());
        assertEquals(InspectionOrderStatusEnum.PASSED.getStatus(), updatedOrder.getStatus());
    }

    @Test
    public void test_submitInspection_criticalVeto() {
        // 准备数据：插入一条待检验状态的检验单（配置宽松 AQL：Ac=5，单条缺陷本应合格）
        InspectionOrderDO order = randomPojo(InspectionOrderDO.class, o -> {
            o.setStatus(InspectionOrderStatusEnum.PENDING.getStatus());
            o.setAcceptanceQuantity(5); // Ac=5：按 AQL 本应合格
        });
        inspectionOrderMapper.insert(order);

        // 构造检验记录：仅 1 条致命缺陷（CRITICAL）
        InspectionRecordSaveReqVO criticalFail = new InspectionRecordSaveReqVO();
        criticalFail.setOrderId(order.getId());
        criticalFail.setItemId(randomLongId());
        criticalFail.setResult(InspectionResultEnum.FAIL.getResult());
        criticalFail.setSeverity(InspectionSeverityEnum.CRITICAL.getSeverity());

        // 调用
        inspectionOrderService.submitInspection(order.getId(), List.of(criticalFail));

        // 断言：致命缺陷一票否决，整单"检验不通过"（即使 AQL 宽松）
        InspectionOrderDO updatedOrder = inspectionOrderMapper.selectById(order.getId());
        assertEquals(InspectionOrderStatusEnum.FAILED.getStatus(), updatedOrder.getStatus());
    }

    @Test
    public void test_submitInspection_emptyRecords() {
        // 准备数据：插入一条待检验状态的检验单
        InspectionOrderDO order = randomPojo(InspectionOrderDO.class, o -> {
            o.setStatus(InspectionOrderStatusEnum.PENDING.getStatus());
        });
        inspectionOrderMapper.insert(order);

        // 调用：提交空检验记录
        inspectionOrderService.submitInspection(order.getId(), List.of());

        // 断言：无检验记录时，默认为"检验通过"
        InspectionOrderDO updatedOrder = inspectionOrderMapper.selectById(order.getId());
        assertEquals(InspectionOrderStatusEnum.PASSED.getStatus(), updatedOrder.getStatus());
    }

    @Test
    public void test_submitInspection_wrongStatus() {
        // 准备数据：插入一条已通过的检验单
        InspectionOrderDO order = randomPojo(InspectionOrderDO.class, o -> {
            o.setStatus(InspectionOrderStatusEnum.PASSED.getStatus());
        });
        inspectionOrderMapper.insert(order);

        // 调用：已通过的检验单不能再次提交，应该抛异常
        assertThrows(Exception.class, () ->
                inspectionOrderService.submitInspection(order.getId(), List.of()));
    }

    @Test
    public void test_submitInspection_overwritesOldRecords() {
        // 准备数据：插入一条检验中状态的检验单
        InspectionOrderDO order = randomPojo(InspectionOrderDO.class, o -> {
            o.setStatus(InspectionOrderStatusEnum.INSPECTING.getStatus());
        });
        inspectionOrderMapper.insert(order);

        // 插入旧的检验记录
        InspectionRecordDO oldRecord = randomPojo(InspectionRecordDO.class, o -> {
            o.setOrderId(order.getId());
        });
        inspectionRecordMapper.insert(oldRecord);

        // 构造新的检验记录
        InspectionRecordSaveReqVO newRecord = new InspectionRecordSaveReqVO();
        newRecord.setOrderId(order.getId());
        newRecord.setItemId(randomLongId());
        newRecord.setResult(InspectionResultEnum.PASS.getResult());

        // 调用：提交新检验记录
        inspectionOrderService.submitInspection(order.getId(), List.of(newRecord));

        // 断言：旧记录已删除，新记录已插入
        List<InspectionRecordDO> records = inspectionRecordMapper.selectListByOrderId(order.getId());
        assertEquals(1, records.size());
        assertEquals(newRecord.getItemId(), records.get(0).getItemId());
    }

    @Test
    public void test_createInspectionOrder_defaultStatus() {
        // 准备数据：不设置状态
        cn.zhicloud.module.qms.controller.admin.inspectionorder.vo.InspectionOrderSaveReqVO createReqVO =
                new cn.zhicloud.module.qms.controller.admin.inspectionorder.vo.InspectionOrderSaveReqVO();
        createReqVO.setOrderNo("QMS20240101001");
        createReqVO.setType(InspectionTypeEnum.IQC.getType());

        // 调用
        Long id = inspectionOrderService.createInspectionOrder(createReqVO);

        // 断言：默认状态为待检验
        InspectionOrderDO order = inspectionOrderMapper.selectById(id);
        assertNotNull(order);
        assertEquals(InspectionOrderStatusEnum.PENDING.getStatus(), order.getStatus());
    }

    // ------------------------------------------------------------------
    // §12.235.6：建单状态护栏（critical）
    //
    // inspection-order 的状态会被 WMS 入库质检卡点直接当作判据：
    //   WmsReceiptOrderServiceImpl#checkQualityGate -> InspectionOrderApi#isQualified
    //   -> order.status == PASSED(30) 即放行。
    // 所以「建单时能传 status」等价于「一次 POST 就能放行本应被拦截的收货」。
    // 这组用例钉住的是 create 忽略外部 status 这件事，以及灰 updated 路径的对称性。
    // ------------------------------------------------------------------

    @Test
    public void test_createInspectionOrder_ignoresSuppliedPassedStatus() {
        // 攻击形态：建单直接传 PASSED(30)
        InspectionOrderSaveReqVO createReqVO = new InspectionOrderSaveReqVO();
        createReqVO.setOrderNo("QMS-BYPASS-PASSED-001");
        createReqVO.setType(InspectionTypeEnum.IQC.getType());
        createReqVO.setBizType(cn.zhicloud.module.qms.enums.qms.InspectionBizTypeEnum.PURCHASE_IN.getBizType());
        createReqVO.setBizId(randomLongId());
        createReqVO.setStatus(InspectionOrderStatusEnum.PASSED.getStatus());

        Long id = inspectionOrderService.createInspectionOrder(createReqVO);

        InspectionOrderDO order = inspectionOrderMapper.selectById(id);
        // 必须仍是待检验：外部传入的终态不得生效
        assertEquals(InspectionOrderStatusEnum.PENDING.getStatus(), order.getStatus());
        // 业务关联要落库：这正是 isQualified 的查找键
        assertNotNull(order.getBizId());
    }

    @Test
    public void test_createInspectionOrder_ignoresSuppliedFailedStatus() {
        // 对称检查：不止 PASSED，任何非 PENDING 状态都不生效
        InspectionOrderSaveReqVO createReqVO = new InspectionOrderSaveReqVO();
        createReqVO.setOrderNo("QMS-BYPASS-FAILED-001");
        createReqVO.setType(InspectionTypeEnum.IQC.getType());
        createReqVO.setStatus(InspectionOrderStatusEnum.FAILED.getStatus());

        Long id = inspectionOrderService.createInspectionOrder(createReqVO);

        InspectionOrderDO order = inspectionOrderMapper.selectById(id);
        assertEquals(InspectionOrderStatusEnum.PENDING.getStatus(), order.getStatus());
    }

    @Test
    public void test_createInspectionOrder_ignoresInspectingStatus() {
        InspectionOrderSaveReqVO createReqVO = new InspectionOrderSaveReqVO();
        createReqVO.setOrderNo("QMS-BYPASS-INSPECTING-001");
        createReqVO.setType(InspectionTypeEnum.IQC.getType());
        createReqVO.setStatus(InspectionOrderStatusEnum.INSPECTING.getStatus());

        Long id = inspectionOrderService.createInspectionOrder(createReqVO);

        InspectionOrderDO order = inspectionOrderMapper.selectById(id);
        assertEquals(InspectionOrderStatusEnum.PENDING.getStatus(), order.getStatus());
    }

    @Test
    public void test_updateInspectionOrder_ignoresSuppliedStatus() {
        // 修改路径此前已有护栏（setStatus(null)），这里钉住它不被改掉，
        // 与 create 的新护栏构成对称的两半。
        InspectionOrderDO order = randomPojo(InspectionOrderDO.class, o -> {
            o.setStatus(InspectionOrderStatusEnum.PENDING.getStatus());
        });
        inspectionOrderMapper.insert(order);

        InspectionOrderSaveReqVO updateReqVO = new InspectionOrderSaveReqVO();
        updateReqVO.setId(order.getId());
        updateReqVO.setOrderNo(order.getOrderNo());
        updateReqVO.setType(order.getType());
        updateReqVO.setStatus(InspectionOrderStatusEnum.PASSED.getStatus());

        inspectionOrderService.updateInspectionOrder(updateReqVO);

        InspectionOrderDO updated = inspectionOrderMapper.selectById(order.getId());
        assertEquals(InspectionOrderStatusEnum.PENDING.getStatus(), updated.getStatus());
    }

    @Test
    public void test_deleteInspectionOrder_notExists() {
        // 调用：删除不存在的检验单，应该抛异常
        assertThrows(Exception.class, () -> inspectionOrderService.deleteInspectionOrder(randomLongId()));
    }

}
