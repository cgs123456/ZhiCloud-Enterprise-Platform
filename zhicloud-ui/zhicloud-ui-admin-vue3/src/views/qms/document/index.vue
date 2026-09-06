<!-- QMS 受控文档 -->
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
      <el-form-item label="文件编号" prop="docNo">
        <el-input v-model="queryParams.docNo" class="!w-240px" clearable placeholder="请输入文件编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" class="!w-240px" clearable placeholder="请输入标题" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="文件类型" prop="docType">
        <el-input-number v-model="queryParams.docType" class="!w-240px" :controls="false" clearable placeholder="请输入文件类型" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="归属部门 ID" prop="ownerDeptId">
        <el-input-number v-model="queryParams.ownerDeptId" class="!w-240px" :controls="false" clearable placeholder="请输入归属部门 ID" />
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
          v-hasPermi="['qms:document:create']"
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
      <el-table-column align="center" label="文件编号" prop="docNo" min-width="110" />
      <el-table-column align="center" label="标题" prop="title" min-width="110" />
      <el-table-column align="center" label="文件类型" prop="docType" min-width="110" />
      <el-table-column align="center" label="版本号" prop="version" min-width="110" />
      <el-table-column align="center" label="状态" prop="status" min-width="110" />
      <el-table-column align="center" label="生效日期" prop="effectiveDate" min-width="110" />
      <el-table-column align="center" label="失效日期" prop="expiryDate" min-width="110" />
      <el-table-column align="center" label="审批人 ID" prop="approverId" min-width="110" />
      <el-table-column align="center" label="审批日期" prop="approveDate" width="170" />
      <el-table-column align="center" label="归属部门 ID" prop="ownerDeptId" min-width="110" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['qms:document:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:document:delete']"
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
  <QmsDocumentForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { QmsDocumentApi } from '@/api/qms/document'
import QmsDocumentForm from './QmsDocumentForm.vue'

defineOptions({ name: 'QmsDocument' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  docNo: undefined,
  title: undefined,
  docType: undefined,
  status: undefined,
  ownerDeptId: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await QmsDocumentApi.getQmsDocumentPage(queryParams)
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
    await QmsDocumentApi.deleteQmsDocument(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
