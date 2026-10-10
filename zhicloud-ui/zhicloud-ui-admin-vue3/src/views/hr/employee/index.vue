<!-- HR 员工 -->
<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      ref="queryFormRef"
      :inline="true"
      :model="queryParams"
      class="-mb-15px"
      label-width="100px"
    >
      <el-form-item label="工号" prop="empNo">
        <el-input v-model="queryParams.empNo" class="!w-240px" clearable placeholder="请输入工号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="姓名" prop="name">
        <el-input v-model="queryParams.name" class="!w-240px" clearable placeholder="请输入姓名" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="性别" prop="gender">
        <el-input-number v-model="queryParams.gender" class="!w-240px" :controls="false" clearable placeholder="请输入性别" />
      </el-form-item>
      <el-form-item label="身份证号" prop="idCard">
        <el-input v-model="queryParams.idCard" class="!w-240px" clearable placeholder="请输入身份证号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="联系电话" prop="phone">
        <el-input v-model="queryParams.phone" class="!w-240px" clearable placeholder="请输入联系电话" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="queryParams.email" class="!w-240px" clearable placeholder="请输入邮箱" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="部门 ID" prop="deptId">
        <el-input-number v-model="queryParams.deptId" class="!w-240px" :controls="false" clearable placeholder="请输入部门 ID" />
      </el-form-item>
      <el-form-item label="职位 ID" prop="positionId">
        <el-input-number v-model="queryParams.positionId" class="!w-240px" :controls="false" clearable placeholder="请输入职位 ID" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="用工类型" prop="employmentType">
        <el-input-number v-model="queryParams.employmentType" class="!w-240px" :controls="false" clearable placeholder="请输入用工类型" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="queryParams.remark" class="!w-240px" clearable placeholder="请输入备注" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="queryParams.sort" class="!w-240px" :controls="false" clearable placeholder="请输入排序" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery">
          <Icon class="mr-5px" icon="ep:search" />
          搜索
        </el-button>
        <el-button @click="resetQuery">
          <Icon class="mr-5px" icon="ep:refresh" />
          重置
        </el-button>
        <el-button
          v-hasPermi="['hr:employee:create']"
          plain
          type="primary"
          @click="openForm('create')"
        >
          <Icon class="mr-5px" icon="ep:plus" />
          新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" :stripe="true">
      <el-table-column align="center" label="工号" prop="empNo" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="姓名" prop="name" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="性别" prop="gender" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="出生日期" prop="birthDate" width="170" />
      <el-table-column align="center" label="身份证号" prop="idCard" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="联系电话" prop="phone" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="邮箱" prop="email" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="部门 ID" prop="deptId" min-width="120" />
      <el-table-column align="center" label="职位 ID" prop="positionId" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="入职日期" prop="hireDate" width="170" />
      <el-table-column align="center" :formatter="dateFormatter" label="离职日期" prop="leaveDate" width="170" />
      <el-table-column align="center" label="状态" prop="status" min-width="120" />
      <el-table-column align="center" label="用工类型" prop="employmentType" min-width="120" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="排序" prop="sort" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['hr:employee:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['hr:employee:delete']"
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNo"
      :total="total"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 表单弹窗 -->
  <HrEmployeeForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { HrEmployeeApi } from '@/api/hr/employee'
import HrEmployeeForm from './HrEmployeeForm.vue'

defineOptions({ name: 'HrEmployee' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  empNo: undefined
  name: undefined
  gender: undefined
  birthDate: undefined
  idCard: undefined
  phone: undefined
  email: undefined
  deptId: undefined
  positionId: undefined
  hireDate: undefined
  leaveDate: undefined
  status: undefined
  employmentType: undefined
  remark: undefined
  sort: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await HrEmployeeApi.getHrEmployeePage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = async () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

const formRef = ref()

const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await HrEmployeeApi.deleteHrEmployee(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
