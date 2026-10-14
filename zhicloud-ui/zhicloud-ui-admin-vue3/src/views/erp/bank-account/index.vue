<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="账号" prop="accountNo">
        <el-input v-model="queryParams.accountNo" placeholder="请输入账号" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="账户名称" prop="accountName">
        <el-input v-model="queryParams.accountName" placeholder="请输入账户名称" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="银行" prop="bankName">
        <el-input v-model="queryParams.bankName" placeholder="请输入银行" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['erp:bank-account:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="账号" align="center" prop="accountNo" />
      <el-table-column label="账户名称" align="center" prop="accountName" />
      <el-table-column label="银行" align="center" prop="bankName" />
      <el-table-column label="开户行" align="center" prop="bankBranch" />
      <el-table-column label="余额" align="center" prop="balance" />
      <el-table-column label="币种" align="center" prop="currencyId" />
      <el-table-column label="所属账簿" align="center" prop="accountBookId" width="140">
        <template #default="scope">
          <span>{{ bookName(scope.row.accountBookId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['erp:bank-account:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['erp:bank-account:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <BankAccountForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { BankAccountApi } from '@/api/erp/bank-account'
import { ErpAccountBookApi } from '@/api/erp/account-book'
import BankAccountForm from './bankAccountForm.vue'

defineOptions({ name: 'ErpBankAccount' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  accountNo: undefined,
  accountName: undefined,
  bankName: undefined,
})
const queryFormRef = ref()
const formRef = ref()

/** 账簿 id → 名称映射（V195 列表展示所属账簿；取启用的账簿精简列表） */
const bookMap = ref<Record<number, string>>({})
const bookName = (id?: number) => (id ? bookMap.value[id] || `#${id}` : '-')
const loadBooks = async () => {
  try {
    const books = (await ErpAccountBookApi.getErpAccountBookSimpleList()) || []
    bookMap.value = books.reduce((acc: Record<number, string>, b: any) => {
      acc[b.id] = `${b.code} ${b.name}`
      return acc
    }, {})
  } catch {
    bookMap.value = {}
  }
}

const getList = async () => {
  loading.value = true
  try {
    const data = await BankAccountApi.getBankAccountPage(queryParams)
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

const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await BankAccountApi.deleteBankAccount(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => {
  loadBooks()
  getList()
})
</script>
