<!-- QMS 培训记录 -->
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
      <el-form-item label="记录编号" prop="recordNo">
        <el-input v-model="queryParams.recordNo" class="!w-240px" clearable placeholder="请输入记录编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="培训计划 ID" prop="planId">
        <el-input-number v-model="queryParams.planId" class="!w-240px" :controls="false" clearable placeholder="请输入培训计划 ID" />
      </el-form-item>
      <el-form-item label="参训人员 ID" prop="traineeId">
        <el-input-number v-model="queryParams.traineeId" class="!w-240px" :controls="false" clearable placeholder="请输入参训人员 ID" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
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
          v-hasPermi="['qms:training-record:create']"
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
      <el-table-column align="center" label="记录编号" prop="recordNo" min-width="110" />
      <el-table-column align="center" label="培训计划 ID" prop="planId" min-width="110" />
      <el-table-column align="center" label="参训人员 ID" prop="traineeId" min-width="110" />
      <el-table-column align="center" label="参训人员姓名" prop="traineeName" min-width="110" />
      <el-table-column align="center" label="成绩" prop="score" min-width="110" />
      <el-table-column align="center" label="是否通过（0 否 1 是）" prop="passed" min-width="110" />
      <el-table-column align="center" label="证书编号" prop="certificateNo" min-width="110" />
      <el-table-column align="center" label="证书到期日" prop="certificateExpireDate" min-width="110" />
      <el-table-column align="center" label="状态" prop="status" min-width="110" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['qms:training-record:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:training-record:delete']"
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
  <TrainingRecordForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { TrainingRecordApi } from '@/api/qms/training/record'
import TrainingRecordForm from './TrainingRecordForm.vue'

defineOptions({ name: 'TrainingRecord' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  recordNo: undefined,
  planId: undefined,
  traineeId: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await TrainingRecordApi.getTrainingRecordPage(queryParams)
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
    await TrainingRecordApi.deleteTrainingRecord(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
