<!-- QMS 变更申请 -->
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
      <el-form-item label="受控文档 ID" prop="documentId">
        <el-input-number v-model="queryParams.documentId" class="!w-240px" :controls="false" clearable placeholder="请输入受控文档 ID" />
      </el-form-item>
      <el-form-item label="变更类型" prop="changeType">
        <el-input-number v-model="queryParams.changeType" class="!w-240px" :controls="false" clearable placeholder="请输入变更类型" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="申请人 ID" prop="applicantId">
        <el-input-number v-model="queryParams.applicantId" class="!w-240px" :controls="false" clearable placeholder="请输入申请人 ID" />
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
          v-hasPermi="['']"
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
      <el-table-column align="center" label="受控文档 ID" prop="documentId" min-width="110" />
      <el-table-column align="center" label="变更类型" prop="changeType" min-width="110" />
      <el-table-column align="center" label="变更原因" prop="changeReason" min-width="120" />
      <el-table-column align="center" label="变更内容" prop="changeContent" min-width="120" />
      <el-table-column align="center" label="申请人 ID" prop="applicantId" min-width="110" />
      <el-table-column align="center" label="申请日期" prop="applyDate" min-width="110" />
      <el-table-column align="center" label="审批人 ID" prop="approverId" min-width="110" />
      <el-table-column align="center" label="审批日期" prop="approveDate" width="170" />
      <el-table-column align="center" label="状态" prop="status" min-width="110" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['']"
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
  <QmsDocumentChangeRequestForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { QmsDocumentChangeRequestApi } from '@/api/qms/document/change-request'
import QmsDocumentChangeRequestForm from './QmsDocumentChangeRequestForm.vue'

defineOptions({ name: 'QmsDocumentChangeRequest' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  documentId: undefined,
  changeType: undefined,
  status: undefined,
  applicantId: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await QmsDocumentChangeRequestApi.getQmsDocumentChangeRequestPage(queryParams)
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
    await QmsDocumentChangeRequestApi.deleteQmsDocumentChangeRequest(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
