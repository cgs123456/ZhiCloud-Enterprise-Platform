package cn.zhicloud.module.crm.controller.admin.followup;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.crm.controller.admin.followup.vo.CrmFollowUpRecordPageReqVO;
import cn.zhicloud.module.crm.controller.admin.followup.vo.CrmFollowUpRecordRespVO;
import cn.zhicloud.module.crm.dal.dataobject.followup.CrmFollowUpRecordDO;
import cn.zhicloud.module.crm.service.business.CrmBusinessService;
import cn.zhicloud.module.crm.service.contact.CrmContactService;
import cn.zhicloud.module.crm.service.followup.CrmFollowUpRecordService;
import cn.zhicloud.module.crm.service.permission.CrmPermissionService;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrmFollowUpRecordControllerTest {

    @Mock
    private CrmFollowUpRecordService followUpRecordService;

    @Mock
    private CrmContactService contactService;

    @Mock
    private CrmBusinessService businessService;

    @Mock
    private CrmPermissionService permissionService;

    @Mock
    private AdminUserApi adminUserApi;

    private CrmFollowUpRecordController controller;

    @BeforeEach
    void setUp() {
        controller = new CrmFollowUpRecordController();
        ReflectionTestUtils.setField(controller, "followUpRecordService", followUpRecordService);
        ReflectionTestUtils.setField(controller, "contactService", contactService);
        ReflectionTestUtils.setField(controller, "businessService", businessService);
        ReflectionTestUtils.setField(controller, "permissionService", permissionService);
        ReflectionTestUtils.setField(controller, "adminUserApi", adminUserApi);
    }

    @Test
    void pageAcceptsNullContactAndBusinessCollections() {
        CrmFollowUpRecordDO record = new CrmFollowUpRecordDO();
        record.setId(6600000L);
        record.setBizType(1);
        record.setBizId(6460000L);
        record.setContactIds(null);
        record.setBusinessIds(null);
        PageResult<CrmFollowUpRecordDO> pageResult = new PageResult<>(List.of(record), 1L);
        when(followUpRecordService.getFollowUpRecordPage(any())).thenReturn(pageResult);

        CommonResult<PageResult<CrmFollowUpRecordRespVO>> result = controller.getFollowUpRecordPage(new CrmFollowUpRecordPageReqVO());

        assertEquals(Integer.valueOf(0), result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(0, result.getData().getList().get(0).getContacts().size());
        assertEquals(0, result.getData().getList().get(0).getBusinesses().size());
    }
}
