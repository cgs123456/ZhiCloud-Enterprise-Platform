<!-- ERP 预算管理 -->
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
      <el-form-item label="预算编号" prop="budgetNo">
        <el-input v-model="queryParams.budgetNo" class="!w-240px" clearable placeholder="请输入预算编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="预算年度" prop="budgetYear">
        <el-input-number v-model="queryParams.budgetYear" class="!w-240px" :controls="false" clearable placeholder="请输入预算年度" />
      </el-form-item>
      <el-form-item label="会计期间编号" prop="periodId">
        <el-input-number v-model="queryParams.periodId" class="!w-240px" :controls="false" clearable placeholder="请输入会计期间编号" />
      </el-form-item>
      <el-form-item label="期间编码" prop="periodCode">
        <el-input v-model="queryParams.periodCode" class="!w-240px" clearable placeholder="请输入期间编码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="部门编号" prop="departmentId">
        <el-input-number v-model="queryParams.departmentId" class="!w-240px" :controls="false" clearable placeholder="请输入部门编号" />
      </el-form-item>
      <el-form-item label="预算类型" prop="budgetType">
        <el-input-number v-model="queryParams.budgetType" class="!w-240px" :controls="false" clearable placeholder="请输入预算类型" />
      </el-form-item>
      <el-form-item label="预算类型名称" prop="budgetTypeName">
        <el-input v-model="queryParams.budgetTypeName" class="!w-240px" clearable placeholder="请输入预算类型名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="预算总额" prop="totalAmount">
        <el-input-number v-model="queryParams.totalAmount" class="!w-240px" :controls="false" clearable placeholder="请输入预算总额" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
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
          v-hasPermi="['erp:budget:create']"
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
      <el-table-column align="center" label="预算编号" prop="budgetNo" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="预算年度" prop="budgetYear" min-width="120" />
      <el-table-column align="center" label="会计期间编号" prop="periodId" min-width="120" />
      <el-table-column align="center" label="期间编码" prop="periodCode" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="部门编号" prop="departmentId" min-width="120" />
      <el-table-column align="center" label="预算类型" prop="budgetType" min-width="120" />
      <el-table-column align="center" label="预算类型名称" prop="budgetTypeName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="预算总额" prop="totalAmount" min-width="120" />
      <el-table-column align="center" label="状态" prop="status" min-width="120" />
      <el-table-column align="center" label="状态名称" prop="statusName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['erp:budget:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['erp:budget:delete']"
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
  <ErpBudgetForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { ErpBudgetApi } from '@/api/erp/budget'
import ErpBudgetForm from './ErpBudgetForm.vue'

defineOptions({ name: 'ErpBudget' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  budgetNo: undefined,
  budgetYear: undefined,
  periodId: undefined,
  periodCode: undefined,
  departmentId: undefined,
  budgetType: undefined,
  budgetTypeName: undefined,
  totalAmount: undefined,
  status: undefined,
  statusName: undefined,
  remark: undefined,
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await ErpBudgetApi.getErpBudgetPage(queryParams)
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
    await ErpBudgetApi.deleteErpBudget(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
