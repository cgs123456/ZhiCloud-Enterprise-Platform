<!-- QMS 不合格品NCR -->
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
      <el-form-item label="NCR 单号" prop="ncrNo">
        <el-input v-model="queryParams.ncrNo" class="!w-240px" clearable placeholder="请输入NCR 单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="来源" prop="source">
        <el-input-number v-model="queryParams.source" class="!w-240px" :controls="false" clearable placeholder="请输入来源" />
      </el-form-item>
      <el-form-item label="缺陷等级" prop="defectLevel">
        <el-input-number v-model="queryParams.defectLevel" class="!w-240px" :controls="false" clearable placeholder="请输入缺陷等级" />
      </el-form-item>
      <el-form-item label="处置方式" prop="disposition">
        <el-input-number v-model="queryParams.disposition" class="!w-240px" :controls="false" clearable placeholder="请输入处置方式" />
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
          v-hasPermi="['qms:ncr:create']"
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
      <el-table-column align="center" label="NCR 单号" prop="ncrNo" min-width="110" />
      <el-table-column align="center" label="来源" prop="source" min-width="110" />
      <el-table-column align="center" label="检验单 ID" prop="inspectionOrderId" min-width="110" />
      <el-table-column align="center" label="产品 ID" prop="productId" min-width="110" />
      <el-table-column align="center" label="供应商 ID" prop="supplierId" min-width="110" />
      <el-table-column align="center" label="工单 ID" prop="workOrderId" min-width="110" />
      <el-table-column align="center" label="缺陷描述" prop="defectDescription" min-width="120" />
      <el-table-column align="center" label="缺陷等级" prop="defectLevel" min-width="110" />
      <el-table-column align="center" label="不合格数量" prop="quantity" min-width="110" />
      <el-table-column align="center" label="处置方式" prop="disposition" min-width="110" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['qms:ncr:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:ncr:delete']"
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
  <NcrDocumentForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { NcrDocumentApi } from '@/api/qms/ncr/document'
import NcrDocumentForm from './NcrDocumentForm.vue'

defineOptions({ name: 'NcrDocument' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  ncrNo: undefined,
  source: undefined,
  defectLevel: undefined,
  disposition: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await NcrDocumentApi.getNcrDocumentPage(queryParams)
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
    await NcrDocumentApi.deleteNcrDocument(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
