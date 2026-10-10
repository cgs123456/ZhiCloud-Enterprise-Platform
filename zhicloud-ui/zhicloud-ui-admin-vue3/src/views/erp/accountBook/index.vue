<!-- ERP 账簿管理 -->
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
      <el-form-item label="账簿编码" prop="code">
        <el-input v-model="queryParams.code" class="!w-240px" clearable placeholder="请输入账簿编码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="账簿名称" prop="name">
        <el-input v-model="queryParams.name" class="!w-240px" clearable placeholder="请输入账簿名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="会计准则" prop="accountingStandard">
        <el-input-number v-model="queryParams.accountingStandard" class="!w-240px" :controls="false" clearable placeholder="请输入会计准则" />
      </el-form-item>
      <el-form-item label="会计准则名称" prop="accountingStandardName">
        <el-input v-model="queryParams.accountingStandardName" class="!w-240px" clearable placeholder="请输入会计准则名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="本位币编号" prop="currencyId">
        <el-input-number v-model="queryParams.currencyId" class="!w-240px" :controls="false" clearable placeholder="请输入本位币编号" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="状态名称" prop="statusName">
        <el-input v-model="queryParams.statusName" class="!w-240px" clearable placeholder="请输入状态名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="queryParams.sort" class="!w-240px" :controls="false" clearable placeholder="请输入排序" />
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
          v-hasPermi="['erp:account-book:create']"
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
      <el-table-column align="center" label="账簿编码" prop="code" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="账簿名称" prop="name" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="会计准则" prop="accountingStandard" min-width="120" />
      <el-table-column align="center" label="会计准则名称" prop="accountingStandardName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="本位币编号" prop="currencyId" min-width="120" />
      <el-table-column align="center" label="是否主账簿" prop="isPrimary" min-width="90" />
      <el-table-column align="center" label="状态" prop="status" min-width="120" />
      <el-table-column align="center" label="状态名称" prop="statusName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="排序" prop="sort" min-width="120" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['erp:account-book:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['erp:account-book:delete']"
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
  <ErpAccountBookForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { ErpAccountBookApi } from '@/api/erp/account-book'
import ErpAccountBookForm from './ErpAccountBookForm.vue'

defineOptions({ name: 'ErpAccountBook' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  code: undefined
  name: undefined
  accountingStandard: undefined
  accountingStandardName: undefined
  currencyId: undefined
  isPrimary: undefined
  status: undefined
  statusName: undefined
  sort: undefined
  remark: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await ErpAccountBookApi.getErpAccountBookPage(queryParams)
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
    await ErpAccountBookApi.deleteErpAccountBook(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
