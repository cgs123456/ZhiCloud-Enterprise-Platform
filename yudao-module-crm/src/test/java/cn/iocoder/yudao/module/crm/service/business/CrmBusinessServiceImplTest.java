package cn.iocoder.yudao.module.crm.service.business;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.crm.controller.admin.business.vo.business.CrmBusinessPageReqVO;
import cn.iocoder.yudao.module.crm.dal.dataobject.business.CrmBusinessDO;
import cn.iocoder.yudao.module.crm.dal.mysql.business.CrmBusinessMapper;
import cn.iocoder.yudao.module.crm.service.business.CrmBusinessStatusService;
import cn.iocoder.yudao.module.crm.service.contact.CrmContactBusinessService;
import cn.iocoder.yudao.module.crm.service.contact.CrmContactService;
import cn.iocoder.yudao.module.crm.service.contract.CrmContractService;
import cn.iocoder.yudao.module.crm.service.customer.CrmCustomerService;
import cn.iocoder.yudao.module.crm.service.permission.CrmPermissionService;
import cn.iocoder.yudao.module.crm.service.product.CrmProductService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.annotation.Resource;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * {@link CrmBusinessServiceImpl} 的单元测试（Phase 4 核心域补测）
 *
 * @author ZhiCloud 平台加固
 */
@Import(CrmBusinessServiceImpl.class)
public class CrmBusinessServiceImplTest extends BaseDbUnitTest {

    @Resource
    private CrmBusinessServiceImpl businessService;
    @Resource
    private CrmBusinessMapper businessMapper;

    @MockitoBean
    private CrmBusinessStatusService businessStatusService;
    @MockitoBean
    private CrmContractService contractService;
    @MockitoBean
    private CrmCustomerService customerService;
    @MockitoBean
    private CrmContactService contactService;
    @MockitoBean
    private CrmContactBusinessService contactBusinessService;
    @MockitoBean
    private CrmPermissionService permissionService;
    @MockitoBean
    private CrmProductService productService;
    @MockitoBean
    private AdminUserApi adminUserApi;

    @Test
    public void test_getBusiness_success() {
        // 准备数据
        CrmBusinessDO business = randomPojo(CrmBusinessDO.class);
        businessMapper.insert(business);

        // 调用并校验
        CrmBusinessDO result = businessService.getBusiness(business.getId());
        assertNotNull(result);
        assertEquals(business.getId(), result.getId());
    }

    @Test
    public void test_validateBusiness_notExists() {
        assertThrows(Exception.class, () -> businessService.validateBusiness(randomLongId()));
    }

    @Test
    public void test_getBusinessList_success() {
        // 准备数据
        CrmBusinessDO b1 = randomPojo(CrmBusinessDO.class);
        CrmBusinessDO b2 = randomPojo(CrmBusinessDO.class);
        businessMapper.insert(b1);
        businessMapper.insert(b2);

        // 调用并校验
        List<CrmBusinessDO> list = businessService.getBusinessList(List.of(b1.getId(), b2.getId()));
        assertEquals(2, list.size());
    }

    @Test
    public void test_getBusinessCountByCustomerId_success() {
        // 准备数据
        CrmBusinessDO b1 = randomPojo(CrmBusinessDO.class, o -> o.setCustomerId(100L));
        CrmBusinessDO b2 = randomPojo(CrmBusinessDO.class, o -> o.setCustomerId(100L));
        CrmBusinessDO b3 = randomPojo(CrmBusinessDO.class, o -> o.setCustomerId(200L));
        businessMapper.insert(b1);
        businessMapper.insert(b2);
        businessMapper.insert(b3);

        // 调用并校验
        assertEquals(2L, businessService.getBusinessCountByCustomerId(100L));
    }

    @Test
    public void test_getBusinessPage_success() {
        // 准备数据
        CrmBusinessDO business = randomPojo(CrmBusinessDO.class);
        businessMapper.insert(business);

        // 调用并校验
        CrmBusinessPageReqVO reqVO = new CrmBusinessPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        PageResult<CrmBusinessDO> page = businessService.getBusinessPage(reqVO, 1L);
        assertNotNull(page);
    }

    @Test
    public void test_deleteBusiness_success() {
        // 准备数据
        CrmBusinessDO business = randomPojo(CrmBusinessDO.class);
        businessMapper.insert(business);

        // mock：未关联合同，允许删除
        when(contractService.getContractCountByBusinessId(anyLong())).thenReturn(0L);

        // 调用
        businessService.deleteBusiness(business.getId());

        // 校验
        assertNull(businessMapper.selectById(business.getId()));
    }

}
