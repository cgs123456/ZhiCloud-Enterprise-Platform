<!-- QMS 纠正预防措施CAPA -->
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
      <el-form-item label="CAPA 单号" prop="capaNo">
        <el-input v-model="queryParams.capaNo" class="!w-240px" clearable placeholder="请输入CAPA 单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="来源" prop="source">
        <el-input-number v-model="queryParams.source" class="!w-240px" :controls="false" clearable placeholder="请输入来源" />
      </el-form-item>
      <el-form-item label="优先级" prop="priority">
        <el-input-number v-model="queryParams.priority" class="!w-240px" :controls="false" clearable placeholder="请输入优先级" />
      </el-form-item>
      <el-form-item label="当前阶段" prop="stage">
        <el-input-number v-model="queryParams.stage" class="!w-240px" :controls="false" clearable placeholder="请输入当前阶段" />
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
          v-hasPermi="['qms:capa:create']"
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
      <el-table-column align="center" label="CAPA 单号" prop="capaNo" min-width="110" />
      <el-table-column align="center" label="来源" prop="source" min-width="110" />
      <el-table-column align="center" label="优先级" prop="priority" min-width="110" />
      <el-table-column align="center" label="当前阶段" prop="stage" min-width="110" />
      <el-table-column align="center" label="问题描述" prop="problem" min-width="120" />
      <el-table-column align="center" label="原因" prop="cause" min-width="120" />
      <el-table-column align="center" label="根本原因分析" prop="rootCauseAnalysis" min-width="120" />
      <el-table-column align="center" label="纠正措施" prop="correctiveAction" min-width="120" />
      <el-table-column align="center" label="预防措施" prop="preventiveAction" min-width="120" />
      <el-table-column align="center" label="责任人" prop="responsiblePerson" min-width="110" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['qms:capa:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:capa:delete']"
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
  <CAPADocumentForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { CAPADocumentApi } from '@/api/qms/capa/document'
import CAPADocumentForm from './CAPADocumentForm.vue'

defineOptions({ name: 'CAPADocument' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  capaNo: undefined,
  source: undefined,
  priority: undefined,
  stage: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await CAPADocumentApi.getCAPADocumentPage(queryParams)
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
    await CAPADocumentApi.deleteCAPADocument(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
