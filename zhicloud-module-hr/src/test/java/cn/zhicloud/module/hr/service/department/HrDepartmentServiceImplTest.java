package cn.zhicloud.module.hr.service.department;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.hr.controller.admin.department.vo.HrDepartmentSaveReqVO;
import cn.zhicloud.module.hr.dal.dataobject.department.HrDepartmentDO;
import cn.zhicloud.module.hr.dal.mysql.department.HrDepartmentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link HrDepartmentServiceImpl} 的单元测试
 *
 * @author 智云
 */
@Import(HrDepartmentServiceImpl.class)
public class HrDepartmentServiceImplTest extends BaseDbUnitTest {

    @Resource
    private HrDepartmentServiceImpl departmentService;

    @Resource
    private HrDepartmentMapper departmentMapper;

    @Test
    public void test_createDepartment_success() {
        // 准备参数
        HrDepartmentSaveReqVO reqVO = new HrDepartmentSaveReqVO();
        reqVO.setCode("DEPT001");
        reqVO.setName("研发部");
        reqVO.setParentId(0L);
        reqVO.setStatus(1);

        // 调用
        Long id = departmentService.createDepartment(reqVO);

        // 校验
        HrDepartmentDO result = departmentMapper.selectById(id);
        assertNotNull(result);
        assertEquals("DEPT001", result.getCode());
        assertEquals("研发部", result.getName());
    }

    @Test
    public void test_createDepartment_duplicateCode() {
        // mock 数据
        HrDepartmentDO existDept = randomPojo(HrDepartmentDO.class, o -> {
            o.setCode("DEPT002");
        });
        departmentMapper.insert(existDept);

        // 准备参数
        HrDepartmentSaveReqVO reqVO = new HrDepartmentSaveReqVO();
        reqVO.setCode("DEPT002");
        reqVO.setName("测试部");
        reqVO.setParentId(0L);
        reqVO.setStatus(1);

        // 调用并校验异常
        assertThrows(Exception.class, () -> departmentService.createDepartment(reqVO));
    }

    @Test
    public void test_updateDepartment_success() {
        // mock 数据
        HrDepartmentDO dept = randomPojo(HrDepartmentDO.class, o -> {
            o.setCode("DEPT003");
            o.setName("旧名称");
        });
        departmentMapper.insert(dept);

        // 准备参数
        HrDepartmentSaveReqVO reqVO = new HrDepartmentSaveReqVO();
        reqVO.setId(dept.getId());
        reqVO.setCode("DEPT003");
        reqVO.setName("新名称");
        reqVO.setParentId(0L);
        reqVO.setStatus(1);

        // 调用
        departmentService.updateDepartment(reqVO);

        // 校验
        HrDepartmentDO result = departmentMapper.selectById(dept.getId());
        assertEquals("新名称", result.getName());
    }

    @Test
    public void test_deleteDepartment_success() {
        // mock 数据
        HrDepartmentDO dept = randomPojo(HrDepartmentDO.class, o -> {
            o.setParentId(0L);
        });
        departmentMapper.insert(dept);

        // 调用
        departmentService.deleteDepartment(dept.getId());

        // 校验
        assertNull(departmentMapper.selectById(dept.getId()));
    }

    @Test
    public void test_deleteDepartment_notExists() {
        assertThrows(Exception.class, () -> departmentService.deleteDepartment(randomLongId()));
    }

    @Test
    public void test_getDepartment_success() {
        // mock 数据
        HrDepartmentDO dept = randomPojo(HrDepartmentDO.class);
        departmentMapper.insert(dept);

        // 调用并校验
        HrDepartmentDO result = departmentService.getDepartment(dept.getId());
        assertNotNull(result);
        assertEquals(dept.getId(), result.getId());
    }

    @Test
    public void test_getChildDepartmentList_success() {
        // mock 父部门
        HrDepartmentDO parent = randomPojo(HrDepartmentDO.class, o -> o.setParentId(0L));
        departmentMapper.insert(parent);
        // mock 子部门
        HrDepartmentDO child = randomPojo(HrDepartmentDO.class, o -> o.setParentId(parent.getId()));
        departmentMapper.insert(child);

        // 调用并校验
        var children = departmentService.getChildDepartmentList(parent.getId());
        assertTrue(children.size() >= 1);
    }

}
