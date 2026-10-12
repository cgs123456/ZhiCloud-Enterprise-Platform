<!-- ERP 合并抵消分录 -->
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
      <el-form-item label="合并任务编号" prop="consolidationNo">
        <el-input v-model="queryParams.consolidationNo" class="!w-240px" clearable placeholder="请输入合并任务编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="会计期间编号" prop="periodId">
        <el-input-number v-model="queryParams.periodId" class="!w-240px" :controls="false" clearable placeholder="请输入会计期间编号" />
      </el-form-item>
      <el-form-item label="期间编码" prop="periodCode">
        <el-input v-model="queryParams.periodCode" class="!w-240px" clearable placeholder="请输入期间编码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="抵消类型" prop="eliminationType">
        <el-input-number v-model="queryParams.eliminationType" class="!w-240px" :controls="false" clearable placeholder="请输入抵消类型" />
      </el-form-item>
      <el-form-item label="抵消类型名称" prop="eliminationTypeName">
        <el-input v-model="queryParams.eliminationTypeName" class="!w-240px" clearable placeholder="请输入抵消类型名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="借方科目编号" prop="debitAccountId">
        <el-input-number v-model="queryParams.debitAccountId" class="!w-240px" :controls="false" clearable placeholder="请输入借方科目编号" />
      </el-form-item>
      <el-form-item label="借方科目编码" prop="debitAccountCode">
        <el-input v-model="queryParams.debitAccountCode" class="!w-240px" clearable placeholder="请输入借方科目编码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="借方科目名称" prop="debitAccountName">
        <el-input v-model="queryParams.debitAccountName" class="!w-240px" clearable placeholder="请输入借方科目名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="贷方科目编号" prop="creditAccountId">
        <el-input-number v-model="queryParams.creditAccountId" class="!w-240px" :controls="false" clearable placeholder="请输入贷方科目编号" />
      </el-form-item>
      <el-form-item label="贷方科目编码" prop="creditAccountCode">
        <el-input v-model="queryParams.creditAccountCode" class="!w-240px" clearable placeholder="请输入贷方科目编码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="贷方科目名称" prop="creditAccountName">
        <el-input v-model="queryParams.creditAccountName" class="!w-240px" clearable placeholder="请输入贷方科目名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="抵消金额" prop="eliminationAmount">
        <el-input-number v-model="queryParams.eliminationAmount" class="!w-240px" :controls="false" clearable placeholder="请输入抵消金额" />
      </el-form-item>
      <el-form-item label="状态（10 草稿 / 20 已审核）" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态（10 草稿 / 20 已审核）" />
      </el-form-item>
      <el-form-item label="状态名称" prop="statusName">
        <el-input v-model="queryParams.statusName" class="!w-240px" clearable placeholder="请输入状态名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="queryParams.remark" class="!w-240px" clearable placeholder="请输入备注" @keyup.enter="handleQuery" />
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
          v-hasPermi="['erp:consolidation-entry:create']"
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
      <el-table-column align="center" label="合并任务编号" prop="consolidationNo" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="会计期间编号" prop="periodId" min-width="120" />
      <el-table-column align="center" label="期间编码" prop="periodCode" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="抵消类型" prop="eliminationType" min-width="120" />
      <el-table-column align="center" label="抵消类型名称" prop="eliminationTypeName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="借方科目编号" prop="debitAccountId" min-width="120" />
      <el-table-column align="center" label="借方科目编码" prop="debitAccountCode" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="借方科目名称" prop="debitAccountName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="贷方科目编号" prop="creditAccountId" min-width="120" />
      <el-table-column align="center" label="贷方科目编码" prop="creditAccountCode" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="贷方科目名称" prop="creditAccountName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="抵消金额" prop="eliminationAmount" min-width="120" />
      <el-table-column align="center" label="状态（10 草稿 / 20 已审核）" prop="status" min-width="120" />
      <el-table-column align="center" label="状态名称" prop="statusName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['erp:consolidation-entry:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['erp:consolidation-entry:delete']"
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
  <ErpConsolidationEntryForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { ErpConsolidationEntryApi } from '@/api/erp/consolidation-entry'
import ErpConsolidationEntryForm from './ErpConsolidationEntryForm.vue'

defineOptions({ name: 'ErpConsolidationEntry' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  consolidationNo: undefined,
  periodId: undefined,
  periodCode: undefined,
  eliminationType: undefined,
  eliminationTypeName: undefined,
  debitAccountId: undefined,
  debitAccountCode: undefined,
  debitAccountName: undefined,
  creditAccountId: undefined,
  creditAccountCode: undefined,
  creditAccountName: undefined,
  eliminationAmount: undefined,
  status: undefined,
  statusName: undefined,
  remark: undefined,
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await ErpConsolidationEntryApi.getErpConsolidationEntryPage(queryParams)
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
    await ErpConsolidationEntryApi.deleteErpConsolidationEntry(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
