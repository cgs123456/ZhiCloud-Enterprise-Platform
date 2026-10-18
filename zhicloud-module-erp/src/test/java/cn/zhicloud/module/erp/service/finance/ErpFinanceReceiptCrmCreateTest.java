package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFinanceReceiptItemDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpFinanceReceiptItemMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.zhicloud.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.zhicloud.module.erp.enums.ErpAuditStatus;
import cn.zhicloud.module.erp.enums.common.ErpBizTypeEnum;
import cn.zhicloud.module.erp.service.accounting.ErpAccountingEventService;
import cn.zhicloud.module.erp.service.finance.bo.ErpFinanceReceiptCrmCreateReqBO;
import cn.zhicloud.module.erp.service.sale.ErpCustomerService;
import cn.zhicloud.module.erp.service.sale.ErpSaleOutService;
import cn.zhicloud.module.erp.service.sale.ErpSaleReturnService;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * §12.267（D1）：CRM 回款联动建收款单的 H2 真表测试。
 *
 * <p>覆盖 mock 测不成的事：DO 全列 INSERT/SELECT 在真表可执行（列与 DDL 对齐）、
 * 幂等唯一键真的生效、集成建单的账户取「租户默认账户」（§12.267 D1 修订）、
 * CRM 明细经「集成建单 -> 财务改账户保存 -> 删除」全程不改语义。
 * H2 不复现 InnoDB 的锁语义，并发仍以真机探针为准（同 §12.248/§12.233 口径）。
 */
@Import(ErpFinanceReceiptServiceImpl.class)
public class ErpFinanceReceiptCrmCreateTest extends BaseDbUnitTest {

    @Resource
    private ErpFinanceReceiptServiceImpl financeReceiptService;

    @Resource
    private ErpFinanceReceiptMapper financeReceiptMapper;
    @Resource
    private ErpFinanceReceiptItemMapper financeReceiptItemMapper;

    @MockitoBean
    private ErpNoRedisDAO noRedisDAO;
    @MockitoBean
    private ErpCustomerService customerService;
    @MockitoBean
    private ErpAccountService accountService;
    @MockitoBean
    private ErpSaleOutService saleOutService;
    @MockitoBean
    private ErpSaleReturnService saleReturnService;
    @MockitoBean
    private ErpAccountingEventService accountingEventService;
    @MockitoBean
    private AdminUserApi adminUserApi;

    @Test
    public void test_createFromCrm_persistsReceiptAndCrmItem() {
        when(noRedisDAO.generate(ErpNoRedisDAO.FINANCE_RECEIPT_NO_PREFIX)).thenReturn("HS-D1-0001");
        when(accountService.getDefaultAccount()).thenReturn(new ErpAccountDO().setId(2L));

        Long id = financeReceiptService.createFinanceReceiptFromCrm(
                crmReq("crm-receivable-9001", "HK20261018000001", 9001L, new BigDecimal("400.00")));

        ErpFinanceReceiptDO receipt = financeReceiptMapper.selectById(id);
        assertNotNull(receipt);
        assertEquals("HS-D1-0001", receipt.getNo());
        assertEquals(ErpAuditStatus.PROCESS.getStatus(), receipt.getStatus());
        assertEquals(913000L, receipt.getCustomerId());
        assertEquals(LocalDateTime.of(2026, 10, 18, 10, 0), receipt.getReceiptTime());
        assertEquals(0, new BigDecimal("400.0000").compareTo(receipt.getTotalPrice()));
        assertEquals(0, new BigDecimal("400.0000").compareTo(receipt.getReceiptPrice()));
        assertEquals(0, BigDecimal.ZERO.compareTo(receipt.getDiscountPrice()));
        // 账户取租户默认账户（§12.267 D1 修订）；财务人员留空：财务审核过账前补选（D8）
        assertEquals(2L, receipt.getAccountId());
        assertNull(receipt.getFinanceUserId());
        assertTrue(receipt.getRemark().contains("HK20261018000001"));

        List<ErpFinanceReceiptItemDO> items = financeReceiptItemMapper.selectListByReceiptId(id);
        assertEquals(1, items.size());
        ErpFinanceReceiptItemDO item = items.get(0);
        assertEquals(ErpBizTypeEnum.CRM_RECEIVABLE.getType(), item.getBizType());
        assertEquals(9001L, item.getBizId());
        assertEquals("HK20261018000001", item.getBizNo());
        assertEquals(0, new BigDecimal("400.0000").compareTo(item.getTotalPrice()));
        assertEquals(0, new BigDecimal("400.0000").compareTo(item.getReceiptedPrice()));
        assertEquals(0, new BigDecimal("400.0000").compareTo(item.getReceiptPrice()));
        // CRM 明细没有 ERP 源单可回写：不得触碰销售出库/退货服务
        verifyNoInteractions(saleOutService, saleReturnService);
    }

    @Test
    public void test_createFromCrm_sameKeySamePayloadReturnsOriginalId() {
        when(noRedisDAO.generate(ErpNoRedisDAO.FINANCE_RECEIPT_NO_PREFIX)).thenReturn("HS-D1-0002");
        when(accountService.getDefaultAccount()).thenReturn(new ErpAccountDO().setId(2L));
        ErpFinanceReceiptCrmCreateReqBO req = crmReq("crm-receivable-9002", "HK20261018000002", 9002L,
                new BigDecimal("400.00"));

        Long first = financeReceiptService.createFinanceReceiptFromCrm(req);
        Long second = financeReceiptService.createFinanceReceiptFromCrm(req);

        // 同键同载荷：返回原单且不产生第二行（重试路径的真实语义）
        assertEquals(first, second);
        assertEquals(1L, financeReceiptMapper.selectCount(null));
    }

