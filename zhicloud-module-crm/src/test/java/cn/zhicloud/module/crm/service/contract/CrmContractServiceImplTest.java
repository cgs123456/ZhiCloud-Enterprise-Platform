package cn.zhicloud.module.crm.service.contract;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.zhicloud.module.crm.dal.mysql.contract.CrmContractMapper;
import cn.zhicloud.module.bpm.api.task.BpmProcessInstanceApi;
import cn.zhicloud.module.crm.dal.redis.no.CrmNoRedisDAO;
import cn.zhicloud.module.crm.service.business.CrmBusinessService;
import cn.zhicloud.module.crm.service.contact.CrmContactService;
import cn.zhicloud.module.crm.service.customer.CrmCustomerService;
import cn.zhicloud.module.crm.service.permission.CrmPermissionService;
import cn.zhicloud.module.crm.service.product.CrmProductService;
import cn.zhicloud.module.crm.service.receivable.CrmReceivableService;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.annotation.Resource;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CrmContractServiceImpl} 的单元测试
 *
 * @author 智云
 */
@Import(CrmContractServiceImpl.class)
public class CrmContractServiceImplTest extends BaseDbUnitTest {

    @Resource
    private CrmContractServiceImpl contractService;

    @Resource
    private CrmContractMapper contractMapper;

    @MockitoBean
    private CrmPermissionService crmPermissionService;
    @MockitoBean
    private CrmCustomerService customerService;
    @MockitoBean
    private CrmBusinessService businessService;
    @MockitoBean
    private CrmContactService contactService;
    @MockitoBean
    private CrmProductService productService;
    @MockitoBean
    private CrmReceivableService receivableService;
    @MockitoBean
    private CrmContractConfigService contractConfigService;
    @MockitoBean
    private CrmNoRedisDAO noRedisDAO;
    @MockitoBean
    private BpmProcessInstanceApi bpmProcessInstanceApi;
    @MockitoBean
    private AdminUserApi adminUserApi;

    @Test
    public void test_getContract_success() {
        // mock 数据
        CrmContractDO contract = randomPojo(CrmContractDO.class);
        contractMapper.insert(contract);

        // 调用并校验
        CrmContractDO result = contractService.getContract(contract.getId());
        assertNotNull(result);
        assertEquals(contract.getId(), result.getId());
    }

    @Test
    public void test_validateContract_notExists() {
        assertThrows(Exception.class, () -> contractService.validateContract(randomLongId()));
    }

    @Test
    public void test_validateContract_success() {
        // mock 数据
        CrmContractDO contract = randomPojo(CrmContractDO.class);
        contractMapper.insert(contract);

        // 调用并校验
        CrmContractDO result = contractService.validateContract(contract.getId());
        assertNotNull(result);
        assertEquals(contract.getId(), result.getId());
    }

    @Test
    public void test_deleteContract_success() {
        // mock 数据
        CrmContractDO contract = randomPojo(CrmContractDO.class);
        contractMapper.insert(contract);

        // 调用
        contractService.deleteContract(contract.getId());

        // 校验
        assertNull(contractMapper.selectById(contract.getId()));
    }

    @Test
    public void test_deleteContract_notExists() {
        assertThrows(Exception.class, () -> contractService.deleteContract(randomLongId()));
    }

    @Test
    public void test_getContractCountByCustomerId_success() {
        // mock 数据
        Long customerId = 100L;
        CrmContractDO contract = randomPojo(CrmContractDO.class, o -> {
            o.setCustomerId(customerId);
        });
        contractMapper.insert(contract);

        // 调用并校验
        Long count = contractService.getContractCountByCustomerId(customerId);
        assertTrue(count >= 1);
    }

}
