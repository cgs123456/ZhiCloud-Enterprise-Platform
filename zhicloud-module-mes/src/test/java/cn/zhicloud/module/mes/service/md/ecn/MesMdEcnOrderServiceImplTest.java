package cn.zhicloud.module.mes.service.md.ecn;

import cn.zhicloud.framework.common.enums.CommonStatusEnum;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.mes.controller.admin.md.bom.vo.MesBomDetailSaveReqVO;
import cn.zhicloud.module.mes.controller.admin.md.bom.vo.MesBomSaveReqVO;
import cn.zhicloud.module.mes.controller.admin.md.ecn.vo.MesMdEcnOrderItemSaveReqVO;
import cn.zhicloud.module.mes.controller.admin.md.ecn.vo.MesMdEcnOrderPageReqVO;
import cn.zhicloud.module.mes.controller.admin.md.ecn.vo.MesMdEcnOrderSaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.md.bom.MesBomDO;
import cn.zhicloud.module.mes.dal.dataobject.md.bom.MesBomDetailDO;
import cn.zhicloud.module.mes.dal.dataobject.md.ecn.MesMdEcnOrderDO;
import cn.zhicloud.module.mes.dal.dataobject.md.ecn.MesMdEcnOrderItemDO;
import cn.zhicloud.module.mes.dal.mysql.md.ecn.MesMdEcnOrderItemMapper;
import cn.zhicloud.module.mes.dal.mysql.md.ecn.MesMdEcnOrderMapper;
import cn.zhicloud.module.mes.service.md.bom.MesBomDetailService;
import cn.zhicloud.module.mes.service.md.bom.MesBomService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link MesMdEcnOrderServiceImpl} 的单元测试（mock mapper，无需 H2 表）
 */
@Import(MesMdEcnOrderServiceImpl.class)
public class MesMdEcnOrderServiceImplTest extends BaseDbUnitTest {

    private static final int STATUS_DRAFT = 10;
    private static final int STATUS_APPROVING = 20;
    private static final int STATUS_APPROVED = 30;
    private static final int STATUS_REJECTED = 40;
    private static final int STATUS_EXECUTED = 50;

    @MockitoBean
    private MesMdEcnOrderMapper ecnOrderMapper;
    @MockitoBean
    private MesMdEcnOrderItemMapper ecnOrderItemMapper;
    @MockitoBean
    private MesBomService bomService;
    @MockitoBean
    private MesBomDetailService bomDetailService;

    @Resource
    private MesMdEcnOrderServiceImpl ecnOrderService;

    @BeforeEach
    public void setUp() {
        // insert 时回填主键
        doAnswer(inv -> {
            MesMdEcnOrderDO ecnOrder = inv.getArgument(0);
            ecnOrder.setId(ecnOrder.getId() == null ? 100L : ecnOrder.getId());
            return 1;
        }).when(ecnOrderMapper).insert(any(MesMdEcnOrderDO.class));
        when(ecnOrderMapper.updateById(any(MesMdEcnOrderDO.class))).thenReturn(1);
        when(ecnOrderMapper.deleteById(anyLong())).thenReturn(1);
        when(ecnOrderItemMapper.insert(any(MesMdEcnOrderItemDO.class))).thenReturn(1);
        when(ecnOrderItemMapper.deleteByEcnOrderId(anyLong())).thenReturn(1);
    }

    private MesMdEcnOrderDO buildEcnOrder(Integer status) {
        return new MesMdEcnOrderDO().setId(100L).setNo("ECN001").setEcnName("X 产品 BOM 变更")
                .setChangeType(10).setStatus(status).setApplicantUserId(1L);
    }

    private MesMdEcnOrderSaveReqVO buildSaveReq() {
        return new MesMdEcnOrderSaveReqVO().setNo("ECN001").setEcnName("X 产品 BOM 变更")
                .setChangeType(10).setApplicantUserId(1L);
    }

    private MesMdEcnOrderItemSaveReqVO buildItemSaveReq() {
        return new MesMdEcnOrderItemSaveReqVO().setChangeItem(10).setOldValue("M001")
                .setNewValue("M002").setBomDetailId(300L);
    }

