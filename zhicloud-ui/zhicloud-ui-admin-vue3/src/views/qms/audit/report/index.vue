<!-- QMS 审核报告 -->
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
      <el-form-item label="审核计划 ID" prop="planId">
        <el-input-number v-model="queryParams.planId" class="!w-240px" :controls="false" clearable placeholder="请输入审核计划 ID" />
      </el-form-item>
      <el-form-item label="报告编号" prop="reportNo">
        <el-input v-model="queryParams.reportNo" class="!w-240px" clearable placeholder="请输入报告编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="审核结论" prop="conclusion">
        <el-input-number v-model="queryParams.conclusion" class="!w-240px" :controls="false" clearable placeholder="请输入审核结论" />
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
          v-hasPermi="['qms:audit:create']"
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
      <el-table-column align="center" label="审核计划 ID" prop="planId" min-width="110" />
      <el-table-column align="center" label="报告编号" prop="reportNo" min-width="110" />
      <el-table-column align="center" label="审核总结" prop="auditSummary" min-width="120" />
      <el-table-column align="center" label="审核结论" prop="conclusion" min-width="120" />
      <el-table-column align="center" label="发现的不符合项数" prop="issueCount" min-width="110" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" />
      <el-table-column align="center" label="排序" prop="sort" min-width="110" />
      <el-table-column align="center" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['qms:audit:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:audit:delete']"
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
  <QmsAuditReportForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { QmsAuditReportApi } from '@/api/qms/audit/report'
import QmsAuditReportForm from './QmsAuditReportForm.vue'

defineOptions({ name: 'QmsAuditReport' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  planId: undefined,
  reportNo: undefined,
  conclusion: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await QmsAuditReportApi.getQmsAuditReportPage(queryParams)
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
    await QmsAuditReportApi.deleteQmsAuditReport(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
