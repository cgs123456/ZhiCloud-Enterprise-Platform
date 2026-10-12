<!-- ERP 会计科目 -->
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
      <el-form-item label="父级编号" prop="parentId">
        <el-input-number v-model="queryParams.parentId" class="!w-240px" :controls="false" clearable placeholder="请输入父级编号" />
      </el-form-item>
      <el-form-item label="科目编码" prop="code">
        <el-input v-model="queryParams.code" class="!w-240px" clearable placeholder="请输入科目编码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="科目名称" prop="name">
        <el-input v-model="queryParams.name" class="!w-240px" clearable placeholder="请输入科目名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="科目类型" prop="type">
        <el-input-number v-model="queryParams.type" class="!w-240px" :controls="false" clearable placeholder="请输入科目类型" />
      </el-form-item>
      <el-form-item label="科目类型名称" prop="typeName">
        <el-input v-model="queryParams.typeName" class="!w-240px" clearable placeholder="请输入科目类型名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="余额方向" prop="balanceDirection">
        <el-input-number v-model="queryParams.balanceDirection" class="!w-240px" :controls="false" clearable placeholder="请输入余额方向" />
      </el-form-item>
      <el-form-item label="余额方向名称" prop="balanceDirectionName">
        <el-input v-model="queryParams.balanceDirectionName" class="!w-240px" clearable placeholder="请输入余额方向名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="层级" prop="level">
        <el-input-number v-model="queryParams.level" class="!w-240px" :controls="false" clearable placeholder="请输入层级" />
      </el-form-item>
      <el-form-item label="期初借方余额" prop="openingDebit">
        <el-input-number v-model="queryParams.openingDebit" class="!w-240px" :controls="false" clearable placeholder="请输入期初借方余额" />
      </el-form-item>
      <el-form-item label="期初贷方余额" prop="openingCredit">
        <el-input-number v-model="queryParams.openingCredit" class="!w-240px" :controls="false" clearable placeholder="请输入期初贷方余额" />
      </el-form-item>
      <el-form-item label="当前借方累计发生额" prop="currentDebit">
        <el-input-number v-model="queryParams.currentDebit" class="!w-240px" :controls="false" clearable placeholder="请输入当前借方累计发生额" />
      </el-form-item>
      <el-form-item label="当前贷方累计发生额" prop="currentCredit">
        <el-input-number v-model="queryParams.currentCredit" class="!w-240px" :controls="false" clearable placeholder="请输入当前贷方累计发生额" />
      </el-form-item>
      <el-form-item label="期末借方余额" prop="closingDebit">
        <el-input-number v-model="queryParams.closingDebit" class="!w-240px" :controls="false" clearable placeholder="请输入期末借方余额" />
      </el-form-item>
      <el-form-item label="期末贷方余额" prop="closingCredit">
        <el-input-number v-model="queryParams.closingCredit" class="!w-240px" :controls="false" clearable placeholder="请输入期末贷方余额" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
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
          v-hasPermi="['erp:gl-account:create']"
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
      <el-table-column align="center" label="父级编号" prop="parentId" min-width="120" />
      <el-table-column align="center" label="科目编码" prop="code" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="科目名称" prop="name" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="科目类型" prop="type" min-width="120" />
      <el-table-column align="center" label="科目类型名称" prop="typeName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="余额方向" prop="balanceDirection" min-width="120" />
      <el-table-column align="center" label="余额方向名称" prop="balanceDirectionName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="层级" prop="level" min-width="120" />
      <el-table-column align="center" label="是否末级科目" prop="isLeaf" min-width="90" />
      <el-table-column align="center" label="期初借方余额" prop="openingDebit" min-width="120" />
      <el-table-column align="center" label="期初贷方余额" prop="openingCredit" min-width="120" />
      <el-table-column align="center" label="当前借方累计发生额" prop="currentDebit" min-width="120" />
      <el-table-column align="center" label="当前贷方累计发生额" prop="currentCredit" min-width="120" />
      <el-table-column align="center" label="期末借方余额" prop="closingDebit" min-width="120" />
      <el-table-column align="center" label="期末贷方余额" prop="closingCredit" min-width="120" />
      <el-table-column align="center" label="状态" prop="status" min-width="120" />
      <el-table-column align="center" label="排序" prop="sort" min-width="120" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['erp:gl-account:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['erp:gl-account:delete']"
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
  <ErpGlAccountForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { ErpGlAccountApi } from '@/api/erp/gl-account'
import ErpGlAccountForm from './ErpGlAccountForm.vue'

defineOptions({ name: 'ErpGlAccount' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  parentId: undefined,
  code: undefined,
  name: undefined,
  type: undefined,
  typeName: undefined,
  balanceDirection: undefined,
  balanceDirectionName: undefined,
  level: undefined,
  isLeaf: undefined,
  openingDebit: undefined,
  openingCredit: undefined,
  currentDebit: undefined,
  currentCredit: undefined,
  closingDebit: undefined,
  closingCredit: undefined,
  status: undefined,
  sort: undefined,
  remark: undefined,
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await ErpGlAccountApi.getErpGlAccountPage(queryParams)
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
    await ErpGlAccountApi.deleteErpGlAccount(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
