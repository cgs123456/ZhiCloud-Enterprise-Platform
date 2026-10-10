<!-- HR 劳动合同 -->
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
      <el-form-item label="合同编号" prop="contractNo">
        <el-input v-model="queryParams.contractNo" class="!w-240px" clearable placeholder="请输入合同编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="合同类型" prop="contractType">
        <el-input-number v-model="queryParams.contractType" class="!w-240px" :controls="false" clearable placeholder="请输入合同类型" />
      </el-form-item>
      <el-form-item label="岗位 ID" prop="positionId">
        <el-input-number v-model="queryParams.positionId" class="!w-240px" :controls="false" clearable placeholder="请输入岗位 ID" />
      </el-form-item>
      <el-form-item label="部门 ID" prop="departmentId">
        <el-input-number v-model="queryParams.departmentId" class="!w-240px" :controls="false" clearable placeholder="请输入部门 ID" />
      </el-form-item>
      <el-form-item label="合同约定薪资" prop="salary">
        <el-input-number v-model="queryParams.salary" class="!w-240px" :controls="false" clearable placeholder="请输入合同约定薪资" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="合同附件" prop="fileUrl">
        <el-input v-model="queryParams.fileUrl" class="!w-240px" clearable placeholder="请输入合同附件" @keyup.enter="handleQuery" />
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
          v-hasPermi="['hr:contract:create']"
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
      <el-table-column align="center" label="合同编号" prop="contractNo" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="合同类型" prop="contractType" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="合同开始日期" prop="startDate" width="170" />
      <el-table-column align="center" :formatter="dateFormatter" label="合同结束日期" prop="endDate" width="170" />
      <el-table-column align="center" :formatter="dateFormatter" label="签订日期" prop="signDate" width="170" />
      <el-table-column align="center" :formatter="dateFormatter" label="试用期结束日期" prop="probationEndDate" width="170" />
      <el-table-column align="center" label="岗位 ID" prop="positionId" min-width="120" />
      <el-table-column align="center" label="部门 ID" prop="departmentId" min-width="120" />
      <el-table-column align="center" label="合同约定薪资" prop="salary" min-width="120" />
      <el-table-column align="center" label="状态" prop="status" min-width="120" />
      <el-table-column align="center" label="合同附件" prop="fileUrl" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['hr:contract:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['hr:contract:delete']"
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
  <HrContractForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { HrContractApi } from '@/api/hr/contract'
import HrContractForm from './HrContractForm.vue'

defineOptions({ name: 'HrContract' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  employeeId: undefined
  contractNo: undefined
  contractType: undefined
  startDate: undefined
  endDate: undefined
  signDate: undefined
  probationEndDate: undefined
  positionId: undefined
  departmentId: undefined
  salary: undefined
  status: undefined
  fileUrl: undefined
  remark: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await HrContractApi.getHrContractPage(queryParams)
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
    await HrContractApi.deleteHrContract(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
