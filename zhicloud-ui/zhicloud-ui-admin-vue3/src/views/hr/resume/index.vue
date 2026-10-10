<!-- HR 简历 -->
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
      <el-form-item label="简历 ID" prop="resumeId">
        <el-input-number v-model="queryParams.resumeId" class="!w-240px" :controls="false" clearable placeholder="请输入简历 ID" />
      </el-form-item>
      <el-form-item label="面试轮次" prop="interviewRound">
        <el-input-number v-model="queryParams.interviewRound" class="!w-240px" :controls="false" clearable placeholder="请输入面试轮次" />
      </el-form-item>
      <el-form-item label="面试官 ID" prop="interviewerId">
        <el-input-number v-model="queryParams.interviewerId" class="!w-240px" :controls="false" clearable placeholder="请输入面试官 ID" />
      </el-form-item>
      <el-form-item label="面试类型" prop="interviewType">
        <el-input-number v-model="queryParams.interviewType" class="!w-240px" :controls="false" clearable placeholder="请输入面试类型" />
      </el-form-item>
      <el-form-item label="结果" prop="result">
        <el-input-number v-model="queryParams.result" class="!w-240px" :controls="false" clearable placeholder="请输入结果" />
      </el-form-item>
      <el-form-item label="面试评价" prop="comment">
        <el-input v-model="queryParams.comment" class="!w-240px" clearable placeholder="请输入面试评价" @keyup.enter="handleQuery" />
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
          v-hasPermi="['hr:resume:create']"
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
      <el-table-column align="center" label="简历 ID" prop="resumeId" min-width="120" />
      <el-table-column align="center" label="面试轮次" prop="interviewRound" min-width="120" />
      <el-table-column align="center" label="面试官 ID" prop="interviewerId" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="面试时间" prop="interviewTime" width="170" />
      <el-table-column align="center" label="面试类型" prop="interviewType" min-width="120" />
      <el-table-column align="center" label="结果" prop="result" min-width="120" />
      <el-table-column align="center" label="面试评价" prop="comment" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="状态" prop="status" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['hr:resume:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['hr:resume:delete']"
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
  <HrResumeForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { HrResumeApi } from '@/api/hr/resume'
import HrResumeForm from './HrResumeForm.vue'

defineOptions({ name: 'HrResume' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  resumeId: undefined
  interviewRound: undefined
  interviewerId: undefined
  interviewTime: undefined
  interviewType: undefined
  result: undefined
  comment: undefined
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await HrResumeApi.getHrResumePage(queryParams)
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
    await HrResumeApi.deleteHrResume(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
