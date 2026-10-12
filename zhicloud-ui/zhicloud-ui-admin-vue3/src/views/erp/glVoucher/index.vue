<!-- ERP 会计凭证 -->
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
      <el-form-item label="凭证字号" prop="voucherNo">
        <el-input v-model="queryParams.voucherNo" class="!w-240px" clearable placeholder="请输入凭证字号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="会计期间编号" prop="periodId">
        <el-input-number v-model="queryParams.periodId" class="!w-240px" :controls="false" clearable placeholder="请输入会计期间编号" />
      </el-form-item>
      <el-form-item label="会计期间编码" prop="periodCode">
        <el-input v-model="queryParams.periodCode" class="!w-240px" clearable placeholder="请输入会计期间编码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="凭证类型" prop="voucherType">
        <el-input-number v-model="queryParams.voucherType" class="!w-240px" :controls="false" clearable placeholder="请输入凭证类型" />
      </el-form-item>
      <el-form-item label="凭证类型名称" prop="voucherTypeName">
        <el-input v-model="queryParams.voucherTypeName" class="!w-240px" clearable placeholder="请输入凭证类型名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="附件张数" prop="attachmentCount">
        <el-input-number v-model="queryParams.attachmentCount" class="!w-240px" :controls="false" clearable placeholder="请输入附件张数" />
      </el-form-item>
      <el-form-item label="凭证摘要" prop="summary">
        <el-input v-model="queryParams.summary" class="!w-240px" clearable placeholder="请输入凭证摘要" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="借方合计" prop="debitTotal">
        <el-input-number v-model="queryParams.debitTotal" class="!w-240px" :controls="false" clearable placeholder="请输入借方合计" />
      </el-form-item>
      <el-form-item label="贷方合计" prop="creditTotal">
        <el-input-number v-model="queryParams.creditTotal" class="!w-240px" :controls="false" clearable placeholder="请输入贷方合计" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="状态名称" prop="statusName">
        <el-input v-model="queryParams.statusName" class="!w-240px" clearable placeholder="请输入状态名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="制单人" prop="preparedBy">
        <el-input v-model="queryParams.preparedBy" class="!w-240px" clearable placeholder="请输入制单人" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="审核人" prop="approvedBy">
        <el-input v-model="queryParams.approvedBy" class="!w-240px" clearable placeholder="请输入审核人" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="queryParams.remark" class="!w-240px" clearable placeholder="请输入备注" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="账簿 ID（多账簿支持，空表示默认主账簿）" prop="accountBookId">
        <el-input-number v-model="queryParams.accountBookId" class="!w-240px" :controls="false" clearable placeholder="请输入账簿 ID（多账簿支持，空表示默认主账簿）" />
      </el-form-item>
      <el-form-item label="来源会计事件 ID" prop="sourceEventId">
        <el-input-number v-model="queryParams.sourceEventId" class="!w-240px" :controls="false" clearable placeholder="请输入来源会计事件 ID" />
      </el-form-item>
      <el-form-item label="来源单据类型" prop="sourceDocType">
        <el-input v-model="queryParams.sourceDocType" class="!w-240px" clearable placeholder="请输入来源单据类型" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="来源单据 ID" prop="sourceDocId">
        <el-input-number v-model="queryParams.sourceDocId" class="!w-240px" :controls="false" clearable placeholder="请输入来源单据 ID" />
      </el-form-item>
      <el-form-item label="来源单据编号" prop="sourceDocNo">
        <el-input v-model="queryParams.sourceDocNo" class="!w-240px" clearable placeholder="请输入来源单据编号" @keyup.enter="handleQuery" />
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
          v-hasPermi="['erp:gl-voucher:create']"
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
      <el-table-column align="center" label="凭证字号" prop="voucherNo" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="凭证日期" prop="voucherDate" width="170" />
      <el-table-column align="center" label="会计期间编号" prop="periodId" min-width="120" />
      <el-table-column align="center" label="会计期间编码" prop="periodCode" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="凭证类型" prop="voucherType" min-width="120" />
      <el-table-column align="center" label="凭证类型名称" prop="voucherTypeName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="附件张数" prop="attachmentCount" min-width="120" />
      <el-table-column align="center" label="凭证摘要" prop="summary" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="借方合计" prop="debitTotal" min-width="120" />
      <el-table-column align="center" label="贷方合计" prop="creditTotal" min-width="120" />
      <el-table-column align="center" label="状态" prop="status" min-width="120" />
      <el-table-column align="center" label="状态名称" prop="statusName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="制单人" prop="preparedBy" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="审核人" prop="approvedBy" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="审核时间" prop="approvedTime" width="170" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="账簿 ID（多账簿支持，空表示默认主账簿）" prop="accountBookId" min-width="120" />
      <el-table-column align="center" label="来源会计事件 ID" prop="sourceEventId" min-width="120" />
      <el-table-column align="center" label="来源单据类型" prop="sourceDocType" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="来源单据 ID" prop="sourceDocId" min-width="120" />
      <el-table-column align="center" label="来源单据编号" prop="sourceDocNo" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['erp:gl-voucher:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['erp:gl-voucher:delete']"
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
  <ErpGlVoucherForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { ErpGlVoucherApi } from '@/api/erp/gl-voucher'
import ErpGlVoucherForm from './ErpGlVoucherForm.vue'

defineOptions({ name: 'ErpGlVoucher' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  voucherNo: undefined,
  voucherDate: undefined,
  periodId: undefined,
  periodCode: undefined,
  voucherType: undefined,
  voucherTypeName: undefined,
  attachmentCount: undefined,
  summary: undefined,
  debitTotal: undefined,
  creditTotal: undefined,
  status: undefined,
  statusName: undefined,
  preparedBy: undefined,
  approvedBy: undefined,
  approvedTime: undefined,
  remark: undefined,
  accountBookId: undefined,
  sourceEventId: undefined,
  sourceDocType: undefined,
  sourceDocId: undefined,
  sourceDocNo: undefined,
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await ErpGlVoucherApi.getErpGlVoucherPage(queryParams)
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
    await ErpGlVoucherApi.deleteErpGlVoucher(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