    private MesBomDO buildBom(Long id, Integer status) {
        return new MesBomDO().setId(id).setBomNo("BOM" + id).setProductId(1L)
                .setBomType("MANUFACTURING").setVersion("V1.0").setStatus(status);
    }

    private MesBomDetailDO buildBomDetail() {
        return new MesBomDetailDO().setId(300L).setBomId(200L).setProductId(5L)
                .setQuantity(new BigDecimal("2.00")).setUnit("个");
    }

    // ========== createEcnOrder ==========

    @Test
    public void testCreateEcnOrder_success() {
        Long id = ecnOrderService.createEcnOrder(buildSaveReq());
        assertEquals(100L, id);
        ArgumentCaptor<MesMdEcnOrderDO> captor = ArgumentCaptor.forClass(MesMdEcnOrderDO.class);
        verify(ecnOrderMapper).insert(captor.capture());
        assertEquals(STATUS_DRAFT, captor.getValue().getStatus());
        verify(ecnOrderItemMapper, never()).insert(any(MesMdEcnOrderItemDO.class));
    }

    @Test
    public void testCreateEcnOrder_withItems() {
        MesMdEcnOrderSaveReqVO reqVO = buildSaveReq()
                .setItems(Arrays.asList(buildItemSaveReq(), buildItemSaveReq().setChangeItem(20)));
        assertEquals(100L, ecnOrderService.createEcnOrder(reqVO));
        verify(ecnOrderItemMapper, times(2)).insert(any(MesMdEcnOrderItemDO.class));
    }

    @Test
    public void testCreateEcnOrder_noNull() {
        // no 为空 + changeType 为空，跳过唯一校验与 BOM 关系校验
        MesMdEcnOrderSaveReqVO reqVO = buildSaveReq().setNo(null).setChangeType(null);
        assertEquals(100L, ecnOrderService.createEcnOrder(reqVO));
        verify(ecnOrderMapper, never()).selectByNo(anyString());
    }

    @Test
    public void testCreateEcnOrder_noDuplicate() {
        when(ecnOrderMapper.selectByNo("ECN001")).thenReturn(buildEcnOrder(STATUS_DRAFT));
        assertServiceException(() -> ecnOrderService.createEcnOrder(buildSaveReq()),
                MD_ECN_ORDER_NO_DUPLICATE, "ECN001");
    }

    @Test
    public void testCreateEcnOrder_bomRequired() {
        MesMdEcnOrderSaveReqVO reqVO = buildSaveReq().setChangeType(20).setBomId(null);
        assertServiceException(() -> ecnOrderService.createEcnOrder(reqVO), MD_ECN_ORDER_BOM_REQUIRED);
    }

    @Test
    public void testCreateEcnOrder_deleteBomTypeWithBomId() {
        MesMdEcnOrderSaveReqVO reqVO = buildSaveReq().setChangeType(30).setBomId(200L);
        assertEquals(100L, ecnOrderService.createEcnOrder(reqVO));
        verify(ecnOrderMapper).insert(any(MesMdEcnOrderDO.class));
    }

    @Test
    public void testCreateEcnOrder_replaceItemTypeWithoutBomId() {
        MesMdEcnOrderSaveReqVO reqVO = buildSaveReq().setChangeType(40);
        assertServiceException(() -> ecnOrderService.createEcnOrder(reqVO), MD_ECN_ORDER_BOM_REQUIRED);
    }

    // ========== updateEcnOrder ==========

