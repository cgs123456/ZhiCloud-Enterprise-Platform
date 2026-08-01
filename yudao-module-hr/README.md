# yudao-module-hr

> 人力资源管理（Human Resources）模块：覆盖员工档案、部门、岗位、考勤、绩效、薪酬等企业级 HR 管控能力。

## 1. 模块简介

`yudao-module-hr` 是 yudao 项目的业务模块（Spring Modulith 声明），提供一套面向中小企业的核心人力资源管理能力：

- 以**员工档案**为中心，串联部门、岗位、考勤、绩效、薪酬
- 薪酬支持月度核算（基本工资 + 加班费 + 奖金 - 扣款 - 社保 - 公积金 - 个税 = 实发工资）
- 考勤与薪酬联动：加班时长自动计入加班费
- 岗位基本工资作为薪酬核算基准
- 作为 MVP，当前不含招聘、培训、社保申报等完整流程

模块所有表均继承 yudao 框架 `TenantBaseDO`，支持多租户隔离。

## 2. 核心能力

| 能力 | 说明 |
|---|---|
| 员工档案 | 员工全生命周期管理：入职、调岗、离职，含工号、身份证、联系方式、用工类型 |
| 部门管理 | 树形部门结构（parentId，根节点 0），支持部门负责人、子部门/员工占用校验 |
| 岗位管理 | 岗位编码、职级、所属部门、基本工资（作为薪酬核算基准） |
| 考勤管理 | 按日记录签到/签退、考勤状态、加班时长；支持月度汇总（用于薪酬核算） |
| 绩效管理 | 按周期（月度 yyyyMM / 季度 yyyyQn）记录考核得分、等级、考核人意见 |
| 薪酬管理 | 月度核算（自动计算加班费、个税）、草稿→审核→发放状态流转 |

## 3. 模块结构

```
yudao-module-hr/
├── pom.xml                        # 模块依赖声明
└── src/main/java/cn/zhicloud/yudao/module/hr/
    ├── package-info.java          # Spring Modulith 模块声明（HR 人力资源管理模块）
    ├── controller/admin/
    │   ├── employee/              # 员工档案 CRUD + 调岗(HrEmployeeTransferReqVO) + 离职(HrEmployeeResignReqVO)
    │   ├── department/             # 部门 CRUD（树形）+ 简单列表（HrDepartmentSimpleRespVO）
    │   ├── position/              # 岗位 CRUD
    │   ├── attendance/            # 考勤 CRUD + 月度汇总（HrAttendanceMonthlySummaryRespVO）
    │   ├── performance/           # 绩效 CRUD + 部门排名（HrPerformanceDeptRankingRespVO）
    │   └── salary/                # 薪资 CRUD + 月度核算(HrSalaryCalculateReqVO) + 审核(HrSalaryApproveReqVO)
    ├── service/
    │   ├── employee/              # HrEmployeeService（含调岗/离职）
    │   ├── department/             # HrDepartmentService（含子部门/员工占用校验）
    │   ├── position/              # HrPositionService
    │   ├── attendance/            # HrAttendanceService（含 getMonthlySummary 供薪酬调用）
    │   ├── performance/           # HrPerformanceService（含部门排名）
    │   └── salary/                # HrSalaryService（含 calculateMonthlySalary / approveSalary）
    ├── dal/
    │   ├── mysql/                 # 各业务 Mapper（继承 BaseMapperX）
    │   └── dataobject/             # HrEmployeeDO / HrDepartmentDO / HrPositionDO / HrAttendanceDO / HrSalaryDO / HrPerformanceDO
    ├── enums/
    │   ├── ErrorCodeConstants.java     # 错误码（1-050-xxx-xxx 段）
    │   ├── DictTypeConstants.java       # 字典类型常量
    │   ├── employee/                    # HrEmployeeStatusEnum / HrEmploymentTypeEnum / HrGenderEnum
    │   ├── department/                  # HrDepartmentStatusEnum
    │   ├── position/                    # HrPositionLevelEnum
    │   ├── attendance/                 # HrAttendanceStatusEnum
    │   ├── performance/               # HrPerformanceGradeEnum
    │   └── salary/                    # HrSalaryStatusEnum
    └── framework/web/config/
        └── HrWebConfiguration.java     # Web 配置
```

> 控制器位于 `controller.admin` 包下，自动继承 yudao 框架的 `/admin-api` URL 前缀，所有写操作均需 `@PreAuthorize` 权限校验。

## 4. 枚举说明

> 所有枚举值与 `sql/mysql/hr.sql` 中字段的 TINYINT 数值保持一致，并在 `DictTypeConstants` 中定义对应字典类型。

### 4.1 员工状态 `HrEmployeeStatusEnum`

