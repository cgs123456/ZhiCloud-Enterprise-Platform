<!-- HR 绩效考核 -->
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
      <el-form-item label="排名" prop="rank">
        <el-input-number v-model="queryParams.rank" class="!w-240px" :controls="false" clearable placeholder="请输入排名" />
      </el-form-item>
      <el-form-item label="员工 ID" prop="employeeId">
        <el-input-number v-model="queryParams.employeeId" class="!w-240px" :controls="false" clearable placeholder="请输入员工 ID" />
      </el-form-item>
      <el-form-item label="考核周期" prop="period">
        <el-input v-model="queryParams.period" class="!w-240px" clearable placeholder="请输入考核周期" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="考核得分" prop="score">
        <el-input-number v-model="queryParams.score" class="!w-240px" :controls="false" clearable placeholder="请输入考核得分" />
      </el-form-item>
      <el-form-item label="考核等级" prop="grade">
        <el-input-number v-model="queryParams.grade" class="!w-240px" :controls="false" clearable placeholder="请输入考核等级" />
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
          v-hasPermi="['hr:performance:create']"
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
      <el-table-column align="center" label="排名" prop="rank" min-width="120" />
      <el-table-column align="center" label="员工 ID" prop="employeeId" min-width="120" />
      <el-table-column align="center" label="考核周期" prop="period" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="考核得分" prop="score" min-width="120" />
      <el-table-column align="center" label="考核等级" prop="grade" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['hr:performance:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['hr:performance:delete']"
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
  <HrPerformanceForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { HrPerformanceApi } from '@/api/hr/performance'
import HrPerformanceForm from './HrPerformanceForm.vue'

defineOptions({ name: 'HrPerformance' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  rank: undefined
  employeeId: undefined
  period: undefined
  score: undefined
  grade: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await HrPerformanceApi.getHrPerformancePage(queryParams)
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
    await HrPerformanceApi.deleteHrPerformance(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
