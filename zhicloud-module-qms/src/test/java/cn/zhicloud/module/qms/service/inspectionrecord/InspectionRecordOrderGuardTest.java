package cn.zhicloud.module.qms.service.inspectionrecord;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.controller.admin.inspectionrecord.vo.InspectionRecordSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.inspectionorder.InspectionOrderDO;
import cn.zhicloud.module.qms.dal.dataobject.inspectionrecord.InspectionRecordDO;
import cn.zhicloud.module.qms.dal.mysql.inspectionorder.InspectionOrderMapper;
import cn.zhicloud.module.qms.dal.mysql.inspectionrecord.InspectionRecordMapper;
import cn.zhicloud.module.qms.enums.qms.InspectionOrderStatusEnum;
import cn.zhicloud.module.qms.enums.qms.InspectionResultEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.INSPECTION_ORDER_NOT_EXISTS;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.INSPECTION_RECORD_ORDER_NOT_EDITABLE;
import static org.junit.jupiter.api.Assertions.*;

/**
 * §12.238：检验记录父单可写护栏测试。
 *
 * <p>攻击形态：给已判定（PASSED/FAILED）的检验单补/改/删明细，或给不存在的 orderId
 * 写入孤儿明细 —— 父单状态是 WMS 入库质检卡点（isQualified）的唯一判据。
 *
 * @author 智云
 */
@Import(InspectionRecordServiceImpl.class)
public class InspectionRecordOrderGuardTest extends BaseDbUnitTest {

    @Resource
    private InspectionRecordServiceImpl inspectionRecordService;

    @Resource
    private InspectionRecordMapper inspectionRecordMapper;

    @Resource
    private InspectionOrderMapper inspectionOrderMapper;

    private InspectionOrderDO insertOrder(Integer status) {
        InspectionOrderDO order = randomPojo(InspectionOrderDO.class, o -> o.setStatus(status));
        inspectionOrderMapper.insert(order);
        return order;
    }

    private InspectionRecordDO insertRecord(Long orderId, String measuredValue) {
        InspectionRecordDO record = randomPojo(InspectionRecordDO.class, o -> {
            o.setOrderId(orderId);
            o.setMeasuredValue(measuredValue);
        });
        inspectionRecordMapper.insert(record);
        return record;
    }

    private InspectionRecordSaveReqVO buildReq(Long orderId) {
        InspectionRecordSaveReqVO req = new InspectionRecordSaveReqVO();
        req.setOrderId(orderId);
        req.setItemId(randomLongId());
        req.setResult(InspectionResultEnum.PASS.getResult());
        return req;
    }

    @Test
    public void test_createRecord_okOnPendingOrder() {
        InspectionOrderDO order = insertOrder(InspectionOrderStatusEnum.PENDING.getStatus());

        Long id = inspectionRecordService.createInspectionRecord(buildReq(order.getId()));

        assertNotNull(id);
        assertEquals(order.getId(), inspectionRecordMapper.selectById(id).getOrderId());
    }

    @Test
    public void test_createRecord_okOnInspectingOrder() {
        InspectionOrderDO order = insertOrder(InspectionOrderStatusEnum.INSPECTING.getStatus());

        Long id = inspectionRecordService.createInspectionRecord(buildReq(order.getId()));

        assertNotNull(id);
    }

    @Test
    public void test_createRecord_rejectedOnPassedOrder() {
        InspectionOrderDO order = insertOrder(InspectionOrderStatusEnum.PASSED.getStatus());

        assertServiceException(() -> inspectionRecordService.createInspectionRecord(buildReq(order.getId())),
                INSPECTION_RECORD_ORDER_NOT_EDITABLE);
        assertTrue(inspectionRecordMapper.selectListByOrderId(order.getId()).isEmpty());
    }

