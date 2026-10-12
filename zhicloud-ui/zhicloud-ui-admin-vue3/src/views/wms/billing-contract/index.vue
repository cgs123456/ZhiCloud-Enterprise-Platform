<!-- WMS 计费合同 -->
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
      <el-form-item label="合同号" prop="contractNo">
        <el-input v-model="queryParams.contractNo" class="!w-240px" clearable placeholder="请输入合同号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="货主编号" prop="ownerId">
        <el-input-number v-model="queryParams.ownerId" class="!w-240px" :controls="false" clearable placeholder="请输入货主编号" />
      </el-form-item>
      <el-form-item label="货主名称" prop="ownerName">
        <el-input v-model="queryParams.ownerName" class="!w-240px" clearable placeholder="请输入货主名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="合同名称" prop="contractName">
        <el-input v-model="queryParams.contractName" class="!w-240px" clearable placeholder="请输入合同名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="合同状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入合同状态" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="queryParams.remark" class="!w-240px" clearable placeholder="请输入备注" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="创建者名称" prop="creatorName">
        <el-input v-model="queryParams.creatorName" class="!w-240px" clearable placeholder="请输入创建者名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="总金额（仅用于展示）" prop="totalAmount">
        <el-input-number v-model="queryParams.totalAmount" class="!w-240px" :controls="false" clearable placeholder="请输入总金额（仅用于展示）" />
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
          v-hasPermi="['wms:billing-contract:create']"
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
      <el-table-column align="center" label="合同号" prop="contractNo" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="货主编号" prop="ownerId" min-width="120" />
      <el-table-column align="center" label="货主名称" prop="ownerName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="合同名称" prop="contractName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="生效日期" prop="startDate" width="170" />
      <el-table-column align="center" :formatter="dateFormatter" label="失效日期" prop="endDate" width="170" />
      <el-table-column align="center" label="合同状态" prop="status" min-width="120" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="创建者名称" prop="creatorName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="总金额（仅用于展示）" prop="totalAmount" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['wms:billing-contract:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['wms:billing-contract:delete']"
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
  <WmsBillingContractForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { WmsBillingContractApi } from '@/api/wms/billing-contract'
import WmsBillingContractForm from './WmsBillingContractForm.vue'

defineOptions({ name: 'WmsBillingContract' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  contractNo: undefined,
  ownerId: undefined,
  ownerName: undefined,
  contractName: undefined,
  startDate: undefined,
  endDate: undefined,
  status: undefined,
  remark: undefined,
  creatorName: undefined,
  totalAmount: undefined,
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await WmsBillingContractApi.getWmsBillingContractPage(queryParams)
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
    await WmsBillingContractApi.deleteWmsBillingContract(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
