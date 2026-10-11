package cn.zhicloud.module.hr.service.hrm.insurance;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo.HrmInsuranceEmployeeInfoVO;
import cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo.HrmInsuranceEmployeeUpdateSchemeReqVO;
import cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo.InsuranceSchemeProjectVO;
import cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo.InsuranceSchemeVO;
import cn.zhicloud.module.hr.dal.dataobject.employee.HrEmployeeDO;
import cn.zhicloud.module.hr.dal.dataobject.insurance.HrmInsuranceEmployeeInfoDO;
import cn.zhicloud.module.hr.dal.dataobject.insurance.HrmInsuranceSchemeDO;
import cn.zhicloud.module.hr.dal.dataobject.insurance.HrmInsuranceSchemeProjectDO;
import cn.zhicloud.module.hr.dal.mysql.employee.HrEmployeeMapper;
import cn.zhicloud.module.hr.dal.mysql.insurance.HrmInsuranceEmployeeInfoMapper;
import cn.zhicloud.module.hr.dal.mysql.insurance.HrmInsuranceSchemeMapper;
import cn.zhicloud.module.hr.dal.mysql.insurance.HrmInsuranceSchemeProjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link HrmInsuranceSchemeServiceImpl} / {@link HrmInsuranceEmployeeInfoServiceImpl} 单元测试
 *
 * @author zhicloud
 */
@Import({HrmInsuranceSchemeServiceImpl.class, HrmInsuranceEmployeeInfoServiceImpl.class})
public class HrmInsuranceServiceImplTest extends BaseDbUnitTest {

    @Resource
    private HrmInsuranceSchemeServiceImpl schemeService;
    @Resource
    private HrmInsuranceEmployeeInfoServiceImpl employeeInfoService;

    @Resource
    private HrmInsuranceSchemeMapper schemeMapper;
    @Resource
    private HrmInsuranceSchemeProjectMapper schemeProjectMapper;
    @Resource
    private HrmInsuranceEmployeeInfoMapper employeeInfoMapper;
    @Resource
    private HrEmployeeMapper employeeMapper;

    private InsuranceSchemeProjectVO project(int type, String name, String base, String corpRate, String persRate) {
        InsuranceSchemeProjectVO vo = new InsuranceSchemeProjectVO();
        vo.setType(type);
        vo.setName(name);
        vo.setBaseAmount(new BigDecimal(base));
        vo.setCorporateRate(new BigDecimal(corpRate));
        vo.setPersonalRate(new BigDecimal(persRate));
        return vo;
    }

    @Test
    public void test_createScheme_proportionMode_computesAmounts() {
        InsuranceSchemeVO reqVO = new InsuranceSchemeVO();
        reqVO.setName("深圳城镇职工方案");
        reqVO.setAreaId(440300L);
        reqVO.setType(1); // 比例
        reqVO.setProjectList(Arrays.asList(
                project(1, "养老保险", "5000", "0.16", "0.08"),
                project(2, "医疗保险", "5000", "0.06", "0.02"),
                project(10, "公积金", "5000", "0.12", "0.05")));

        Long id = schemeService.createScheme(reqVO);

        HrmInsuranceSchemeDO scheme = schemeMapper.selectById(id);
        assertNotNull(scheme);
        // 社保组（1/2）：个人 0.08+0.02=0.10→500，公司 0.16+0.06=0.22→1100
        assertEquals(0, new BigDecimal("500.0000").compareTo(scheme.getPersonalInsuranceAmount()));
        assertEquals(0, new BigDecimal("1100.0000").compareTo(scheme.getCorporateInsuranceAmount()));
        // 公积金组（10）：个人 0.05→250，公司 0.12→600
        assertEquals(0, new BigDecimal("250.0000").compareTo(scheme.getPersonalProvidentFundAmount()));
        assertEquals(0, new BigDecimal("600.0000").compareTo(scheme.getCorporateProvidentFundAmount()));
        // 项目 3 行落库
        List<HrmInsuranceSchemeProjectDO> rows = schemeProjectMapper.selectListBySchemeId(id);
        assertEquals(3, rows.size());
    }

    @Test
    public void test_getScheme_splitsProjectGroups() {
        InsuranceSchemeVO reqVO = new InsuranceSchemeVO();
        reqVO.setName("分组方案");
        reqVO.setAreaId(440300L);
        reqVO.setType(1);
        reqVO.setProjectList(Arrays.asList(
                project(1, "养老保险", "5000", "0.16", "0.08"),
                project(10, "公积金", "5000", "0.12", "0.05"),
                project(11, "公积金自定义", "3000", "0.10", "0.05")));

        Long id = schemeService.createScheme(reqVO);
        InsuranceSchemeVO detail = schemeService.getScheme(id);

        assertEquals(3, detail.getProjectList().size());
        assertEquals(1, detail.getSocialSecurityProjectList().size());
        assertEquals(2, detail.getProvidentFundProjectList().size());
        assertEquals("广东省 深圳市", detail.getAreaName());
    }

    @Test
    public void test_createScheme_duplicateName() {
        InsuranceSchemeVO first = new InsuranceSchemeVO();
        first.setName("重名方案");
        first.setAreaId(440300L);
        first.setType(1);
        first.setProjectList(Arrays.asList(project(1, "养老保险", "5000", "0.16", "0.08")));
        schemeService.createScheme(first);

        InsuranceSchemeVO dup = new InsuranceSchemeVO();
        dup.setName("重名方案");
        dup.setAreaId(440300L);
        dup.setType(1);
        dup.setProjectList(Arrays.asList(project(1, "养老保险", "5000", "0.16", "0.08")));
        cn.zhicloud.framework.common.exception.ServiceException ex =
                assertThrows(cn.zhicloud.framework.common.exception.ServiceException.class,
                        () -> schemeService.createScheme(dup));
        assertEquals(1_050_017_001, ex.getCode());
    }

