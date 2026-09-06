<!-- QMS FMEA文档 -->
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
      <el-form-item label="FMEA 单号" prop="fmeaNo">
        <el-input v-model="queryParams.fmeaNo" class="!w-240px" clearable placeholder="请输入FMEA 单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="FMEA 类型" prop="fmeaType">
        <el-input-number v-model="queryParams.fmeaType" class="!w-240px" :controls="false" clearable placeholder="请输入FMEA 类型" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="产品 ID" prop="productId">
        <el-input-number v-model="queryParams.productId" class="!w-240px" :controls="false" clearable placeholder="请输入产品 ID" />
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
          v-hasPermi="['qms:fmea:create']"
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
      <el-table-column align="center" label="FMEA 单号" prop="fmeaNo" min-width="110" />
      <el-table-column align="center" label="FMEA 类型" prop="fmeaType" min-width="110" />
      <el-table-column align="center" label="产品 ID" prop="productId" min-width="110" />
      <el-table-column align="center" label="工序 ID" prop="processId" min-width="110" />
      <el-table-column align="center" label="版本" prop="version" min-width="110" />
      <el-table-column align="center" label="状态" prop="status" min-width="110" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" />
      <el-table-column align="center" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['qms:fmea:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:fmea:delete']"
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
  <FmeaDocumentForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { FmeaDocumentApi } from '@/api/qms/fmea/document'
import FmeaDocumentForm from './FmeaDocumentForm.vue'

defineOptions({ name: 'FmeaDocument' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  fmeaNo: undefined,
  fmeaType: undefined,
  status: undefined,
  productId: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await FmeaDocumentApi.getFmeaDocumentPage(queryParams)
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
    await FmeaDocumentApi.deleteFmeaDocument(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
