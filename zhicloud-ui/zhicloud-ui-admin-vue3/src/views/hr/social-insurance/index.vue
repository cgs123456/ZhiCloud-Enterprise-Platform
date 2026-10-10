<!-- HR 社保 -->
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
      <el-form-item label="年份" prop="year">
        <el-input-number v-model="queryParams.year" class="!w-240px" :controls="false" clearable placeholder="请输入年份" />
      </el-form-item>
      <el-form-item label="养老基数" prop="pensionBase">
        <el-input-number v-model="queryParams.pensionBase" class="!w-240px" :controls="false" clearable placeholder="请输入养老基数" />
      </el-form-item>
      <el-form-item label="医疗基数" prop="medicalBase">
        <el-input-number v-model="queryParams.medicalBase" class="!w-240px" :controls="false" clearable placeholder="请输入医疗基数" />
      </el-form-item>
      <el-form-item label="失业基数" prop="unemploymentBase">
        <el-input-number v-model="queryParams.unemploymentBase" class="!w-240px" :controls="false" clearable placeholder="请输入失业基数" />
      </el-form-item>
      <el-form-item label="工伤基数" prop="workInjuryBase">
        <el-input-number v-model="queryParams.workInjuryBase" class="!w-240px" :controls="false" clearable placeholder="请输入工伤基数" />
      </el-form-item>
      <el-form-item label="生育基数" prop="maternityBase">
        <el-input-number v-model="queryParams.maternityBase" class="!w-240px" :controls="false" clearable placeholder="请输入生育基数" />
      </el-form-item>
      <el-form-item label="公积金基数" prop="housingFundBase">
        <el-input-number v-model="queryParams.housingFundBase" class="!w-240px" :controls="false" clearable placeholder="请输入公积金基数" />
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
          v-hasPermi="['hr:social-insurance:create']"
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
      <el-table-column align="center" label="年份" prop="year" min-width="120" />
      <el-table-column align="center" label="养老基数" prop="pensionBase" min-width="120" />
      <el-table-column align="center" label="医疗基数" prop="medicalBase" min-width="120" />
      <el-table-column align="center" label="失业基数" prop="unemploymentBase" min-width="120" />
      <el-table-column align="center" label="工伤基数" prop="workInjuryBase" min-width="120" />
      <el-table-column align="center" label="生育基数" prop="maternityBase" min-width="120" />
      <el-table-column align="center" label="公积金基数" prop="housingFundBase" min-width="120" />
      <el-table-column align="center" label="状态" prop="status" min-width="120" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['hr:social-insurance:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['hr:social-insurance:delete']"
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
  <HrSocialInsuranceForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { HrSocialInsuranceApi } from '@/api/hr/social-insurance'
import HrSocialInsuranceForm from './HrSocialInsuranceForm.vue'

defineOptions({ name: 'HrSocialInsurance' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  employeeId: undefined
  year: undefined
  pensionBase: undefined
  medicalBase: undefined
  unemploymentBase: undefined
  workInjuryBase: undefined
  maternityBase: undefined
  housingFundBase: undefined
  status: undefined
  remark: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await HrSocialInsuranceApi.getHrSocialInsurancePage(queryParams)
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
    await HrSocialInsuranceApi.deleteHrSocialInsurance(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
