package cn.iocoder.yudao.module.qms.service.inspectionorder;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.qms.controller.admin.inspectionrecord.vo.InspectionRecordSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.inspectionorder.InspectionOrderDO;
import cn.iocoder.yudao.module.qms.dal.dataobject.inspectionrecord.InspectionRecordDO;
import cn.iocoder.yudao.module.qms.dal.mysql.inspectionorder.InspectionOrderMapper;
import cn.iocoder.yudao.module.qms.dal.mysql.inspectionrecord.InspectionRecordMapper;
import cn.iocoder.yudao.module.qms.enums.qms.InspectionOrderStatusEnum;
import cn.iocoder.yudao.module.qms.enums.qms.InspectionResultEnum;
import cn.iocoder.yudao.module.qms.enums.qms.InspectionTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link InspectionOrderServiceImpl} 的单元测试
 *
 * @author 芋道源码
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
        // 准备数据：插入一条待检验状态的检验单
        InspectionOrderDO order = randomPojo(InspectionOrderDO.class, o -> {
            o.setStatus(InspectionOrderStatusEnum.PENDING.getStatus());
        });
        inspectionOrderMapper.insert(order);

        // 构造检验记录：3条中2条不合格（不合格率 66.7% > 50% 阈值）
        InspectionRecordSaveReqVO passRecord = new InspectionRecordSaveReqVO();
        passRecord.setOrderId(order.getId());
        passRecord.setItemId(randomLongId());
        passRecord.setResult(InspectionResultEnum.PASS.getResult());

        InspectionRecordSaveReqVO failRecord1 = new InspectionRecordSaveReqVO();
        failRecord1.setOrderId(order.getId());
        failRecord1.setItemId(randomLongId());
        failRecord1.setResult(InspectionResultEnum.FAIL.getResult());

        InspectionRecordSaveReqVO failRecord2 = new InspectionRecordSaveReqVO();
        failRecord2.setOrderId(order.getId());
        failRecord2.setItemId(randomLongId());
        failRecord2.setResult(InspectionResultEnum.FAIL.getResult());

        // 调用
        inspectionOrderService.submitInspection(order.getId(), List.of(passRecord, failRecord1, failRecord2));

        // 断言：不合格率 > 50%，检验单状态应为"检验不通过"
        InspectionOrderDO updatedOrder = inspectionOrderMapper.selectById(order.getId());
        assertEquals(InspectionOrderStatusEnum.FAILED.getStatus(), updatedOrder.getStatus());
    }

    @Test
    public void test_submitInspection_minorityFail() {
        // 准备数据：插入一条待检验状态的检验单
        InspectionOrderDO order = randomPojo(InspectionOrderDO.class, o -> {
            o.setStatus(InspectionOrderStatusEnum.PENDING.getStatus());
        });
        inspectionOrderMapper.insert(order);

        // 构造检验记录：3条中1条不合格（不合格率 33.3% < 50% 阈值）
        InspectionRecordSaveReqVO passRecord1 = new InspectionRecordSaveReqVO();
        passRecord1.setOrderId(order.getId());
        passRecord1.setItemId(randomLongId());
        passRecord1.setResult(InspectionResultEnum.PASS.getResult());

        InspectionRecordSaveReqVO passRecord2 = new InspectionRecordSaveReqVO();
        passRecord2.setOrderId(order.getId());
        passRecord2.setItemId(randomLongId());
        passRecord2.setResult(InspectionResultEnum.PASS.getResult());

        InspectionRecordSaveReqVO failRecord = new InspectionRecordSaveReqVO();
        failRecord.setOrderId(order.getId());
        failRecord.setItemId(randomLongId());
        failRecord.setResult(InspectionResultEnum.FAIL.getResult());

        // 调用
        inspectionOrderService.submitInspection(order.getId(), List.of(passRecord1, passRecord2, failRecord));

        // 断言：不合格率 < 50%，检验单状态应为"检验通过"
        InspectionOrderDO updatedOrder = inspectionOrderMapper.selectById(order.getId());
        assertEquals(InspectionOrderStatusEnum.PASSED.getStatus(), updatedOrder.getStatus());
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
        cn.iocoder.yudao.module.qms.controller.admin.inspectionorder.vo.InspectionOrderSaveReqVO createReqVO =
                new cn.iocoder.yudao.module.qms.controller.admin.inspectionorder.vo.InspectionOrderSaveReqVO();
        createReqVO.setOrderNo("QMS20240101001");
        createReqVO.setType(InspectionTypeEnum.IQC.getType());

        // 调用
        Long id = inspectionOrderService.createInspectionOrder(createReqVO);

        // 断言：默认状态为待检验
        InspectionOrderDO order = inspectionOrderMapper.selectById(id);
        assertNotNull(order);
        assertEquals(InspectionOrderStatusEnum.PENDING.getStatus(), order.getStatus());
    }

    @Test
    public void test_deleteInspectionOrder_notExists() {
        // 调用：删除不存在的检验单，应该抛异常
        assertThrows(Exception.class, () -> inspectionOrderService.deleteInspectionOrder(randomLongId()));
    }

}
