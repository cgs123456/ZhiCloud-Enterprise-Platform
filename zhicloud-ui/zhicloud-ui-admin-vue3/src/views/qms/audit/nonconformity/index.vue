<!-- QMS 审核不符合项 -->
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
      <el-form-item label="审核报告 ID" prop="reportId">
        <el-input-number v-model="queryParams.reportId" class="!w-240px" :controls="false" clearable placeholder="请输入审核报告 ID" />
      </el-form-item>
      <el-form-item label="不符合项编号" prop="ncNo">
        <el-input v-model="queryParams.ncNo" class="!w-240px" clearable placeholder="请输入不符合项编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="严重程度" prop="severity">
        <el-input-number v-model="queryParams.severity" class="!w-240px" :controls="false" clearable placeholder="请输入严重程度" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-240px" clearable placeholder="请选择状态">
          <el-option v-for="item in NC_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="责任部门 ID" prop="responsibleDeptId">
        <el-input-number v-model="queryParams.responsibleDeptId" class="!w-240px" :controls="false" clearable placeholder="请输入责任部门 ID" />
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
          v-hasPermi="['qms:audit:create']"
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
      <el-table-column align="center" label="审核报告 ID" prop="reportId" min-width="110" />
      <el-table-column align="center" label="不符合项编号" prop="ncNo" min-width="110" />
      <el-table-column align="center" label="严重程度" prop="severity" min-width="110" />
      <el-table-column align="center" label="不符合描述" prop="description" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="不符合条款" prop="clause" min-width="110" show-overflow-tooltip />
      <el-table-column align="center" label="责任部门 ID" prop="responsibleDeptId" min-width="110" />
      <el-table-column align="center" label="整改截止日期" prop="correctiveActionDeadline" min-width="110" />
      <el-table-column align="center" label="状态" prop="status" min-width="110">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="small">
            {{ statusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="排序" prop="sort" min-width="110" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="300">
        <template #default="scope">
          <!-- 10 待整改：提交整改措施 -->
          <el-button
            v-if="scope.row.status === NcStatus.PENDING"
            v-hasPermi="['qms:audit:rectify']"
            link
            type="primary"
            @click="openRectify(scope.row)"
          >
            整改
          </el-button>
          <!-- 20 整改中：完成整改 -->
          <el-button
            v-if="scope.row.status === NcStatus.RECTIFYING"
            v-hasPermi="['qms:audit:rectify']"
            link
            type="primary"
            @click="handleComplete(scope.row)"
          >
            完成整改
          </el-button>
          <!-- 30 已整改：验证（电子签名） -->
          <el-button
            v-if="scope.row.status === NcStatus.RECTIFIED"
            v-hasPermi="['qms:audit:verify']"
            link
            type="warning"
            @click="openVerify(scope.row)"
          >
            验证
          </el-button>
          <!-- 40 已验证：关闭（电子签名） -->
          <el-button
            v-if="scope.row.status === NcStatus.VERIFIED"
            v-hasPermi="['qms:audit:close']"
            link
            type="success"
            @click="openClose(scope.row)"
          >
            关闭
          </el-button>
          <el-button
            v-hasPermi="['qms:audit:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:audit:delete']"
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
  <QmsAuditNonconformityForm ref="formRef" @success="getList" />

  <!-- 提交整改措施（10 -> 20） -->
  <Dialog v-model="rectifyVisible" title="提交整改措施" width="480">
    <el-form ref="rectifyFormRef" :model="rectifyForm" :rules="rectifyRules" label-width="110px">
      <el-form-item label="不符合项">
        <span>{{ currentRow?.ncNo }}（{{ currentRow?.description }}）</span>
      </el-form-item>
      <el-form-item label="整改措施" prop="action">
        <el-input v-model="rectifyForm.action" type="textarea" :rows="3" placeholder="请输入整改措施" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="rectifyLoading" type="primary" @click="submitRectify">提交</el-button>
      <el-button @click="rectifyVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 验证整改效果（30 -> 40，受控操作：需电子签名） -->
  <Dialog v-model="verifyVisible" title="验证整改效果（电子签名）" width="520">
    <el-form ref="verifyFormRef" :model="verifyForm" :rules="verifyRules" label-width="110px">
      <el-form-item label="不符合项">
        <span>{{ currentRow?.ncNo }}</span>
      </el-form-item>
      <el-form-item label="验证结果" prop="result">
        <el-input v-model="verifyForm.result" type="textarea" :rows="2" placeholder="请输入验证结果" />
      </el-form-item>
      <el-divider content-position="left">电子签名（21 CFR Part 11）</el-divider>
      <el-form-item label="用户名" prop="username">
        <el-input v-model="verifyForm.username" placeholder="请输入当前登录用户名" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="verifyForm.password" type="password" show-password placeholder="请输入密码" />
      </el-form-item>
      <el-form-item label="签名原因" prop="reason">
        <el-input v-model="verifyForm.reason" type="textarea" :rows="2" placeholder="例如：整改完成，现场验证符合要求" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="verifyLoading" type="primary" @click="submitVerify">签名并验证</el-button>
      <el-button @click="verifyVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 关闭不符合项（40 -> 50，受控操作：需电子签名） -->
  <Dialog v-model="closeVisible" title="关闭不符合项（电子签名）" width="480">
    <el-form ref="closeFormRef" :model="closeForm" :rules="closeRules" label-width="110px">
      <el-form-item label="不符合项">
        <span>{{ currentRow?.ncNo }}（{{ currentRow?.description }}）</span>
      </el-form-item>
      <el-divider content-position="left">电子签名（21 CFR Part 11）</el-divider>
      <el-form-item label="用户名" prop="username">
        <el-input v-model="closeForm.username" placeholder="请输入当前登录用户名" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="closeForm.password" type="password" show-password placeholder="请输入密码" />
      </el-form-item>
      <el-form-item label="签名原因" prop="reason">
        <el-input v-model="closeForm.reason" type="textarea" :rows="2" placeholder="例如：不合符合项经验证已关闭" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="closeLoading" type="primary" @click="submitClose">签名并关闭</el-button>
      <el-button @click="closeVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { QmsAuditNonconformityApi } from '@/api/qms/audit/nonconformity'
import QmsAuditNonconformityForm from './QmsAuditNonconformityForm.vue'

defineOptions({ name: 'QmsAuditNonconformity' })

const message = useMessage()

/** 五态状态机（与后端 QmsNcStatusEnum 对齐；§12.218 补齐 20 -> 30 一环前的断链） */
const NcStatus = {
  PENDING: 10,
  RECTIFYING: 20,
  RECTIFIED: 30,
  VERIFIED: 40,
  CLOSED: 50
} as const

const NC_STATUS_OPTIONS = [
  { value: NcStatus.PENDING, label: '待整改' },
  { value: NcStatus.RECTIFYING, label: '整改中' },
  { value: NcStatus.RECTIFIED, label: '已整改' },
  { value: NcStatus.VERIFIED, label: '已验证' },
  { value: NcStatus.CLOSED, label: '已关闭' }
]

const statusText = (status?: number) =>
  NC_STATUS_OPTIONS.find((item) => item.value === status)?.label ?? '-'
const statusTagType = (status?: number) => {
  if (status === NcStatus.CLOSED) return 'success'
  if (status === NcStatus.VERIFIED) return 'warning'
  if (status === NcStatus.RECTIFIED) return 'primary'
  if (status === NcStatus.RECTIFYING) return 'info'
  return 'danger'
}

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  reportId: undefined,
  ncNo: undefined,
  severity: undefined,
  status: undefined,
  responsibleDeptId: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await QmsAuditNonconformityApi.getQmsAuditNonconformityPage(queryParams)
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
    await QmsAuditNonconformityApi.deleteQmsAuditNonconformity(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

const currentRow = ref<any>({})

/** 提交整改措施（10 -> 20） */
const rectifyVisible = ref(false)
const rectifyLoading = ref(false)
const rectifyFormRef = ref()
const rectifyForm = ref<any>({})
const rectifyRules = reactive({
  action: [{ required: true, message: '整改措施不能为空', trigger: 'blur' }]
})

const openRectify = (row: any) => {
  currentRow.value = row
  rectifyForm.value = { action: undefined }
  rectifyVisible.value = true
}

const submitRectify = async () => {
  await rectifyFormRef.value.validate()
  rectifyLoading.value = true
  try {
    await QmsAuditNonconformityApi.rectifyNonconformity({
      id: currentRow.value.id,
      action: rectifyForm.value.action
    })
    message.success('整改措施已提交')
    rectifyVisible.value = false
    await getList()
  } finally {
    rectifyLoading.value = false
  }
}

/** 完成整改（20 -> 30） */
const handleComplete = async (row: any) => {
  await message.confirm(`是否确认完成不符合项「${row.ncNo}」的整改？`)
  await QmsAuditNonconformityApi.completeRectifyNonconformity({ id: row.id })
  message.success('已完成整改')
  await getList()
}

/** 验证整改效果（30 -> 40，电子签名） */
const verifyVisible = ref(false)
const verifyLoading = ref(false)
const verifyFormRef = ref()
const verifyForm = ref<any>({})
const verifyRules = reactive({
  result: [{ required: true, message: '验证结果不能为空', trigger: 'blur' }],
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }],
  reason: [{ required: true, message: '签名原因不能为空', trigger: 'blur' }]
})

const openVerify = (row: any) => {
  currentRow.value = row
  verifyForm.value = { result: undefined, username: undefined, password: undefined, reason: undefined }
  verifyVisible.value = true
}

const submitVerify = async () => {
  await verifyFormRef.value.validate()
  verifyLoading.value = true
  try {
    const { result, username, password, reason } = verifyForm.value
    await QmsAuditNonconformityApi.verifyNonconformity(
      { id: currentRow.value.id, result },
      {
        'X-Electronic-Signature-Username': encodeURIComponent(username),
        'X-Electronic-Signature-Password': encodeURIComponent(password),
        'X-Electronic-Signature-Reason': encodeURIComponent(reason)
      }
    )
    message.success('已验证并签名')
    verifyVisible.value = false
    await getList()
  } finally {
    verifyLoading.value = false
  }
}

/** 关闭不符合项（40 -> 50，电子签名） */
const closeVisible = ref(false)
const closeLoading = ref(false)
const closeFormRef = ref()
const closeForm = ref<any>({})
const closeRules = reactive({
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }],
  reason: [{ required: true, message: '签名原因不能为空', trigger: 'blur' }]
})

const openClose = (row: any) => {
  currentRow.value = row
  closeForm.value = { username: undefined, password: undefined, reason: undefined }
  closeVisible.value = true
}

const submitClose = async () => {
  await closeFormRef.value.validate()
  closeLoading.value = true
  try {
    const { username, password, reason } = closeForm.value
    await QmsAuditNonconformityApi.closeNonconformity(currentRow.value.id, {
      'X-Electronic-Signature-Username': encodeURIComponent(username),
      'X-Electronic-Signature-Password': encodeURIComponent(password),
      'X-Electronic-Signature-Reason': encodeURIComponent(reason)
    })
    message.success('已签名并关闭')
    closeVisible.value = false
    await getList()
  } finally {
    closeLoading.value = false
  }
}

onMounted(() => getList())
</script>
