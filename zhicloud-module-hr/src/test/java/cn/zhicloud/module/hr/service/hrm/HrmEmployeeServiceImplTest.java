package cn.zhicloud.module.hr.service.hrm;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.hr.controller.admin.hrm.vo.HrmEmployeeCancelQuitReqVO;
import cn.zhicloud.module.hr.controller.admin.hrm.vo.HrmEmployeeChangeReqVO;
import cn.zhicloud.module.hr.controller.admin.hrm.vo.HrmEmployeeQuitReqVO;
import cn.zhicloud.module.hr.controller.admin.hrm.vo.HrmEmployeeSaveReqVO;
import cn.zhicloud.module.hr.dal.dataobject.employee.HrEmployeeDO;
import cn.zhicloud.module.hr.dal.dataobject.hrm.HrmEmployeeChangeRecordDO;
import cn.zhicloud.module.hr.dal.dataobject.hrm.HrmEmployeeQuitInfoDO;
import cn.zhicloud.module.hr.dal.mysql.employee.HrEmployeeMapper;
import cn.zhicloud.module.hr.dal.mysql.hrm.HrmEmployeeChangeRecordMapper;
import cn.zhicloud.module.hr.dal.mysql.hrm.HrmEmployeeQuitInfoMapper;
import cn.zhicloud.module.system.api.dept.DeptApi;
import cn.zhicloud.module.system.api.notify.NotifyMessageSendApi;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link HrmEmployeeServiceImpl} 的单元测试（HRM 融合链路）
 *
 * @author zhicloud
 */
@Import(HrmEmployeeServiceImpl.class)
public class HrmEmployeeServiceImplTest extends BaseDbUnitTest {

    @Resource
    private HrmEmployeeServiceImpl employeeService;

    @Resource
    private HrEmployeeMapper employeeMapper;
    @Resource
    private HrmEmployeeChangeRecordMapper changeRecordMapper;
    @Resource
    private HrmEmployeeQuitInfoMapper quitInfoMapper;

    @MockitoBean
    private DeptApi deptApi;
    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private NotifyMessageSendApi notifyMessageSendApi;

    @Test
    public void test_createEmployee_success() {
        // 准备参数（HRM 新轴口径）
        HrmEmployeeSaveReqVO reqVO = new HrmEmployeeSaveReqVO();
        reqVO.setName("张三");
        reqVO.setJobNumber("EMP001");
        reqVO.setSex(1);
        reqVO.setEntryStatus(1);
        reqVO.setType(1);
        reqVO.setStatus(1);
        reqVO.setEntryTime(LocalDateTime.of(2026, 1, 1, 9, 0));

        // 调用
        Long id = employeeService.createEmployee(reqVO);

        // 校验：新轴落库（旧镜像列已于 V208 退役，不再断言）
        HrEmployeeDO employee = employeeMapper.selectById(id);
        assertNotNull(employee);
        assertEquals("EMP001", employee.getEmpNo());
        assertEquals(Integer.valueOf(1), employee.getEntryStatus());
        assertEquals(Integer.valueOf(1), employee.getEmployeeStatus());
        assertEquals(Integer.valueOf(1), employee.getType());
        // 正式员工转正时间兜底 = 入职时间；司龄起点 = 入职时间
        assertEquals(employee.getEntryTime(), employee.getRegularTime());
        assertEquals(employee.getEntryTime(), employee.getCompanyAgeStartTime());
    }

    @Test
    public void test_createEmployee_jobNumberRequired() {
        HrmEmployeeSaveReqVO reqVO = new HrmEmployeeSaveReqVO();
        reqVO.setName("李四");
        reqVO.setEntryStatus(1); // 在职必须有工号
        // 调用 + 校验
        assertException(() -> employeeService.createEmployee(reqVO),
                1_050_015_001, "在职员工的工号不能为空");
    }

    @Test
    public void test_createEmployee_duplicateJobNumber() {
        HrEmployeeDO exist = randomPojo(HrEmployeeDO.class, o -> {
            o.setEmpNo("EMP002");
        });
        employeeMapper.insert(exist);
        HrmEmployeeSaveReqVO reqVO = new HrmEmployeeSaveReqVO();
        reqVO.setName("王五");
        reqVO.setJobNumber("EMP002");
        assertException(() -> employeeService.createEmployee(reqVO),
                1_050_015_002, "工号已存在");
    }

