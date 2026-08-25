package cn.zhicloud.module.wms.service.order.asn;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.module.wms.controller.admin.order.asn.vo.WmsAsnOrderDetailSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.order.asn.WmsAsnOrderDetailDO;
import cn.zhicloud.module.wms.dal.mysql.order.asn.WmsAsnOrderDetailMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.ASN_ORDER_DETAIL_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * {@link WmsAsnOrderDetailServiceImpl} 的纯 Mockito 单元测试（Item C 覆盖率补测）
 *
 * @author ZhiCloud 平台加固
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WmsAsnOrderDetailServiceImplUnitTest {

    @Mock
    private WmsAsnOrderDetailMapper asnOrderDetailMapper;

    @InjectMocks
    private WmsAsnOrderDetailServiceImpl asnOrderDetailService;

    private static WmsAsnOrderDetailSaveReqVO buildDetailVO(Long id) {
        WmsAsnOrderDetailSaveReqVO vo = new WmsAsnOrderDetailSaveReqVO();
        vo.setId(id);
        vo.setSkuId(11L);
        vo.setProductName("显示器");
        vo.setExpectedQuantity(new BigDecimal("10"));
        vo.setUnit("台");
        return vo;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateAsnOrderDetailList_success() {
        asnOrderDetailService.createAsnOrderDetailList(100L, List.of(buildDetailVO(9L)));

        ArgumentCaptor<List<WmsAsnOrderDetailDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(asnOrderDetailMapper).insertBatch(captor.capture());
        WmsAsnOrderDetailDO saved = captor.getValue().get(0);
        // 主键被重置、asnOrderId 被回填、已收数量归零
        assertNull(saved.getId());
        assertEquals(100L, saved.getAsnOrderId());
        assertEquals(BigDecimal.ZERO, saved.getReceivedQuantity());
    }

    @Test
    public void testCreateAsnOrderDetailList_empty() {
        asnOrderDetailService.createAsnOrderDetailList(100L, Collections.emptyList());
        asnOrderDetailService.createAsnOrderDetailList(100L, null);

        verify(asnOrderDetailMapper, never()).insertBatch(anyList());
    }

    @Test
    public void testUpdateAsnOrderDetailList_deletesThenCreates() {
        asnOrderDetailService.updateAsnOrderDetailList(100L, List.of(buildDetailVO(null)));

        InOrder inOrder = inOrder(asnOrderDetailMapper);
        inOrder.verify(asnOrderDetailMapper).deleteByAsnOrderId(100L);
        inOrder.verify(asnOrderDetailMapper).insertBatch(anyList());
    }

    @Test
    public void testDeleteAsnOrderDetailListByAsnOrderId() {
        asnOrderDetailService.deleteAsnOrderDetailListByAsnOrderId(100L);

        verify(asnOrderDetailMapper).deleteByAsnOrderId(100L);
    }

    @Test
    public void testGetAsnOrderDetailList() {
        when(asnOrderDetailMapper.selectListByAsnOrderId(100L))
                .thenReturn(List.of(WmsAsnOrderDetailDO.builder().id(1L).build()));

        assertEquals(1, asnOrderDetailService.getAsnOrderDetailList(100L).size());
    }

    @Test
    public void testAddReceivedQuantity_success() {
        when(asnOrderDetailMapper.selectById(1L))
                .thenReturn(WmsAsnOrderDetailDO.builder().id(1L).receivedQuantity(new BigDecimal("3")).build());

        asnOrderDetailService.addReceivedQuantity(1L, new BigDecimal("2"));

        ArgumentCaptor<WmsAsnOrderDetailDO> captor = ArgumentCaptor.forClass(WmsAsnOrderDetailDO.class);
        verify(asnOrderDetailMapper).updateById(captor.capture());
        assertEquals(0, new BigDecimal("5").compareTo(captor.getValue().getReceivedQuantity()));
    }

    @Test
    public void testAddReceivedQuantity_nullCurrentTreatedAsZero() {
        when(asnOrderDetailMapper.selectById(1L))
                .thenReturn(WmsAsnOrderDetailDO.builder().id(1L).receivedQuantity(null).build());

        asnOrderDetailService.addReceivedQuantity(1L, new BigDecimal("2"));

        ArgumentCaptor<WmsAsnOrderDetailDO> captor = ArgumentCaptor.forClass(WmsAsnOrderDetailDO.class);
        verify(asnOrderDetailMapper).updateById(captor.capture());
        assertEquals(0, new BigDecimal("2").compareTo(captor.getValue().getReceivedQuantity()));
    }

    @Test
    public void testAddReceivedQuantity_notExists() {
        when(asnOrderDetailMapper.selectById(404L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> asnOrderDetailService.addReceivedQuantity(404L, BigDecimal.ONE));
        assertEquals(ASN_ORDER_DETAIL_NOT_EXISTS.getCode(), ex.getCode());
    }

}
