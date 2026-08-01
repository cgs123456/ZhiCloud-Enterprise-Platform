package cn.iocoder.yudao.module.hr.service.employee;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hr.controller.admin.employee.vo.HrEmployeePageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.employee.vo.HrEmployeeSaveReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.employee.HrEmployeeDO;
import cn.iocoder.yudao.module.hr.dal.mysql.employee.HrEmployeeMapper;
import cn.iocoder.yudao.module.hr.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.hr.service.department.HrDepartmentService;
import cn.iocoder.yudao.module.hr.service.position.HrPositionService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.annotation.Resource;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * {@link HrEmployeeServiceImpl} 的单元测试
 *
 * @author 芋道源码
 */
@Import(HrEmployeeServiceImpl.class)
public class HrEmployeeServiceImplTest extends BaseDbUnitTest {

    @Resource
    private HrEmployeeServiceImpl employeeService;

    @Resource
    private HrEmployeeMapper employeeMapper;

    @MockitoBean
    private HrDepartmentService departmentService;
    @MockitoBean
    private HrPositionService positionService;

    @Test
    public void test_createEmployee_success() {
        // 准备参数
        HrEmployeeSaveReqVO reqVO = new HrEmployeeSaveReqVO();
        reqVO.setEmpNo("EMP001");
        reqVO.setName("张三");
        reqVO.setGender(1);
        reqVO.setStatus(1);

        // 调用
        Long id = employeeService.createEmployee(reqVO);

        // 校验
        HrEmployeeDO employee = employeeMapper.selectById(id);
        assertNotNull(employee);
        assertEquals("EMP001", employee.getEmpNo());
        assertEquals("张三", employee.getName());
    }

    @Test
    public void test_createEmployee_duplicateEmpNo() {
        // mock 数据
        HrEmployeeDO existEmployee = randomPojo(HrEmployeeDO.class, o -> {
            o.setEmpNo("EMP002");
        });
        employeeMapper.insert(existEmployee);

        // 准备参数
        HrEmployeeSaveReqVO reqVO = new HrEmployeeSaveReqVO();
        reqVO.setEmpNo("EMP002");
        reqVO.setName("李四");

        // 调用并校验异常
        assertThrows(Exception.class, () -> employeeService.createEmployee(reqVO));
    }

    @Test
    public void test_updateEmployee_success() {
        // mock 数据
        HrEmployeeDO employee = randomPojo(HrEmployeeDO.class, o -> {
            o.setEmpNo("EMP003");
        });
        employeeMapper.insert(employee);

        // 准备参数
        HrEmployeeSaveReqVO reqVO = new HrEmployeeSaveReqVO();
        reqVO.setId(employee.getId());
        reqVO.setEmpNo("EMP003");
        reqVO.setName("张三改");
        reqVO.setGender(1);
        reqVO.setStatus(1);

        // 调用
        employeeService.updateEmployee(reqVO);

        // 校验
        HrEmployeeDO updated = employeeMapper.selectById(employee.getId());
        assertEquals("张三改", updated.getName());
    }

    @Test
    public void test_deleteEmployee_success() {
        // mock 数据
        HrEmployeeDO employee = randomPojo(HrEmployeeDO.class);
        employeeMapper.insert(employee);

        // 调用
        employeeService.deleteEmployee(employee.getId());

        // 校验
        assertNull(employeeMapper.selectById(employee.getId()));
    }

    @Test
    public void test_deleteEmployee_notExists() {
        // 调用并校验异常
        assertThrows(Exception.class, () -> employeeService.deleteEmployee(randomLongId()));
    }

    @Test
    public void test_getEmployee_success() {
        // mock 数据
        HrEmployeeDO employee = randomPojo(HrEmployeeDO.class);
        employeeMapper.insert(employee);

        // 调用并校验
        HrEmployeeDO result = employeeService.getEmployee(employee.getId());
        assertNotNull(result);
        assertEquals(employee.getId(), result.getId());
    }

    @Test
    public void test_getEmployeePage_success() {
        // mock 数据
        HrEmployeeDO employee = randomPojo(HrEmployeeDO.class, o -> {
            o.setName("测试员工");
        });
        employeeMapper.insert(employee);

        // 准备参数
        HrEmployeePageReqVO reqVO = new HrEmployeePageReqVO();
        reqVO.setName("测试员工");
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        // 调用并校验
        var pageResult = employeeService.getEmployeePage(reqVO);
        assertTrue(pageResult.getList().size() > 0);
    }

}
