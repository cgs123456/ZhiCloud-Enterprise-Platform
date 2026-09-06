<!-- QMS 供应商审核 -->
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
      <el-form-item label="审核编号" prop="auditNo">
        <el-input v-model="queryParams.auditNo" class="!w-240px" clearable placeholder="请输入审核编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="审核名称" prop="auditName">
        <el-input v-model="queryParams.auditName" class="!w-240px" clearable placeholder="请输入审核名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="供应商 ID" prop="supplierId">
        <el-input-number v-model="queryParams.supplierId" class="!w-240px" :controls="false" clearable placeholder="请输入供应商 ID" />
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
          v-hasPermi="['qms:supplier-audit:create']"
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
      <el-table-column align="center" label="审核编号" prop="auditNo" min-width="110" />
      <el-table-column align="center" label="审核名称" prop="auditName" min-width="110" />
      <el-table-column align="center" label="供应商 ID" prop="supplierId" min-width="110" />
      <el-table-column align="center" label="供应商名称" prop="supplierName" min-width="110" />
      <el-table-column align="center" label="审核类型" prop="auditType" min-width="110" />
      <el-table-column align="center" label="计划日期" prop="plannedDate" min-width="110" />
      <el-table-column align="center" label="实际日期" prop="actualDate" min-width="110" />
      <el-table-column align="center" label="审核员" prop="auditor" min-width="110" />
      <el-table-column align="center" label="审核结论" prop="conclusion" min-width="120" />
      <el-table-column align="center" label="审核报告" prop="auditReport" min-width="110" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['qms:supplier-audit:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:supplier-audit:delete']"
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
  <SupplierAuditForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { SupplierAuditApi } from '@/api/qms/sqm/supplier-audit'
import SupplierAuditForm from './SupplierAuditForm.vue'

defineOptions({ name: 'SupplierAudit' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  auditNo: undefined,
  auditName: undefined,
  supplierId: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await SupplierAuditApi.getSupplierAuditPage(queryParams)
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
    await SupplierAuditApi.deleteSupplierAudit(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
