<!-- QMS 8D报告 -->
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
      <el-form-item label="8D 报告编号" prop="reportNo">
        <el-input v-model="queryParams.reportNo" class="!w-240px" clearable placeholder="请输入8D 报告编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" class="!w-240px" clearable placeholder="请输入标题" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="关联 NCR 编号 ID" prop="ncrId">
        <el-input-number v-model="queryParams.ncrId" class="!w-240px" :controls="false" clearable placeholder="请输入关联 NCR 编号 ID" />
      </el-form-item>
      <el-form-item label="关联 CAPA 编号 ID" prop="capaId">
        <el-input-number v-model="queryParams.capaId" class="!w-240px" :controls="false" clearable placeholder="请输入关联 CAPA 编号 ID" />
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
          v-hasPermi="['qms:eight-d:create']"
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
      <el-table-column align="center" label="8D 报告编号" prop="reportNo" min-width="110" />
      <el-table-column align="center" label="标题" prop="title" min-width="110" />
      <el-table-column align="center" label="关联 NCR 编号 ID" prop="ncrId" min-width="110" />
      <el-table-column align="center" label="关联 CAPA 编号 ID" prop="capaId" min-width="110" />
      <el-table-column align="center" label="状态" prop="status" min-width="110" />
      <el-table-column align="center" label="D1 团队成员" prop="d1TeamMembers" min-width="110" />
      <el-table-column align="center" label="D2 问题描述" prop="d2ProblemDescription" min-width="120" />
      <el-table-column align="center" label="D3 临时遏制措施" prop="d3InterimAction" min-width="120" />
      <el-table-column align="center" label="D4 根本原因分析" prop="d4RootCause" min-width="120" />
      <el-table-column align="center" label="D5 永久纠正措施" prop="d5PermanentAction" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['qms:eight-d:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:eight-d:delete']"
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
  <EightDReportForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { EightDReportApi } from '@/api/qms/eightd/report'
import EightDReportForm from './EightDReportForm.vue'

defineOptions({ name: 'EightDReport' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  reportNo: undefined,
  title: undefined,
  status: undefined,
  ncrId: undefined,
  capaId: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await EightDReportApi.getEightDReportPage(queryParams)
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
    await EightDReportApi.deleteEightDReport(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
