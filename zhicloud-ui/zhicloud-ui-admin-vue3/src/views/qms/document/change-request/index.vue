<!-- QMS 变更申请 -->
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
      <el-form-item label="受控文档 ID" prop="documentId">
        <el-input-number v-model="queryParams.documentId" class="!w-240px" :controls="false" clearable placeholder="请输入受控文档 ID" />
      </el-form-item>
      <el-form-item label="变更类型" prop="changeType">
        <el-input-number v-model="queryParams.changeType" class="!w-240px" :controls="false" clearable placeholder="请输入变更类型" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="申请人 ID" prop="applicantId">
        <el-input-number v-model="queryParams.applicantId" class="!w-240px" :controls="false" clearable placeholder="请输入申请人 ID" />
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
          v-hasPermi="['qms:document:change']"
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
      <el-table-column align="center" label="受控文档 ID" prop="documentId" min-width="110" />
      <el-table-column align="center" label="变更类型" prop="changeType" min-width="110" />
      <el-table-column align="center" label="变更原因" prop="changeReason" min-width="120" />
      <el-table-column align="center" label="变更内容" prop="changeContent" min-width="120" />
      <el-table-column align="center" label="申请人 ID" prop="applicantId" min-width="110" />
      <el-table-column align="center" label="申请日期" prop="applyDate" min-width="110" />
      <el-table-column align="center" label="审批人 ID" prop="approverId" min-width="110" />
      <el-table-column align="center" label="审批日期" prop="approveDate" width="170" />
      <el-table-column align="center" label="状态" min-width="110" prop="status">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.QMS_CHANGE_REQUEST_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="备注" prop="remark" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="320">
        <template #default="scope">
          <!-- 10 待审：批准（电子签名，批准后自动执行变更） -->
          <el-button
            v-if="scope.row.status === CRStatus.PENDING"
            v-hasPermi="['qms:document:approve']"
            link
            type="success"
            @click="openApprove(scope.row)"
          >
            批准
          </el-button>
          <!-- 10 待审：驳回 -->
          <el-button
            v-if="scope.row.status === CRStatus.PENDING"
            v-hasPermi="['qms:document:approve']"
            link
            type="warning"
            @click="openReject(scope.row)"
          >
            驳回
          </el-button>
          <el-button
            v-hasPermi="['qms:document:change']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:document:change']"
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
  <QmsDocumentChangeRequestForm ref="formRef" @success="getList" />

  <!-- 批准变更申请（10 待审 → 20 已批准；受控操作：需电子签名） -->
  <Dialog v-model="approveVisible" title="批准变更申请（电子签名）" width="520">
    <el-form ref="approveFormRef" :model="approveForm" :rules="approveRules" label-width="110px">
      <el-form-item label="变更申请">
        <span>文档 ID {{ currentRow?.documentId }}：{{ currentRow?.changeReason }}</span>
      </el-form-item>
      <el-form-item label="执行说明">
        <span>批准后将自动执行变更：修订/新增生成新版本草稿，作废类型使原文档转已作废。</span>
      </el-form-item>
      <el-divider content-position="left">电子签名（21 CFR Part 11）</el-divider>
      <el-form-item label="用户名" prop="username">
        <el-input v-model="approveForm.username" placeholder="请输入当前登录用户名" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="approveForm.password" type="password" show-password placeholder="请输入密码" />
      </el-form-item>
      <el-form-item label="签名原因" prop="reason">
        <el-input v-model="approveForm.reason" type="textarea" :rows="2" placeholder="例如：变更内容评估通过，批准执行" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="approveLoading" type="primary" @click="submitApprove">签名并批准</el-button>
      <el-button @click="approveVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 驳回变更申请（10 待审 → 30 已驳回） -->
  <Dialog v-model="rejectVisible" title="驳回变更申请" width="480">
    <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="110px">
      <el-form-item label="变更申请">
        <span>文档 ID {{ currentRow?.documentId }}：{{ currentRow?.changeReason }}</span>
      </el-form-item>
      <el-form-item label="驳回原因" prop="reason">
        <el-input v-model="rejectForm.reason" type="textarea" :rows="3" placeholder="请输入驳回原因（将写入备注）" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="rejectLoading" type="primary" @click="submitReject">确 定</el-button>
      <el-button @click="rejectVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { QmsDocumentChangeRequestApi } from '@/api/qms/document/change-request'
import { DICT_TYPE } from '@/utils/dict'
import QmsDocumentChangeRequestForm from './QmsDocumentChangeRequestForm.vue'

defineOptions({ name: 'QmsDocumentChangeRequest' })

const message = useMessage()

/** 变更申请状态机（与后端 QmsChangeRequestStatusEnum 对齐；§12.239 接线审批闭环） */
const CRStatus = {
  PENDING: 10,
  APPROVED: 20,
  REJECTED: 30
} as const

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  documentId: undefined,
  changeType: undefined,
  status: undefined,
  applicantId: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await QmsDocumentChangeRequestApi.getQmsDocumentChangeRequestPage(queryParams)
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
    await QmsDocumentChangeRequestApi.deleteQmsDocumentChangeRequest(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

const currentRow = ref<any>({})

/** 批准（10 待审 → 20 已批准，电子签名） */
const approveVisible = ref(false)
const approveLoading = ref(false)
const approveFormRef = ref()
const approveForm = ref<any>({})
const approveRules = reactive({
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }],
  reason: [{ required: true, message: '签名原因不能为空', trigger: 'blur' }]
})

const openApprove = (row: any) => {
  currentRow.value = row
  approveForm.value = { username: undefined, password: undefined, reason: undefined }
  approveVisible.value = true
}

const submitApprove = async () => {
  await approveFormRef.value.validate()
  approveLoading.value = true
  try {
    const { username, password, reason } = approveForm.value
    await QmsDocumentChangeRequestApi.approveQmsDocumentChangeRequest(currentRow.value.id, {
      'X-Electronic-Signature-Username': encodeURIComponent(username),
      'X-Electronic-Signature-Password': encodeURIComponent(password),
      'X-Electronic-Signature-Reason': encodeURIComponent(reason)
    })
    message.success('已签名并批准')
    approveVisible.value = false
    await getList()
  } finally {
    approveLoading.value = false
  }
}

/** 驳回（10 待审 → 30 已驳回） */
const rejectVisible = ref(false)
const rejectLoading = ref(false)
const rejectFormRef = ref()
const rejectForm = ref<any>({})
const rejectRules = reactive({
  reason: [{ required: true, message: '驳回原因不能为空', trigger: 'blur' }]
})

const openReject = (row: any) => {
  currentRow.value = row
  rejectForm.value = { reason: undefined }
  rejectVisible.value = true
}

const submitReject = async () => {
  await rejectFormRef.value.validate()
  rejectLoading.value = true
  try {
    await QmsDocumentChangeRequestApi.rejectQmsDocumentChangeRequest({
      id: currentRow.value.id,
      reason: rejectForm.value.reason
    })
    message.success('已驳回')
    rejectVisible.value = false
    await getList()
  } finally {
    rejectLoading.value = false
  }
}

onMounted(() => getList())
</script>