    @Test
    public void testUpdateEcnOrder_success() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(buildEcnOrder(STATUS_DRAFT));
        MesMdEcnOrderSaveReqVO reqVO = buildSaveReq().setId(100L).setEcnName("变更名称 V2")
                .setItems(Arrays.asList(buildItemSaveReq()));
        ecnOrderService.updateEcnOrder(reqVO);
        verify(ecnOrderMapper).updateById(any(MesMdEcnOrderDO.class));
        verify(ecnOrderItemMapper).deleteByEcnOrderId(100L);
        verify(ecnOrderItemMapper).insert(any(MesMdEcnOrderItemDO.class));
    }

    @Test
    public void testUpdateEcnOrder_sameNoSelf() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(buildEcnOrder(STATUS_DRAFT));
        when(ecnOrderMapper.selectByNo("ECN001")).thenReturn(buildEcnOrder(STATUS_DRAFT));
        ecnOrderService.updateEcnOrder(buildSaveReq().setId(100L));
        verify(ecnOrderMapper).updateById(any(MesMdEcnOrderDO.class));
    }

    @Test
    public void testUpdateEcnOrder_notExists() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> ecnOrderService.updateEcnOrder(buildSaveReq().setId(100L)),
                MD_ECN_ORDER_NOT_EXISTS);
    }

    @Test
    public void testUpdateEcnOrder_statusInvalid() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(buildEcnOrder(STATUS_APPROVING));
        assertServiceException(() -> ecnOrderService.updateEcnOrder(buildSaveReq().setId(100L)),
                MD_ECN_ORDER_STATUS_INVALID);
    }

    @Test
    public void testUpdateEcnOrder_noDuplicate() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(buildEcnOrder(STATUS_DRAFT));
        when(ecnOrderMapper.selectByNo("ECN001")).thenReturn(buildEcnOrder(STATUS_DRAFT).setId(200L));
        assertServiceException(() -> ecnOrderService.updateEcnOrder(buildSaveReq().setId(100L)),
                MD_ECN_ORDER_NO_DUPLICATE, "ECN001");
    }

    // ========== deleteEcnOrder ==========

    @Test
    public void testDeleteEcnOrder_success() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(buildEcnOrder(STATUS_DRAFT));
        ecnOrderService.deleteEcnOrder(100L);
        verify(ecnOrderItemMapper).deleteByEcnOrderId(100L);
        verify(ecnOrderMapper).deleteById(100L);
    }

    @Test
    public void testDeleteEcnOrder_notExists() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> ecnOrderService.deleteEcnOrder(100L), MD_ECN_ORDER_NOT_EXISTS);
    }

    @Test
    public void testDeleteEcnOrder_statusInvalid() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(buildEcnOrder(STATUS_EXECUTED));
        assertServiceException(() -> ecnOrderService.deleteEcnOrder(100L), MD_ECN_ORDER_STATUS_INVALID);
    }

    // ========== get / validate / page ==========

    @Test
    public void testGetEcnOrder() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(buildEcnOrder(STATUS_DRAFT));
        MesMdEcnOrderDO ecnOrder = ecnOrderService.getEcnOrder(100L);
        assertNotNull(ecnOrder);
        assertEquals("ECN001", ecnOrder.getNo());
    }

    @Test
    public void testGetEcnOrder_null() {
        when(ecnOrderMapper.selectById(999L)).thenReturn(null);
        assertNull(ecnOrderService.getEcnOrder(999L));
    }

    @Test
    public void testValidateEcnOrderExists_success() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(buildEcnOrder(STATUS_DRAFT));
        assertNotNull(ecnOrderService.validateEcnOrderExists(100L));
    }

    @Test
    public void testValidateEcnOrderExists_notExists() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> ecnOrderService.validateEcnOrderExists(100L), MD_ECN_ORDER_NOT_EXISTS);
    }

    @Test
    public void testGetEcnOrderPage() {
        PageResult<MesMdEcnOrderDO> page = new PageResult<>(Arrays.asList(buildEcnOrder(STATUS_DRAFT)), 1L);
        when(ecnOrderMapper.selectPage(any(MesMdEcnOrderPageReqVO.class))).thenReturn(page);
        assertEquals(1, ecnOrderService.getEcnOrderPage(new MesMdEcnOrderPageReqVO()).getTotal());
    }

    // ========== submitEcnOrder ==========

    @Test
    public void testSubmitEcnOrder_success() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(buildEcnOrder(STATUS_DRAFT));
        ecnOrderService.submitEcnOrder(100L);
        ArgumentCaptor<MesMdEcnOrderDO> captor = ArgumentCaptor.forClass(MesMdEcnOrderDO.class);
        verify(ecnOrderMapper).updateById(captor.capture());
        assertEquals(STATUS_APPROVING, captor.getValue().getStatus());
    }

    @Test
    public void testSubmitEcnOrder_statusInvalid() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(buildEcnOrder(STATUS_APPROVED));
        assertServiceException(() -> ecnOrderService.submitEcnOrder(100L), MD_ECN_ORDER_STATUS_INVALID);
    }

    // ========== approveEcnOrder ==========

    @Test
    public void testApproveEcnOrder_approved() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(buildEcnOrder(STATUS_APPROVING));
        ecnOrderService.approveEcnOrder(100L, true, 66L);
        ArgumentCaptor<MesMdEcnOrderDO> captor = ArgumentCaptor.forClass(MesMdEcnOrderDO.class);
        verify(ecnOrderMapper).updateById(captor.capture());
        assertEquals(STATUS_APPROVED, captor.getValue().getStatus());
        assertEquals(66L, captor.getValue().getApproveUserId());
        assertNotNull(captor.getValue().getApproveDate());
    }

    @Test
    public void testApproveEcnOrder_rejected() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(buildEcnOrder(STATUS_APPROVING));
        ecnOrderService.approveEcnOrder(100L, false, 66L);
        ArgumentCaptor<MesMdEcnOrderDO> captor = ArgumentCaptor.forClass(MesMdEcnOrderDO.class);
        verify(ecnOrderMapper).updateById(captor.capture());
        assertEquals(STATUS_REJECTED, captor.getValue().getStatus());
    }

    @Test
    public void testApproveEcnOrder_approveUserNull() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(buildEcnOrder(STATUS_APPROVING));
        assertServiceException(() -> ecnOrderService.approveEcnOrder(100L, true, null),
                MD_ECN_ORDER_BOM_REQUIRED);
    }

    @Test
    public void testApproveEcnOrder_statusInvalid() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(buildEcnOrder(STATUS_DRAFT));
        assertServiceException(() -> ecnOrderService.approveEcnOrder(100L, true, 66L),
                MD_ECN_ORDER_STATUS_INVALID);
    }

    // ========== executeEcnOrder ==========

    @Test
    public void testExecuteEcnOrder_statusInvalid() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(buildEcnOrder(STATUS_DRAFT));
        assertServiceException(() -> ecnOrderService.executeEcnOrder(100L), MD_ECN_ORDER_STATUS_INVALID);
    }

    @Test
    public void testExecuteEcnOrder_changeTypeNull() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(buildEcnOrder(STATUS_APPROVED).setChangeType(null));
        ecnOrderService.executeEcnOrder(100L);
        ArgumentCaptor<MesMdEcnOrderDO> captor = ArgumentCaptor.forClass(MesMdEcnOrderDO.class);
        verify(ecnOrderMapper).updateById(captor.capture());
        assertEquals(STATUS_EXECUTED, captor.getValue().getStatus());
        verify(bomService, never()).updateBom(any(MesBomSaveReqVO.class));
    }

    @Test
    public void testExecuteEcnOrder_unknownChangeType() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(buildEcnOrder(STATUS_APPROVED).setChangeType(99));
        ecnOrderService.executeEcnOrder(100L);
        verify(ecnOrderMapper).updateById(any(MesMdEcnOrderDO.class));
        verify(bomService, never()).updateBom(any(MesBomSaveReqVO.class));
    }

    @Test
    public void testExecuteEcnOrder_createBom() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(
                buildEcnOrder(STATUS_APPROVED).setChangeType(10).setNewBomId(200L));
        when(bomService.getBom(200L)).thenReturn(buildBom(200L, CommonStatusEnum.DISABLE.getStatus()));
        ecnOrderService.executeEcnOrder(100L);
        ArgumentCaptor<MesBomSaveReqVO> captor = ArgumentCaptor.forClass(MesBomSaveReqVO.class);
        verify(bomService).updateBom(captor.capture());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), captor.getValue().getStatus());
        assertEquals(200L, captor.getValue().getId());
    }

    @Test
    public void testExecuteEcnOrder_createBom_newBomIdNull() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(
                buildEcnOrder(STATUS_APPROVED).setChangeType(10).setNewBomId(null));
        ecnOrderService.executeEcnOrder(100L);
        verify(bomService, never()).getBom(anyLong());
        verify(bomService, never()).updateBom(any(MesBomSaveReqVO.class));
    }

    @Test
    public void testExecuteEcnOrder_createBom_alreadyEnable() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(
                buildEcnOrder(STATUS_APPROVED).setChangeType(10).setNewBomId(200L));
        when(bomService.getBom(200L)).thenReturn(buildBom(200L, CommonStatusEnum.ENABLE.getStatus()));
        ecnOrderService.executeEcnOrder(100L);
        verify(bomService, never()).updateBom(any(MesBomSaveReqVO.class));
    }

    @Test
    public void testExecuteEcnOrder_createBom_bomNotExists() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(
                buildEcnOrder(STATUS_APPROVED).setChangeType(10).setNewBomId(200L));
        when(bomService.getBom(200L)).thenReturn(null);
        ecnOrderService.executeEcnOrder(100L);
        verify(bomService, never()).updateBom(any(MesBomSaveReqVO.class));
    }

    @Test
    public void testExecuteEcnOrder_updateBom() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(
                buildEcnOrder(STATUS_APPROVED).setChangeType(20).setBomId(200L).setNewBomId(201L));
        when(bomService.getBom(200L)).thenReturn(buildBom(200L, CommonStatusEnum.ENABLE.getStatus()));
        when(bomService.getBom(201L)).thenReturn(buildBom(201L, CommonStatusEnum.DISABLE.getStatus()));
        ecnOrderService.executeEcnOrder(100L);
        verify(bomService, times(2)).updateBom(any(MesBomSaveReqVO.class));
    }

    @Test
    public void testExecuteEcnOrder_updateBom_bomIdNull() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(
                buildEcnOrder(STATUS_APPROVED).setChangeType(20).setBomId(null).setNewBomId(null));
        ecnOrderService.executeEcnOrder(100L);
        verify(bomService, never()).updateBom(any(MesBomSaveReqVO.class));
    }

    @Test
    public void testExecuteEcnOrder_deleteBom() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(
                buildEcnOrder(STATUS_APPROVED).setChangeType(30).setBomId(200L));
        ecnOrderService.executeEcnOrder(100L);
        verify(bomService).deleteBom(200L);
    }

    @Test
    public void testExecuteEcnOrder_deleteBom_bomIdNull() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(
                buildEcnOrder(STATUS_APPROVED).setChangeType(30).setBomId(null));
        ecnOrderService.executeEcnOrder(100L);
        verify(bomService, never()).deleteBom(anyLong());
    }

    @Test
    public void testExecuteEcnOrder_replaceItem_product() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(
                buildEcnOrder(STATUS_APPROVED).setChangeType(40).setBomId(200L));
        MesMdEcnOrderItemDO item = new MesMdEcnOrderItemDO().setId(1L).setEcnOrderId(100L)
                .setChangeItem(10).setNewValue("999").setBomDetailId(300L);
        when(ecnOrderItemMapper.selectListByEcnOrderId(100L)).thenReturn(Arrays.asList(item));
        when(bomDetailService.validateBomDetailExists(300L)).thenReturn(buildBomDetail());
        ecnOrderService.executeEcnOrder(100L);
        ArgumentCaptor<MesBomDetailSaveReqVO> captor = ArgumentCaptor.forClass(MesBomDetailSaveReqVO.class);
        verify(bomDetailService).updateBomDetail(captor.capture());
        assertEquals(999L, captor.getValue().getProductId());
    }

    @Test
    public void testExecuteEcnOrder_replaceItem_quantity() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(
                buildEcnOrder(STATUS_APPROVED).setChangeType(40).setBomId(200L));
        MesMdEcnOrderItemDO item = new MesMdEcnOrderItemDO().setId(1L).setEcnOrderId(100L)
                .setChangeItem(20).setNewValue("5.50").setBomDetailId(300L);
        when(ecnOrderItemMapper.selectListByEcnOrderId(100L)).thenReturn(Arrays.asList(item));
        when(bomDetailService.validateBomDetailExists(300L)).thenReturn(buildBomDetail());
        ecnOrderService.executeEcnOrder(100L);
        ArgumentCaptor<MesBomDetailSaveReqVO> captor = ArgumentCaptor.forClass(MesBomDetailSaveReqVO.class);
        verify(bomDetailService).updateBomDetail(captor.capture());
        assertTrue(captor.getValue().getQuantity().compareTo(new BigDecimal("5.5")) == 0);
    }

    @Test
    public void testExecuteEcnOrder_replaceItem_blankNewValue() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(
                buildEcnOrder(STATUS_APPROVED).setChangeType(40).setBomId(200L));
        MesMdEcnOrderItemDO item = new MesMdEcnOrderItemDO().setId(1L).setEcnOrderId(100L)
                .setChangeItem(10).setNewValue(null).setBomDetailId(300L);
        when(ecnOrderItemMapper.selectListByEcnOrderId(100L)).thenReturn(Arrays.asList(item));
        when(bomDetailService.validateBomDetailExists(300L)).thenReturn(buildBomDetail());
        ecnOrderService.executeEcnOrder(100L);
        ArgumentCaptor<MesBomDetailSaveReqVO> captor = ArgumentCaptor.forClass(MesBomDetailSaveReqVO.class);
        verify(bomDetailService).updateBomDetail(captor.capture());
        assertEquals(5L, captor.getValue().getProductId());
    }

    @Test
    public void testExecuteEcnOrder_replaceItem_otherChangeItem() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(
                buildEcnOrder(STATUS_APPROVED).setChangeType(40).setBomId(200L));
        MesMdEcnOrderItemDO item = new MesMdEcnOrderItemDO().setId(1L).setEcnOrderId(100L)
                .setChangeItem(30).setNewValue("备注").setBomDetailId(300L);
        when(ecnOrderItemMapper.selectListByEcnOrderId(100L)).thenReturn(Arrays.asList(item));
        when(bomDetailService.validateBomDetailExists(300L)).thenReturn(buildBomDetail());
        ecnOrderService.executeEcnOrder(100L);
        verify(bomDetailService).updateBomDetail(any(MesBomDetailSaveReqVO.class));
    }

    @Test
    public void testExecuteEcnOrder_replaceItem_changeItemNull() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(
                buildEcnOrder(STATUS_APPROVED).setChangeType(40).setBomId(200L));
        MesMdEcnOrderItemDO item = new MesMdEcnOrderItemDO().setId(1L).setEcnOrderId(100L)
                .setChangeItem(null).setBomDetailId(300L);
        when(ecnOrderItemMapper.selectListByEcnOrderId(100L)).thenReturn(Arrays.asList(item));
        when(bomDetailService.validateBomDetailExists(300L)).thenReturn(buildBomDetail());
        ecnOrderService.executeEcnOrder(100L);
        verify(bomDetailService).updateBomDetail(any(MesBomDetailSaveReqVO.class));
    }

    @Test
    public void testExecuteEcnOrder_replaceItem_bomDetailIdNull() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(
                buildEcnOrder(STATUS_APPROVED).setChangeType(40).setBomId(200L));
        MesMdEcnOrderItemDO item = new MesMdEcnOrderItemDO().setId(1L).setEcnOrderId(100L)
                .setChangeItem(10).setNewValue("999").setBomDetailId(null);
        when(ecnOrderItemMapper.selectListByEcnOrderId(100L)).thenReturn(Arrays.asList(item));
        ecnOrderService.executeEcnOrder(100L);
        verify(bomDetailService, never()).updateBomDetail(any(MesBomDetailSaveReqVO.class));
    }

    @Test
    public void testExecuteEcnOrder_replaceItem_emptyItems() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(
                buildEcnOrder(STATUS_APPROVED).setChangeType(40).setBomId(200L));
        when(ecnOrderItemMapper.selectListByEcnOrderId(100L)).thenReturn(Collections.emptyList());
        ecnOrderService.executeEcnOrder(100L);
        verify(bomDetailService, never()).updateBomDetail(any(MesBomDetailSaveReqVO.class));
        verify(ecnOrderMapper).updateById(any(MesMdEcnOrderDO.class));
    }

    @Test
    public void testExecuteEcnOrder_replaceItem_bomIdNull() {
        when(ecnOrderMapper.selectById(100L)).thenReturn(
                buildEcnOrder(STATUS_APPROVED).setChangeType(40).setBomId(null));
        MesMdEcnOrderItemDO item = new MesMdEcnOrderItemDO().setId(1L).setEcnOrderId(100L)
                .setChangeItem(10).setNewValue("999").setBomDetailId(300L);
        when(ecnOrderItemMapper.selectListByEcnOrderId(100L)).thenReturn(Arrays.asList(item));
        ecnOrderService.executeEcnOrder(100L);
        verify(bomDetailService, never()).updateBomDetail(any(MesBomDetailSaveReqVO.class));
    }

}
