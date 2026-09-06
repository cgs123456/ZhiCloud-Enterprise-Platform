<!-- QMS 文档分发 -->
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
      <el-form-item label="分发对象" prop="distributeTo">
        <el-input v-model="queryParams.distributeTo" class="!w-240px" clearable placeholder="请输入分发对象" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="签收人" prop="receivedBy">
        <el-input v-model="queryParams.receivedBy" class="!w-240px" clearable placeholder="请输入签收人" @keyup.enter="handleQuery" />
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
      <el-table-column align="center" label="分发对象" prop="distributeTo" min-width="110" />
      <el-table-column align="center" label="分发份数" prop="distributeQty" min-width="110" />
      <el-table-column align="center" label="分发日期" prop="distributeDate" min-width="110" />
      <el-table-column align="center" label="签收人" prop="receivedBy" min-width="110" />
      <el-table-column align="center" label="签收日期" prop="receivedDate" min-width="110" />
      <el-table-column align="center" label="回收份数" prop="returnedQty" min-width="110" />
      <el-table-column align="center" label="回收日期" prop="returnedDate" min-width="110" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" />
      <el-table-column align="center" label="排序" prop="sort" min-width="110" />
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
  <QmsDocumentDistributeForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { QmsDocumentDistributeApi } from '@/api/qms/document/distribute'
import QmsDocumentDistributeForm from './QmsDocumentDistributeForm.vue'

defineOptions({ name: 'QmsDocumentDistribute' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  documentId: undefined,
  distributeTo: undefined,
  receivedBy: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await QmsDocumentDistributeApi.getQmsDocumentDistributePage(queryParams)
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
    await QmsDocumentDistributeApi.deleteQmsDocumentDistribute(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
