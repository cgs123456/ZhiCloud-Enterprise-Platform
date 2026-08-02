package cn.iocoder.yudao.module.wms.service.md.merchant;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.wms.controller.admin.md.merchant.vo.WmsMerchantListReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.md.merchant.vo.WmsMerchantPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.md.merchant.vo.WmsMerchantSaveReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.merchant.WmsMerchantDO;
import cn.iocoder.yudao.module.wms.dal.mysql.md.merchant.WmsMerchantMapper;
import cn.iocoder.yudao.module.wms.enums.md.WmsMerchantTypeEnum;
import cn.iocoder.yudao.module.wms.service.order.receipt.WmsReceiptOrderService;
import cn.iocoder.yudao.module.wms.service.order.shipment.WmsShipmentOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link WmsMerchantServiceImpl} 的纯 Mockito 单元测试（Item C 覆盖率补测）
 *
 * @author ZhiCloud 平台加固
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WmsMerchantServiceImplUnitTest {

    @Mock
    private WmsMerchantMapper merchantMapper;
    @Mock
    private WmsReceiptOrderService receiptOrderService;
    @Mock
    private WmsShipmentOrderService shipmentOrderService;

    @InjectMocks
    private WmsMerchantServiceImpl merchantService;

    private static WmsMerchantSaveReqVO buildSaveReqVO(Long id) {
        WmsMerchantSaveReqVO reqVO = new WmsMerchantSaveReqVO();
        reqVO.setId(id);
        reqVO.setCode("M001");
        reqVO.setName("上海某某物流");
        reqVO.setType(WmsMerchantTypeEnum.SUPPLIER.getType());
        reqVO.setContact("张三");
        reqVO.setMobile("13800000000");
        return reqVO;
    }

    @Test
    public void testCreateMerchant_success() {
        when(merchantMapper.selectByCode("M001")).thenReturn(null);
        when(merchantMapper.selectByName("上海某某物流")).thenReturn(null);
        doAnswer(invocation -> {
            WmsMerchantDO arg = invocation.getArgument(0);
            arg.setId(300L);
            return 1;
        }).when(merchantMapper).insert(any(WmsMerchantDO.class));

        assertEquals(300L, merchantService.createMerchant(buildSaveReqVO(null)));
    }

    @Test
    public void testCreateMerchant_codeDuplicate() {
        when(merchantMapper.selectByCode("M001")).thenReturn(WmsMerchantDO.builder().id(1L).build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> merchantService.createMerchant(buildSaveReqVO(null)));
        assertEquals(MERCHANT_CODE_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    public void testCreateMerchant_nameDuplicate() {
        when(merchantMapper.selectByCode("M001")).thenReturn(null);
        when(merchantMapper.selectByName("上海某某物流")).thenReturn(WmsMerchantDO.builder().id(1L).build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> merchantService.createMerchant(buildSaveReqVO(null)));
        assertEquals(MERCHANT_NAME_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    public void testUpdateMerchant_success() {
        when(merchantMapper.selectById(5L)).thenReturn(WmsMerchantDO.builder().id(5L).build());
        when(merchantMapper.selectByCode("M001")).thenReturn(WmsMerchantDO.builder().id(5L).build());
        when(merchantMapper.selectByName("上海某某物流")).thenReturn(WmsMerchantDO.builder().id(5L).build());

        merchantService.updateMerchant(buildSaveReqVO(5L));

        verify(merchantMapper).updateById(any(WmsMerchantDO.class));
    }

    @Test
    public void testUpdateMerchant_notExists() {
        when(merchantMapper.selectById(5L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> merchantService.updateMerchant(buildSaveReqVO(5L)));
        assertEquals(MERCHANT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    public void testDeleteMerchant_success() {
        when(merchantMapper.selectById(5L)).thenReturn(WmsMerchantDO.builder().id(5L).build());
        when(receiptOrderService.getReceiptOrderCountByMerchantId(5L)).thenReturn(0L);
        when(shipmentOrderService.getShipmentOrderCountByMerchantId(5L)).thenReturn(0L);

        merchantService.deleteMerchant(5L);

        verify(merchantMapper).deleteById(5L);
    }

    @Test
    public void testDeleteMerchant_hasReceiptOrder() {
        when(merchantMapper.selectById(5L)).thenReturn(WmsMerchantDO.builder().id(5L).build());
        when(receiptOrderService.getReceiptOrderCountByMerchantId(5L)).thenReturn(2L);

        ServiceException ex = assertThrows(ServiceException.class, () -> merchantService.deleteMerchant(5L));
        assertEquals(MERCHANT_HAS_ORDER.getCode(), ex.getCode());
    }

    @Test
    public void testDeleteMerchant_hasShipmentOrder() {
        when(merchantMapper.selectById(5L)).thenReturn(WmsMerchantDO.builder().id(5L).build());
        when(receiptOrderService.getReceiptOrderCountByMerchantId(5L)).thenReturn(0L);
        when(shipmentOrderService.getShipmentOrderCountByMerchantId(5L)).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> merchantService.deleteMerchant(5L));
        assertEquals(MERCHANT_HAS_ORDER.getCode(), ex.getCode());
    }

    @Test
    public void testValidateSupplierMerchantExists() {
        when(merchantMapper.selectById(1L)).thenReturn(
                WmsMerchantDO.builder().id(1L).type(WmsMerchantTypeEnum.SUPPLIER.getType()).build());
        when(merchantMapper.selectById(2L)).thenReturn(
                WmsMerchantDO.builder().id(2L).type(WmsMerchantTypeEnum.CUSTOMER_SUPPLIER.getType()).build());
        when(merchantMapper.selectById(3L)).thenReturn(
                WmsMerchantDO.builder().id(3L).type(WmsMerchantTypeEnum.CUSTOMER.getType()).build());

        assertNotNull(merchantService.validateSupplierMerchantExists(1L));
        assertNotNull(merchantService.validateSupplierMerchantExists(2L));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> merchantService.validateSupplierMerchantExists(3L));
        assertEquals(MERCHANT_NOT_SUPPLIER.getCode(), ex.getCode());
    }

    @Test
    public void testValidateCustomerMerchantExists() {
        when(merchantMapper.selectById(1L)).thenReturn(
                WmsMerchantDO.builder().id(1L).type(WmsMerchantTypeEnum.CUSTOMER.getType()).build());
        when(merchantMapper.selectById(2L)).thenReturn(
                WmsMerchantDO.builder().id(2L).type(WmsMerchantTypeEnum.CUSTOMER_SUPPLIER.getType()).build());
        when(merchantMapper.selectById(3L)).thenReturn(
                WmsMerchantDO.builder().id(3L).type(WmsMerchantTypeEnum.SUPPLIER.getType()).build());

        assertNotNull(merchantService.validateCustomerMerchantExists(1L));
        assertNotNull(merchantService.validateCustomerMerchantExists(2L));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> merchantService.validateCustomerMerchantExists(3L));
        assertEquals(MERCHANT_NOT_CUSTOMER.getCode(), ex.getCode());
    }

    @Test
    public void testQueryMethods() {
        when(merchantMapper.selectById(1L)).thenReturn(WmsMerchantDO.builder().id(1L).build());
        when(merchantMapper.selectPage(any(WmsMerchantPageReqVO.class))).thenReturn(PageResult.empty());
        when(merchantMapper.selectList(any(WmsMerchantListReqVO.class)))
                .thenReturn(List.of(WmsMerchantDO.builder().id(1L).build()));
        when(merchantMapper.selectByIds(anyCollection())).thenReturn(List.of(WmsMerchantDO.builder().id(1L).build()));

        assertNotNull(merchantService.getMerchant(1L));
        assertNotNull(merchantService.getMerchantPage(new WmsMerchantPageReqVO()));
        assertEquals(1, merchantService.getMerchantList(new WmsMerchantListReqVO()).size());
        assertEquals(1, merchantService.getMerchantList(List.of(1L)).size());
        assertTrue(merchantService.getMerchantList(Collections.emptyList()).isEmpty());
    }

}