    @Test
    public void test_createFromCrm_sameKeyDifferentPayloadRejected() {
        when(noRedisDAO.generate(ErpNoRedisDAO.FINANCE_RECEIPT_NO_PREFIX)).thenReturn("HS-D1-0003");
        when(accountService.getDefaultAccount()).thenReturn(new ErpAccountDO().setId(2L));
        financeReceiptService.createFinanceReceiptFromCrm(
                crmReq("crm-receivable-9003", "HK20261018000003", 9003L, new BigDecimal("400.00")));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> financeReceiptService.createFinanceReceiptFromCrm(
                        crmReq("crm-receivable-9003", "HK20261018000003", 9003L, new BigDecimal("500.00"))));

        assertEquals(1_030_602_007, ex.getCode());
        assertEquals(1L, financeReceiptMapper.selectCount(null));
    }

    @Test
    public void test_updateKeepsCrmItemFieldsFromVo() {
        when(noRedisDAO.generate(ErpNoRedisDAO.FINANCE_RECEIPT_NO_PREFIX)).thenReturn("HS-D1-0004");
        when(accountService.getDefaultAccount()).thenReturn(new ErpAccountDO().setId(2L));
        Long id = financeReceiptService.createFinanceReceiptFromCrm(
                crmReq("crm-receivable-9004", "HK20261018000004", 9004L, new BigDecimal("400.00")));
        ErpFinanceReceiptItemDO item = financeReceiptItemMapper.selectListByReceiptId(id).get(0);

        // 财务改账户后保存：前端回传明细（含 bizNo）走 HTTP VO 路径，字段必须原样保留
        ErpFinanceReceiptSaveReqVO updateReqVO = new ErpFinanceReceiptSaveReqVO()
                .setId(id)
                .setReceiptTime(LocalDateTime.of(2026, 10, 18, 10, 0))
                .setCustomerId(913000L)
                .setAccountId(2L)
                .setDiscountPrice(BigDecimal.ZERO)
                .setRemark("财务补选账户")
                .setItems(List.of(new ErpFinanceReceiptSaveReqVO.Item()
                        .setId(item.getId())
                        .setBizType(ErpBizTypeEnum.CRM_RECEIVABLE.getType())
                        .setBizId(9004L)
                        .setBizNo("HK20261018000004")
                        .setReceiptedPrice(new BigDecimal("400.00"))
                        .setReceiptPrice(new BigDecimal("400.00"))));
        financeReceiptService.updateFinanceReceipt(updateReqVO);

        ErpFinanceReceiptDO receipt = financeReceiptMapper.selectById(id);
        assertEquals(2L, receipt.getAccountId());
        ErpFinanceReceiptItemDO after = financeReceiptItemMapper.selectListByReceiptId(id).get(0);
        assertEquals(ErpBizTypeEnum.CRM_RECEIVABLE.getType(), after.getBizType());
        assertEquals(9004L, after.getBizId());
        assertEquals("HK20261018000004", after.getBizNo());
        assertEquals(0, new BigDecimal("400.0000").compareTo(after.getTotalPrice()));
    }

    @Test
    public void test_deleteCrmLinkedReceiptSkipsSaleWriteBack() {
        when(noRedisDAO.generate(ErpNoRedisDAO.FINANCE_RECEIPT_NO_PREFIX)).thenReturn("HS-D1-0005");
        when(accountService.getDefaultAccount()).thenReturn(new ErpAccountDO().setId(2L));
        Long id = financeReceiptService.createFinanceReceiptFromCrm(
                crmReq("crm-receivable-9005", "HK20261018000005", 9005L, new BigDecimal("400.00")));

        financeReceiptService.deleteFinanceReceipt(List.of(id));

        assertNull(financeReceiptMapper.selectById(id));
        assertEquals(0, financeReceiptItemMapper.selectListByReceiptId(id).size());
        // 删除回收款金额的路径对 CRM 明细整行跳过：不得触碰销售出库/退货服务
        verifyNoInteractions(saleOutService, saleReturnService);
    }

    @Test
    public void test_createFromCrm_missingDefaultAccountRejected() {
        when(accountService.getDefaultAccount()).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> financeReceiptService.createFinanceReceiptFromCrm(
                        crmReq("crm-receivable-9006", "HK20261018000006", 9006L, new BigDecimal("400.00"))));

        assertEquals(1_030_602_008, ex.getCode());
        assertEquals(0L, financeReceiptMapper.selectCount(null));
    }

    private static ErpFinanceReceiptCrmCreateReqBO crmReq(String key, String no, Long receivableId,
                                                          BigDecimal price) {
        return ErpFinanceReceiptCrmCreateReqBO.builder()
                .idempotencyKey(key)
                .receivableNo(no)
                .receivableId(receivableId)
                .customerId(913000L)
                .receiptTime(LocalDateTime.of(2026, 10, 18, 10, 0))
                .price(price)
                .build();
    }

}
