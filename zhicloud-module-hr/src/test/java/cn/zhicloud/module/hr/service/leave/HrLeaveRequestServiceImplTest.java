package cn.zhicloud.module.hr.service.leave;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.hr.controller.admin.leave.vo.HrLeaveRequestPageReqVO;
import cn.zhicloud.module.hr.controller.admin.leave.vo.HrLeaveRequestSaveReqVO;
import cn.zhicloud.module.hr.dal.dataobject.leave.HrLeaveRequestDO;
import cn.zhicloud.module.hr.dal.dataobject.leave.HrLeaveTypeDO;
import cn.zhicloud.module.hr.dal.mysql.leave.HrLeaveRequestMapper;
import cn.zhicloud.module.hr.dal.mysql.leave.HrLeaveTypeMapper;
import cn.zhicloud.module.hr.service.employee.HrEmployeeService;
import cn.zhicloud.module.hr.service.leave.HrLeaveTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.annotation.Resource;
import java.math.BigDecimal;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link HrLeaveRequestServiceImpl} 的单元测试
 *
 * @author 智云
 */
@Import(HrLeaveRequestServiceImpl.class)
public class HrLeaveRequestServiceImplTest extends BaseDbUnitTest {

    @Resource
    private HrLeaveRequestServiceImpl leaveRequestService;

    @Resource
    private HrLeaveRequestMapper leaveRequestMapper;
    @Resource
    private HrLeaveTypeMapper leaveTypeMapper;

    @MockitoBean
    private HrEmployeeService employeeService;
    @MockitoBean
    private HrLeaveTypeService leaveTypeService;

    @Test
    public void test_createLeaveRequest_success() {
        // 准备关联的请假类型（createLeaveRequest 通过 leaveTypeMapper 校验存在）
        HrLeaveTypeDO leaveType = randomPojo(HrLeaveTypeDO.class, o -> {
            o.setId(1L);
            o.setIsPaid(0); // 非带薪，跳过余额校验
        });
        leaveTypeMapper.insert(leaveType);

        // 准备参数
        HrLeaveRequestSaveReqVO reqVO = new HrLeaveRequestSaveReqVO();
        reqVO.setEmployeeId(1L);
        reqVO.setLeaveTypeId(1L);
        reqVO.setDays(new BigDecimal("2.0"));
        reqVO.setReason("个人事务");

        // 调用
        Long id = leaveRequestService.createLeaveRequest(reqVO);

        // 校验
        HrLeaveRequestDO result = leaveRequestMapper.selectById(id);
        assertNotNull(result);
        assertEquals(0, new BigDecimal("2.0").compareTo(result.getDays()));
        assertEquals("个人事务", result.getReason());
    }

    @Test
    public void test_cancelLeaveRequest_success() {
        // mock 数据
        HrLeaveRequestDO leaveRequest = randomPojo(HrLeaveRequestDO.class, o -> {
            o.setStatus(0); // 待审批
        });
        leaveRequestMapper.insert(leaveRequest);

        // 调用
        leaveRequestService.cancelLeaveRequest(leaveRequest.getId());

        // 校验
        HrLeaveRequestDO result = leaveRequestMapper.selectById(leaveRequest.getId());
        assertEquals(3, result.getStatus()); // 已取消 CANCELLED=3
    }

    @Test
    public void test_cancelLeaveRequest_notExists() {
        assertThrows(Exception.class, () -> leaveRequestService.cancelLeaveRequest(randomLongId()));
    }

    @Test
    public void test_getLeaveRequest_success() {
        // mock 数据
        HrLeaveRequestDO leaveRequest = randomPojo(HrLeaveRequestDO.class);
        leaveRequestMapper.insert(leaveRequest);

        // 调用并校验
        HrLeaveRequestDO result = leaveRequestService.getLeaveRequest(leaveRequest.getId());
        assertNotNull(result);
        assertEquals(leaveRequest.getId(), result.getId());
    }

    @Test
    public void test_getLeaveRequestPage_success() {
        // mock 数据
        HrLeaveRequestDO leaveRequest = randomPojo(HrLeaveRequestDO.class, o -> {
            o.setReason("测试请假");
        });
        leaveRequestMapper.insert(leaveRequest);

        // 准备参数
        HrLeaveRequestPageReqVO reqVO = new HrLeaveRequestPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        // 调用并校验
        var pageResult = leaveRequestService.getLeaveRequestPage(reqVO);
        assertTrue(pageResult.getList().size() > 0);
    }

}
