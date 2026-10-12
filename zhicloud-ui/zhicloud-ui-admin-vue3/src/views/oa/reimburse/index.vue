<!-- OA 报销单 -->
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
      <el-form-item label="报销单号" prop="no">
        <el-input v-model="queryParams.no" class="!w-240px" clearable placeholder="请输入报销单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="报销主题" prop="reimburseName">
        <el-input v-model="queryParams.reimburseName" class="!w-240px" clearable placeholder="请输入报销主题" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="申请人 ID" prop="applicantUserId">
        <el-input-number v-model="queryParams.applicantUserId" class="!w-240px" :controls="false" clearable placeholder="请输入申请人 ID" />
      </el-form-item>
      <el-form-item label="部门 ID" prop="deptId">
        <el-input-number v-model="queryParams.deptId" class="!w-240px" :controls="false" clearable placeholder="请输入部门 ID" />
      </el-form-item>
      <el-form-item label="报销类型（10 差旅 20 招待 30 办公 40 交通 50 其他）" prop="reimburseType">
        <el-input-number v-model="queryParams.reimburseType" class="!w-240px" :controls="false" clearable placeholder="请输入报销类型（10 差旅 20 招待 30 办公 40 交通 50 其他）" />
      </el-form-item>
      <el-form-item label="报销总额" prop="totalAmount">
        <el-input-number v-model="queryParams.totalAmount" class="!w-240px" :controls="false" clearable placeholder="请输入报销总额" />
      </el-form-item>
      <el-form-item label="支付状态（10 未支付 20 部分支付 30 已支付）" prop="paymentStatus">
        <el-input-number v-model="queryParams.paymentStatus" class="!w-240px" :controls="false" clearable placeholder="请输入支付状态（10 未支付 20 部分支付 30 已支付）" />
      </el-form-item>
      <el-form-item label="工作流编号" prop="processInstanceId">
        <el-input v-model="queryParams.processInstanceId" class="!w-240px" clearable placeholder="请输入工作流编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态（10 草稿 20 审批中 30 已通过 40 已驳回 50 已撤销）" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态（10 草稿 20 审批中 30 已通过 40 已驳回 50 已撤销）" />
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
          v-hasPermi="['oa:reimburse:create']"
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
      <el-table-column align="center" label="报销单号" prop="no" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="报销主题" prop="reimburseName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="申请人 ID" prop="applicantUserId" min-width="120" />
      <el-table-column align="center" label="部门 ID" prop="deptId" min-width="120" />
      <el-table-column align="center" label="报销类型（10 差旅 20 招待 30 办公 40 交通 50 其他）" prop="reimburseType" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="报销日期" prop="reimburseDate" width="170" />
      <el-table-column align="center" label="报销总额" prop="totalAmount" min-width="120" />
      <el-table-column align="center" label="支付状态（10 未支付 20 部分支付 30 已支付）" prop="paymentStatus" min-width="120" />
      <el-table-column align="center" label="工作流编号" prop="processInstanceId" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="状态（10 草稿 20 审批中 30 已通过 40 已驳回 50 已撤销）" prop="status" min-width="120" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['oa:reimburse:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['oa:reimburse:delete']"
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
  <OaReimburseForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { OaReimburseApi } from '@/api/oa/reimburse'
import OaReimburseForm from './OaReimburseForm.vue'

defineOptions({ name: 'OaReimburse' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  no: undefined,
  reimburseName: undefined,
  applicantUserId: undefined,
  deptId: undefined,
  reimburseType: undefined,
  reimburseDate: undefined,
  totalAmount: undefined,
  paymentStatus: undefined,
  processInstanceId: undefined,
  status: undefined,
  remark: undefined,
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await OaReimburseApi.getOaReimbursePage(queryParams)
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
    await OaReimburseApi.deleteOaReimburse(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