| 值 | 名称 | 说明 |
|---|---|---|
| 10 | ACTIVE | 在职 |
| 20 | RESIGNED | 离职 |
| 30 | SUSPENDED | 停薪 |

### 4.2 用工类型 `HrEmploymentTypeEnum`

| 值 | 名称 |
|---|---|
| 10 | FULL_TIME（全职） |
| 20 | PART_TIME（兼职） |
| 30 | INTERN（实习） |
| 40 | OUTSOURCE（外包） |

### 4.3 性别 `HrGenderEnum`

| 值 | 名称 |
|---|---|
| 10 | MALE（男） |
| 20 | FEMALE（女） |

### 4.4 部门状态 `HrDepartmentStatusEnum`

| 值 | 名称 |
|---|---|
| 10 | ENABLE（启用） |
| 20 | DISABLE（禁用） |

### 4.5 岗位职级 `HrPositionLevelEnum`

| 值 | 名称 |
|---|---|
| 10 | JUNIOR（初级） |
| 20 | MIDDLE（中级） |
| 30 | SENIOR（高级） |
| 40 | EXPERT（专家） |
| 50 | MANAGER（管理） |

### 4.6 考勤状态 `HrAttendanceStatusEnum`

| 值 | 名称 | 说明 |
|---|---|---|
| 10 | NORMAL | 正常 |
| 20 | LATE | 迟到 |
| 30 | EARLY_LEAVE | 早退 |
| 40 | ABSENT | 缺勤 |
| 50 | OVERTIME | 加班 |

### 4.7 绩效等级 `HrPerformanceGradeEnum`

| 值 | 名称 |
|---|---|
| 10 | A |
| 20 | B |
| 30 | C |
| 40 | D |

### 4.8 薪资状态 `HrSalaryStatusEnum`

| 值 | 名称 | 说明 |
|---|---|---|
| 10 | DRAFT | 草稿（可修改、可审核） |
| 20 | APPROVED | 已审核（不可修改） |
| 30 | PAID | 已发放 |

## 5. 数据表

> 建表脚本：`sql/mysql/hr.sql`，所有表均含 `creator/create_time/updater/update_time/deleted/tenant_id` 字段（继承 yudao 框架 `TenantBaseDO`）。

### 5.1 `hr_employee` 员工档案表

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT | 主键 |
| `emp_no` | VARCHAR | 工号（唯一） |
| `name` | VARCHAR | 姓名 |
| `gender` | TINYINT | 性别（10/20） |
| `birth_date` | DATE | 出生日期 |
| `id_card` | VARCHAR | 身份证号 |
| `phone` | VARCHAR | 联系电话 |
| `email` | VARCHAR | 邮箱 |
| `dept_id` | BIGINT | 部门 ID（关联 `hr_department.id`） |
| `position_id` | BIGINT | 岗位 ID（关联 `hr_position.id`） |
| `hire_date` | DATE | 入职日期 |
| `leave_date` | DATE | 离职日期 |
| `status` | TINYINT | 状态（10/20/30） |
| `employment_type` | TINYINT | 用工类型（10/20/30/40） |

### 5.2 `hr_department` 部门表（树形）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT | 主键 |
| `parent_id` | BIGINT | 父部门 ID，根节点为 0 |
| `code` | VARCHAR | 部门编码（唯一） |
| `name` | VARCHAR | 部门名称 |
| `leader_id` | BIGINT | 部门负责人（员工 ID） |
| `status` | TINYINT | 状态（10/20） |
| `sort` | INT | 排序 |

### 5.3 `hr_position` 岗位表

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT | 主键 |
| `code` | VARCHAR | 岗位编码（唯一） |
| `name` | VARCHAR | 岗位名称 |
| `dept_id` | BIGINT | 所属部门 ID |
| `level` | TINYINT | 职级（10/20/30/40/50） |
| `base_salary` | DECIMAL | 基本工资（薪酬核算基准） |

### 5.4 `hr_attendance` 考勤记录表

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT | 主键 |
| `employee_id` | BIGINT | 员工 ID |
| `attendance_date` | DATE | 考勤日期 |
| `check_in_time` | DATETIME | 签到时间 |
| `check_out_time` | DATETIME | 签退时间 |
| `status` | TINYINT | 状态（10/20/30/40/50） |
| `overtime_hours` | DECIMAL | 加班时长（小时，计入薪酬） |

### 5.5 `hr_performance` 绩效记录表

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT | 主键 |
| `employee_id` | BIGINT | 员工 ID |
| `period` | VARCHAR | 考核周期（yyyyMM 月度 / yyyyQn 季度） |
| `score` | DECIMAL | 考核得分 |
| `grade` | TINYINT | 等级（10/20/30/40） |
| `evaluator_id` | BIGINT | 考核人 ID |
| `evaluation_date` | DATE | 考核日期 |
| `comment` | TEXT | 考核意见 |