    @Test
    public void test_quitEmployee_writesQuitInfo() {
        // 准备：在职员工
        HrmEmployeeSaveReqVO createVO = new HrmEmployeeSaveReqVO();
        createVO.setName("赵六");
        createVO.setJobNumber("EMP003");
        createVO.setEntryStatus(1);
        createVO.setType(1);
        createVO.setStatus(1);
        createVO.setEntryTime(LocalDateTime.of(2025, 6, 1, 9, 0));
        Long id = employeeService.createEmployee(createVO);

        // 调用：离职
        HrmEmployeeQuitReqVO quitVO = new HrmEmployeeQuitReqVO();
        quitVO.setEmployeeId(id);
        quitVO.setPlanQuitTime(LocalDateTime.of(2026, 10, 11, 18, 0));
        quitVO.setType(1);
        quitVO.setReason(2);
        employeeService.quitEmployee(quitVO);

        // 校验：主表新轴
        HrEmployeeDO employee = employeeMapper.selectById(id);
        assertEquals(Integer.valueOf(4), employee.getEntryStatus());
        assertNotNull(employee.getLeaveTime());
        // 校验：离职信息表落行
        HrmEmployeeQuitInfoDO quitInfo = quitInfoMapper.selectByEmployeeId(id);
        assertNotNull(quitInfo);
        assertEquals(Integer.valueOf(1), quitInfo.getType());
        assertEquals(Integer.valueOf(2), quitInfo.getReason());
        assertEquals(Integer.valueOf(1), quitInfo.getOldEmployeeStatus());
    }

    @Test
    public void test_cancelQuit_clearsLeave() {
        // 准备：离职员工
        HrEmployeeDO left = randomPojo(HrEmployeeDO.class, o -> {
            o.setEntryStatus(4);
            o.setLeaveTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        });
        employeeMapper.insert(left);

        // 调用：取消离职
        HrmEmployeeCancelQuitReqVO reqVO = new HrmEmployeeCancelQuitReqVO();
        reqVO.setEmployeeId(left.getId());
        reqVO.setReason("员工撤销离职申请");
        employeeService.cancelQuitEmployee(reqVO);

        // 校验：回在职且离职时间清空
        HrEmployeeDO employee = employeeMapper.selectById(left.getId());
        assertEquals(Integer.valueOf(1), employee.getEntryStatus());
        assertNull(employee.getLeaveTime());
    }

    @Test
    public void test_transferEmployee_writesChangeRecord() {
        // 准备：在职员工
        HrmEmployeeSaveReqVO createVO = new HrmEmployeeSaveReqVO();
        createVO.setName("孙七");
        createVO.setJobNumber("EMP004");
        createVO.setEntryStatus(1);
        createVO.setType(1);
        createVO.setStatus(1);
        createVO.setPostName("软件工程师");
        Long id = employeeService.createEmployee(createVO);

        // 调用：调岗
        HrmEmployeeChangeReqVO changeVO = new HrmEmployeeChangeReqVO();
        changeVO.setEmployeeId(id);
        changeVO.setReason(1);
        changeVO.setNewPostName("高级软件工程师");
        changeVO.setNewPostLevel("P7");
        employeeService.transferEmployee(changeVO);

        // 校验：主表变更
        HrEmployeeDO employee = employeeMapper.selectById(id);
        assertEquals("高级软件工程师", employee.getPostName());
        assertEquals("P7", employee.getPostLevel());
        // 校验：异动记录落行（type=5，含新旧快照）
        List<HrmEmployeeChangeRecordDO> records = changeRecordMapper.selectListByEmployeeId(id);
        assertEquals(1, records.size());
        HrmEmployeeChangeRecordDO record = records.get(0);
        assertEquals(HrmEmployeeChangeRecordDO.TYPE_TRANSFER, record.getType());
        assertEquals("软件工程师", record.getOldPostName());
        assertEquals("高级软件工程师", record.getNewPostName());
    }

    @Test
    public void test_deleteEmployee_releasesJobNumber() {
        // 准备：离职员工（软删会撞 uk_emp_no 物理唯一索引，删除时必须释放工号）
        HrEmployeeDO left = randomPojo(HrEmployeeDO.class, o -> {
            o.setEmpNo("EMP-DEL-001");
            o.setEntryStatus(4);
        });
        employeeMapper.insert(left);

        // 调用：删除
        employeeService.deleteEmployee(left.getId());

        // 校验：软删后正常查询不可见
        assertNull(employeeMapper.selectById(left.getId()));

        // 同工号可再建档（若未释放 uk_emp_no 槽位，此处将撞唯一索引）
        HrmEmployeeSaveReqVO reqVO = new HrmEmployeeSaveReqVO();
        reqVO.setName("复用工号");
        reqVO.setJobNumber("EMP-DEL-001");
        reqVO.setEntryStatus(1);
        Long newId = employeeService.createEmployee(reqVO);
        assertNotNull(newId);
    }

    @SuppressWarnings("unchecked")
    private static void assertException(org.junit.jupiter.api.function.Executable executable, int code, String message) {
        cn.zhicloud.framework.common.exception.ServiceException ex =
                assertThrows(cn.zhicloud.framework.common.exception.ServiceException.class, executable);
        assertEquals(code, ex.getCode());
        assertEquals(message, ex.getMessage());
    }

}
