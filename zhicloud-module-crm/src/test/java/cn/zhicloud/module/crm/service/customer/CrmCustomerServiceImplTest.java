package cn.zhicloud.module.crm.service.customer;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.crm.controller.admin.customer.vo.customer.CrmCustomerPageReqVO;
import cn.zhicloud.module.crm.controller.admin.customer.vo.customer.CrmCustomerSaveReqVO;
import cn.zhicloud.module.crm.dal.dataobject.customer.CrmCustomerDO;
import cn.zhicloud.module.crm.dal.mysql.customer.CrmCustomerMapper;
import cn.zhicloud.module.crm.service.business.CrmBusinessService;
import cn.zhicloud.module.crm.service.contact.CrmContactService;
import cn.zhicloud.module.crm.service.contract.CrmContractService;
import cn.zhicloud.module.crm.service.permission.CrmOwnerRecordService;
import cn.zhicloud.module.crm.service.permission.CrmPermissionService;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import cn.zhicloud.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.annotation.Resource;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link CrmCustomerServiceImpl} 的单元测试
 *
 * @author 智云
 */
@Import(CrmCustomerServiceImpl.class)
public class CrmCustomerServiceImplTest extends BaseDbUnitTest {

    @Resource
    private CrmCustomerServiceImpl customerService;

    @Resource
    private CrmCustomerMapper customerMapper;

    @MockitoBean
    private CrmPermissionService crmPermissionService;
    @MockitoBean
    private CrmOwnerRecordService ownerRecordService;
    @MockitoBean
    private CrmContactService contactService;
    @MockitoBean
    private CrmCustomerPoolConfigService customerPoolConfigService;
    @MockitoBean
    private CrmBusinessService businessService;
    @MockitoBean
    private CrmContractService contractService;
    @MockitoBean
    private CrmCustomerLimitConfigService customerLimitConfigService;
    @MockitoBean
    private AdminUserApi adminUserApi;

    @Test
    public void test_createCustomer_success() {
        // mock
        when(adminUserApi.getUser(any())).thenReturn(new AdminUserRespDTO());

        // 准备参数
        CrmCustomerSaveReqVO reqVO = new CrmCustomerSaveReqVO();
        reqVO.setName("测试客户");
        reqVO.setMobile("13900139000");

        // 调用
        Long id = customerService.createCustomer(reqVO, 1L);

        // 校验
        CrmCustomerDO result = customerMapper.selectById(id);
        assertNotNull(result);
        assertEquals("测试客户", result.getName());
    }

    @Test
    public void test_deleteCustomer_success() {
        // mock 数据
        CrmCustomerDO customer = randomPojo(CrmCustomerDO.class, o -> {
            o.setOwnerUserId(1L);
        });
        customerMapper.insert(customer);

        // 调用
        customerService.deleteCustomer(customer.getId());

        // 校验
        assertNull(customerMapper.selectById(customer.getId()));
    }

    @Test
    public void test_deleteCustomer_notExists() {
        assertThrows(Exception.class, () -> customerService.deleteCustomer(randomLongId()));
    }

    @Test
    public void test_getCustomer_success() {
        // mock 数据
        CrmCustomerDO customer = randomPojo(CrmCustomerDO.class);
        customerMapper.insert(customer);

        // 调用并校验
        CrmCustomerDO result = customerService.getCustomer(customer.getId());
        assertNotNull(result);
        assertEquals(customer.getId(), result.getId());
    }

    @Test
    public void test_getCustomer_notExists() {
        assertThrows(Exception.class, () -> customerService.validateCustomer(randomLongId()));
    }

    @Test
    public void test_getCustomerPage_success() {
        // mock 数据
        CrmCustomerDO customer = randomPojo(CrmCustomerDO.class, o -> {
            o.setName("分页测试客户");
            o.setOwnerUserId(1L);
        });
        customerMapper.insert(customer);

        // 准备参数
        CrmCustomerPageReqVO reqVO = new CrmCustomerPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        // 调用并校验
        var pageResult = customerService.getCustomerPage(reqVO, 1L);
        assertNotNull(pageResult);
    }

}