### 5.6 `hr_salary` 薪资记录表

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT | 主键 |
| `employee_id` | BIGINT | 员工 ID |
| `salary_month` | VARCHAR | 薪资月份（yyyyMM） |
| `base_salary` | DECIMAL | 基本工资 |
| `overtime_pay` | DECIMAL | 加班费 |
| `bonus` | DECIMAL | 奖金 |
| `deduction` | DECIMAL | 扣款 |
| `social_insurance` | DECIMAL | 社保 |
| `housing_fund` | DECIMAL | 公积金 |
| `tax` | DECIMAL | 个税 |
| `net_salary` | DECIMAL | 实发工资 |
| `status` | TINYINT | 状态（10/20/30） |

## 6. 薪酬核算逻辑

`HrSalaryServiceImpl.calculateMonthlySalary` 实现月度薪资核算，公式如下：

```
加班费 = 加班时长 × (基本工资 / 21.75 / 8) × 1.5
应纳税所得额 = 基本工资 + 加班费 + 奖金 - 扣款 - 社保 - 公积金
个税 = 分级累进（起征点 5000，税率 3%/10%/20%/25%/30%）
实发工资 = 应纳税所得额 - 个税
```

其中：
- **基本工资**取自员工所属岗位（`hr_position.base_salary`）
- **加班时长**取自考勤月度汇总（`HrAttendanceService.getMonthlySummary`）
- **21.75** 为月计薪天数（劳动法标准），**8** 为日工作时长，**1.5** 为工作日延时加班倍率
- 个税采用月度简化版分级累进（实际应使用累计预扣预缴法）

核算结果以草稿状态（DRAFT）写入 `hr_salary`，经 `approveSalary` 审核后变为 APPROVED，不可再修改。

## 7. 与其他模块关系

### 7.1 与 `yudao-module-erp`（计划联动）

薪酬审核通过后，应生成 ERP 总账凭证（应付职工薪酬 / 银行存款 / 管理费用等科目）：

```
HR 薪资审核（APPROVED） → 调用 erp 模块 → 生成总账凭证 → 过账
```

当前版本为 MVP，**凭证生成尚未实现**，预留接口扩展点。后续可通过：
1. 在 `HrSalaryServiceImpl.approveSalary` 审核通过后发布事件
2. ERP 模块监听事件，调用 `ErpGlService` 生成薪酬凭证

### 7.2 与 `yudao-module-system`（用户体系）

员工档案 `hr_employee` 可关联系统用户 `system_users`（通过 userId 字段，当前 MVP 未实现），实现：
- 员工自助查询考勤/薪资
- 登录账号与员工身份绑定

### 7.3 与 `yudao-module-infra`（基础设施）

复用 yudao 框架能力：
- 多租户（`yudao-spring-boot-starter-biz-tenant`）
- 数据权限、操作日志、Excel 导入导出

## 8. 未来扩展方向

| 方向 | 说明 |
|---|---|
| 招聘管理 | 招聘需求、候选人、面试流程、Offer 发放 |
| 培训管理 | 培训计划、课程、参训记录、培训效果评估 |
| 社保申报 | 五险一金基数管理、社保申报、公积金对账 |
| 薪酬凭证联动 | 对接 `yudao-module-erp` 总账，自动生成薪酬凭证 |
| 员工自助 | 对接 `system_users`，员工自助查询考勤/薪资/绩效 |
| 考勤机对接 | 对接钉钉/企业微信/指纹考勤机，自动导入考勤数据 |
| 组织架构图 | 可视化部门树形结构、岗位编制 |

## 9. 错误码段

使用 `1-050-xxx-xxx` 段：

| 段 | 说明 |
|---|---|
| `1_050_000_000` ~ `1_050_000_002` | 员工档案（不存在 / 工号重复 / 已离职） |
| `1_050_001_000` ~ `1_050_001_003` | 部门（不存在 / 编码重复 / 存在子部门 / 存在员工） |
| `1_050_002_000` ~ `1_050_002_001` | 岗位（不存在 / 编码重复） |
| `1_050_003_000` | 考勤（不存在） |
| `1_050_004_000` ~ `1_050_004_002` | 薪资（不存在 / 状态非法 / 已审核） |
| `1_050_005_000` | 绩效（不存在） |

## 10. 参考链接

- 劳动法月计薪天数（21.75 天）：http://www.mohrss.gov.cn/
- 个人所得税累计预扣预缴法：https://www.chinatax.gov.cn/
- yudao-module-erp（企业资源计划）：见项目根目录
- yudao-module-system（用户体系）：见项目根目录