    @Test
    public void test_createRecord_rejectedOnFailedOrder() {
        InspectionOrderDO order = insertOrder(InspectionOrderStatusEnum.FAILED.getStatus());

        assertServiceException(() -> inspectionRecordService.createInspectionRecord(buildReq(order.getId())),
                INSPECTION_RECORD_ORDER_NOT_EDITABLE);
        assertTrue(inspectionRecordMapper.selectListByOrderId(order.getId()).isEmpty());
    }

    @Test
    public void test_createRecord_rejectedOnMissingOrder() {
        // 孤儿明细：orderId 不存在也必须拒绝（此前可直接落库）
        assertServiceException(() -> inspectionRecordService.createInspectionRecord(buildReq(randomLongId())),
                INSPECTION_ORDER_NOT_EXISTS);
    }

    @Test
    public void test_updateRecord_rejectedOnTerminalOrder() {
        InspectionOrderDO order = insertOrder(InspectionOrderStatusEnum.PASSED.getStatus());
        InspectionRecordDO record = insertRecord(order.getId(), "ORIG");
        InspectionRecordSaveReqVO req = buildReq(order.getId());
        req.setId(record.getId());
        req.setMeasuredValue("HACKED");

        assertServiceException(() -> inspectionRecordService.updateInspectionRecord(req),
                INSPECTION_RECORD_ORDER_NOT_EDITABLE);
        assertEquals("ORIG", inspectionRecordMapper.selectById(record.getId()).getMeasuredValue());
    }

    @Test
    public void test_deleteRecord_rejectedOnTerminalOrder() {
        InspectionOrderDO order = insertOrder(InspectionOrderStatusEnum.FAILED.getStatus());
        InspectionRecordDO record = insertRecord(order.getId(), "ORIG");

        assertServiceException(() -> inspectionRecordService.deleteInspectionRecord(record.getId()),
                INSPECTION_RECORD_ORDER_NOT_EDITABLE);
        assertNotNull(inspectionRecordMapper.selectById(record.getId()));
    }

    @Test
    public void test_updateRecord_moveToTerminalOrder_rejected() {
        InspectionOrderDO openOrder = insertOrder(InspectionOrderStatusEnum.PENDING.getStatus());
        InspectionOrderDO terminalOrder = insertOrder(InspectionOrderStatusEnum.PASSED.getStatus());
        InspectionRecordDO record = insertRecord(openOrder.getId(), "ORIG");
        InspectionRecordSaveReqVO req = buildReq(terminalOrder.getId());
        req.setId(record.getId());

        // 把待检验单下的明细「搬进」终态单也必须被拒（否则等于给终态单补明细）
        assertServiceException(() -> inspectionRecordService.updateInspectionRecord(req),
                INSPECTION_RECORD_ORDER_NOT_EDITABLE);
        assertEquals(openOrder.getId(), inspectionRecordMapper.selectById(record.getId()).getOrderId());
    }

    @Test
    public void test_updateRecord_moveBetweenOpenOrders_ok() {
        InspectionOrderDO fromOrder = insertOrder(InspectionOrderStatusEnum.PENDING.getStatus());
        InspectionOrderDO toOrder = insertOrder(InspectionOrderStatusEnum.INSPECTING.getStatus());
        InspectionRecordDO record = insertRecord(fromOrder.getId(), "ORIG");
        InspectionRecordSaveReqVO req = buildReq(toOrder.getId());
        req.setId(record.getId());

        inspectionRecordService.updateInspectionRecord(req);

        assertEquals(toOrder.getId(), inspectionRecordMapper.selectById(record.getId()).getOrderId());
    }

    @Test
    public void test_deleteRecord_okOnPendingOrder() {
        InspectionOrderDO order = insertOrder(InspectionOrderStatusEnum.PENDING.getStatus());
        InspectionRecordDO record = insertRecord(order.getId(), "ORIG");

        inspectionRecordService.deleteInspectionRecord(record.getId());

        assertNull(inspectionRecordMapper.selectById(record.getId()));
    }
}