    @Test
    public void test_updateScheme_replacesProjects() {
        InsuranceSchemeVO reqVO = new InsuranceSchemeVO();
        reqVO.setName("待改方案");
        reqVO.setAreaId(440300L);
        reqVO.setType(1);
        reqVO.setProjectList(Arrays.asList(project(1, "养老保险", "5000", "0.16", "0.08")));
        Long id = schemeService.createScheme(reqVO);
        assertEquals(1, schemeProjectMapper.selectListBySchemeId(id).size());

        // 改为 2 个项目（旧行应被替换，不残留）
        InsuranceSchemeVO updateVO = new InsuranceSchemeVO();
        updateVO.setId(id);
        updateVO.setName("待改方案");
        updateVO.setAreaId(440300L);
        updateVO.setType(1);
        updateVO.setProjectList(Arrays.asList(
                project(1, "养老保险", "6000", "0.16", "0.08"),
                project(2, "医疗保险", "6000", "0.06", "0.02")));
        schemeService.updateScheme(updateVO);

        List<HrmInsuranceSchemeProjectDO> rows = schemeProjectMapper.selectListBySchemeId(id);
        assertEquals(2, rows.size());
        assertTrue(rows.stream().allMatch(r -> r.getBaseAmount().compareTo(new BigDecimal("6000")) == 0));
    }

    @Test
    public void test_deleteScheme_usedByEmployee_rejected() {
        InsuranceSchemeVO reqVO = new InsuranceSchemeVO();
        reqVO.setName("被引用方案");
        reqVO.setAreaId(440300L);
        reqVO.setType(1);
        reqVO.setProjectList(Arrays.asList(project(1, "养老保险", "5000", "0.16", "0.08")));
        Long schemeId = schemeService.createScheme(reqVO);

        // 员工引用该方案
        HrEmployeeDO employee = randomPojo(HrEmployeeDO.class, o -> o.setName("参保员工"));
        employeeMapper.insert(employee);
        employeeInfoMapper.insert(HrmInsuranceEmployeeInfoDO.builder()
                .employeeId(employee.getId()).schemeId(schemeId).build());

        cn.zhicloud.framework.common.exception.ServiceException ex =
                assertThrows(cn.zhicloud.framework.common.exception.ServiceException.class,
                        () -> schemeService.deleteScheme(schemeId));
        assertEquals(1_050_017_002, ex.getCode());
    }

    @Test
    public void test_saveEmployeeInfo_mirrorsAccountsToEmployee() {
        HrEmployeeDO employee = randomPojo(HrEmployeeDO.class, o -> o.setName("账号镜像员工"));
        employeeMapper.insert(employee);

        HrmInsuranceEmployeeInfoVO reqVO = new HrmInsuranceEmployeeInfoVO();
        reqVO.setEmployeeId(employee.getId());
        reqVO.setFirstSocialSecurity(true);
        reqVO.setFirstAccumulationFund(false);
        reqVO.setSocialSecurityNumber("SB-440300-001");
        reqVO.setAccumulationFundNumber("GJJ-440300-001");

        Long id = employeeInfoService.saveEmployeeInfo(reqVO);

        // 参保特有字段落 hr_insurance_employee_info
        HrmInsuranceEmployeeInfoDO info = employeeInfoMapper.selectById(id);
        assertNotNull(info);
        assertEquals(true, info.getFirstSocialSecurity());
        // 账号镜像 hr_employee 两列（单一真相）
        HrEmployeeDO updated = employeeMapper.selectById(employee.getId());
        assertEquals("SB-440300-001", updated.getSocialSecurityNumber());
        assertEquals("GJJ-440300-001", updated.getAccumulationFundNumber());

        // 回读 enrich
        HrmInsuranceEmployeeInfoVO read = employeeInfoService.getEmployeeInfo(employee.getId());
        assertEquals("SB-440300-001", read.getSocialSecurityNumber());
        assertEquals("GJJ-440300-001", read.getAccumulationFundNumber());
    }

    @Test
    public void test_updateEmployeeScheme_createsInfoIfAbsent() {
        HrEmployeeDO employee = randomPojo(HrEmployeeDO.class, o -> o.setName("设方案员工"));
        employeeMapper.insert(employee);
        InsuranceSchemeVO schemeVO = new InsuranceSchemeVO();
        schemeVO.setName("目标方案");
        schemeVO.setAreaId(440300L);
        schemeVO.setType(1);
        schemeVO.setProjectList(Arrays.asList(project(1, "养老保险", "5000", "0.16", "0.08")));
        Long schemeId = schemeService.createScheme(schemeVO);

        HrmInsuranceEmployeeUpdateSchemeReqVO reqVO = new HrmInsuranceEmployeeUpdateSchemeReqVO();
        reqVO.setEmployeeId(employee.getId());
        reqVO.setSchemeId(schemeId);
        employeeInfoService.updateEmployeeScheme(reqVO);

        HrmInsuranceEmployeeInfoDO info = employeeInfoMapper.selectByEmployeeId(employee.getId());
        assertNotNull(info);
        assertEquals(schemeId, info.getSchemeId());
    }

}
