<!-- QMS 审核不符合项 -->
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
      <el-form-item label="审核报告 ID" prop="reportId">
        <el-input-number v-model="queryParams.reportId" class="!w-240px" :controls="false" clearable placeholder="请输入审核报告 ID" />
      </el-form-item>
      <el-form-item label="不符合项编号" prop="ncNo">
        <el-input v-model="queryParams.ncNo" class="!w-240px" clearable placeholder="请输入不符合项编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="严重程度" prop="severity">
        <el-input-number v-model="queryParams.severity" class="!w-240px" :controls="false" clearable placeholder="请输入严重程度" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="责任部门 ID" prop="responsibleDeptId">
        <el-input-number v-model="queryParams.responsibleDeptId" class="!w-240px" :controls="false" clearable placeholder="请输入责任部门 ID" />
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
      <el-table-column align="center" label="审核报告 ID" prop="reportId" min-width="110" />
      <el-table-column align="center" label="不符合项编号" prop="ncNo" min-width="110" />
      <el-table-column align="center" label="严重程度" prop="severity" min-width="110" />
      <el-table-column align="center" label="不符合描述" prop="description" min-width="120" />
      <el-table-column align="center" label="不符合条款" prop="clause" min-width="110" />
      <el-table-column align="center" label="责任部门 ID" prop="responsibleDeptId" min-width="110" />
      <el-table-column align="center" label="整改截止日期" prop="correctiveActionDeadline" min-width="110" />
      <el-table-column align="center" label="状态" prop="status" min-width="110" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" />
      <el-table-column align="center" label="排序" prop="sort" min-width="110" />
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
  <QmsAuditNonconformityForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { QmsAuditNonconformityApi } from '@/api/qms/audit/nonconformity'
import QmsAuditNonconformityForm from './QmsAuditNonconformityForm.vue'

defineOptions({ name: 'QmsAuditNonconformity' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  reportId: undefined,
  ncNo: undefined,
  severity: undefined,
  status: undefined,
  responsibleDeptId: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await QmsAuditNonconformityApi.getQmsAuditNonconformityPage(queryParams)
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
    await QmsAuditNonconformityApi.deleteQmsAuditNonconformity(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
