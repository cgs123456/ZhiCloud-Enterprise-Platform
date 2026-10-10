<!-- HR 薪资 -->
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
      <el-form-item label="员工 ID" prop="employeeId">
        <el-input-number v-model="queryParams.employeeId" class="!w-240px" :controls="false" clearable placeholder="请输入员工 ID" />
      </el-form-item>
      <el-form-item label="薪资月份" prop="salaryMonth">
        <el-input v-model="queryParams.salaryMonth" class="!w-240px" clearable placeholder="请输入薪资月份" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="基本工资" prop="baseSalary">
        <el-input-number v-model="queryParams.baseSalary" class="!w-240px" :controls="false" clearable placeholder="请输入基本工资" />
      </el-form-item>
      <el-form-item label="加班费" prop="overtimePay">
        <el-input-number v-model="queryParams.overtimePay" class="!w-240px" :controls="false" clearable placeholder="请输入加班费" />
      </el-form-item>
      <el-form-item label="奖金" prop="bonus">
        <el-input-number v-model="queryParams.bonus" class="!w-240px" :controls="false" clearable placeholder="请输入奖金" />
      </el-form-item>
      <el-form-item label="扣款" prop="deduction">
        <el-input-number v-model="queryParams.deduction" class="!w-240px" :controls="false" clearable placeholder="请输入扣款" />
      </el-form-item>
      <el-form-item label="社保" prop="socialInsurance">
        <el-input-number v-model="queryParams.socialInsurance" class="!w-240px" :controls="false" clearable placeholder="请输入社保" />
      </el-form-item>
      <el-form-item label="公积金" prop="housingFund">
        <el-input-number v-model="queryParams.housingFund" class="!w-240px" :controls="false" clearable placeholder="请输入公积金" />
      </el-form-item>
      <el-form-item label="个税" prop="tax">
        <el-input-number v-model="queryParams.tax" class="!w-240px" :controls="false" clearable placeholder="请输入个税" />
      </el-form-item>
      <el-form-item label="实发工资" prop="netSalary">
        <el-input-number v-model="queryParams.netSalary" class="!w-240px" :controls="false" clearable placeholder="请输入实发工资" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="queryParams.remark" class="!w-240px" clearable placeholder="请输入备注" @keyup.enter="handleQuery" />
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
          v-hasPermi="['hr:salary:create']"
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
      <el-table-column align="center" label="员工 ID" prop="employeeId" min-width="120" />
      <el-table-column align="center" label="薪资月份" prop="salaryMonth" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="基本工资" prop="baseSalary" min-width="120" />
      <el-table-column align="center" label="加班费" prop="overtimePay" min-width="120" />
      <el-table-column align="center" label="奖金" prop="bonus" min-width="120" />
      <el-table-column align="center" label="扣款" prop="deduction" min-width="120" />
      <el-table-column align="center" label="社保" prop="socialInsurance" min-width="120" />
      <el-table-column align="center" label="公积金" prop="housingFund" min-width="120" />
      <el-table-column align="center" label="个税" prop="tax" min-width="120" />
      <el-table-column align="center" label="实发工资" prop="netSalary" min-width="120" />
      <el-table-column align="center" label="状态" prop="status" min-width="120" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['hr:salary:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['hr:salary:delete']"
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
  <HrSalaryForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { HrSalaryApi } from '@/api/hr/salary'
import HrSalaryForm from './HrSalaryForm.vue'

defineOptions({ name: 'HrSalary' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  employeeId: undefined
  salaryMonth: undefined
  baseSalary: undefined
  overtimePay: undefined
  bonus: undefined
  deduction: undefined
  socialInsurance: undefined
  housingFund: undefined
  tax: undefined
  netSalary: undefined
  status: undefined
  remark: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await HrSalaryApi.getHrSalaryPage(queryParams)
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
    await HrSalaryApi.deleteHrSalary(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
