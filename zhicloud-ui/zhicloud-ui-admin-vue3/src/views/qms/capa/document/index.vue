<!-- QMS 纠正预防措施CAPA -->
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
      <el-form-item label="CAPA 单号" prop="capaNo">
        <el-input v-model="queryParams.capaNo" class="!w-240px" clearable placeholder="请输入CAPA 单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="来源" prop="source">
        <el-input-number v-model="queryParams.source" class="!w-240px" :controls="false" clearable placeholder="请输入来源" />
      </el-form-item>
      <el-form-item label="优先级" prop="priority">
        <el-input-number v-model="queryParams.priority" class="!w-240px" :controls="false" clearable placeholder="请输入优先级" />
      </el-form-item>
      <el-form-item label="当前阶段" prop="stage">
        <el-input-number v-model="queryParams.stage" class="!w-240px" :controls="false" clearable placeholder="请输入当前阶段" />
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
          v-hasPermi="['qms:capa:create']"
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
      <el-table-column align="center" label="CAPA 单号" prop="capaNo" min-width="110" />
      <el-table-column align="center" label="来源" prop="source" min-width="110" />
      <el-table-column align="center" label="优先级" prop="priority" min-width="110" />
      <el-table-column align="center" label="当前阶段" prop="stage" min-width="110" />
      <el-table-column align="center" label="问题描述" prop="problem" min-width="120" />
      <el-table-column align="center" label="原因" prop="cause" min-width="120" />
      <el-table-column align="center" label="根本原因分析" prop="rootCauseAnalysis" min-width="120" />
      <el-table-column align="center" label="纠正措施" prop="correctiveAction" min-width="120" />
      <el-table-column align="center" label="预防措施" prop="preventiveAction" min-width="120" />
      <el-table-column align="center" label="责任人" prop="responsiblePerson" min-width="110" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="380">
        <template #default="scope">
          <!-- 10–40：推进阶段（target = 当前阶段 + 10；后端校验对应字段已填写） -->
          <el-button
            v-if="(scope.row.stage ?? CapaStage.CREATED) < CapaStage.VERIFICATION"
            v-hasPermi="['qms:capa:transition']"
            link
            type="primary"
            @click="handleAdvance(scope.row)"
          >
            推进阶段
          </el-button>
          <!-- 50 验证阶段：提交验证结果 -->
          <el-button
            v-if="scope.row.stage === CapaStage.VERIFICATION"
            v-hasPermi="['qms:capa:verify']"
            link
            type="warning"
            @click="openVerify(scope.row)"
          >
            验证
          </el-button>
          <!-- 50 验证阶段：退回纠正措施（清空验证结果） -->
          <el-button
            v-if="scope.row.stage === CapaStage.VERIFICATION"
            v-hasPermi="['qms:capa:transition']"
            link
            type="danger"
            @click="handleBackToCorrective(scope.row)"
          >
            退回纠正
          </el-button>
          <!-- 50 验证阶段且验证通过：关闭（电子签名） -->
          <el-button
            v-if="
              scope.row.stage === CapaStage.VERIFICATION &&
              scope.row.verificationResult === CAPAVerificationResult.PASSED
            "
            v-hasPermi="['qms:capa:close']"
            link
            type="success"
            @click="openClose(scope.row)"
          >
            关闭
          </el-button>
          <el-button
            v-hasPermi="['qms:capa:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:capa:delete']"
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
  <CAPADocumentForm ref="formRef" @success="getList" />

  <!-- 提交验证结果（50 验证阶段） -->
  <Dialog v-model="verifyVisible" title="提交验证结果" width="520">
    <el-form ref="verifyFormRef" :model="verifyForm" :rules="verifyRules" label-width="110px">
      <el-form-item label="CAPA 单号">
        <span>{{ currentRow?.capaNo }}</span>
      </el-form-item>
      <el-form-item label="验证结果" prop="verificationResult">
        <el-select v-model="verifyForm.verificationResult" class="!w-1/1" placeholder="请选择验证结果">
          <el-option label="通过" :value="CAPAVerificationResult.PASSED" />
          <el-option label="不通过（自动退回纠正措施阶段）" :value="CAPAVerificationResult.FAILED" />
        </el-select>
      </el-form-item>
      <el-form-item label="验证说明" prop="verificationComment">
        <el-input v-model="verifyForm.verificationComment" type="textarea" :rows="3" placeholder="请输入验证说明" />
      </el-form-item>
      <el-form-item label="验证人" prop="verifiedBy">
        <el-input v-model="verifyForm.verifiedBy" placeholder="可选：验证人姓名" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="verifyLoading" type="primary" @click="submitVerify">提 交</el-button>
      <el-button @click="verifyVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 关闭 CAPA（50 验证通过；受控操作：需电子签名） -->
  <Dialog v-model="closeVisible" title="关闭 CAPA（电子签名）" width="480">
    <el-form ref="closeFormRef" :model="closeForm" :rules="closeRules" label-width="110px">
      <el-form-item label="CAPA 单号">
        <span>{{ currentRow?.capaNo }}</span>
      </el-form-item>
      <el-divider content-position="left">电子签名（21 CFR Part 11）</el-divider>
      <el-form-item label="用户名" prop="username">
        <el-input v-model="closeForm.username" placeholder="请输入当前登录用户名" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="closeForm.password" type="password" show-password placeholder="请输入密码" />
      </el-form-item>
      <el-form-item label="签名原因" prop="reason">
        <el-input v-model="closeForm.reason" type="textarea" :rows="2" placeholder="例如：验证通过，CAPA 关闭" />
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
import { CAPADocumentApi } from '@/api/qms/capa/document'
import CAPADocumentForm from './CAPADocumentForm.vue'

defineOptions({ name: 'CAPADocument' })

const message = useMessage()

/** CAPA 阶段机（与后端 CAPAStageEnum 对齐；§12.239 接线） */
const CapaStage = {
  CREATED: 10,
  ROOT_CAUSE_ANALYSIS: 20,
  CORRECTIVE_ACTION: 30,
  PREVENTIVE_ACTION: 40,
  VERIFICATION: 50,
  CLOSED: 60
} as const

/** 验证结果（与后端 CAPAVerificationResultEnum 对齐） */
const CAPAVerificationResult = {
  PASSED: 20,
  FAILED: 30
} as const

/** 推进目标阶段的展示名（仅用于确认文案） */
const CAPA_STAGE_LABEL: Record<number, string> = {
  20: '根因分析',
  30: '纠正措施',
  40: '预防措施',
  50: '验证'
}

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  capaNo: undefined,
  source: undefined,
  priority: undefined,
  stage: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await CAPADocumentApi.getCAPADocumentPage(queryParams)
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
    await CAPADocumentApi.deleteCAPADocument(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

const currentRow = ref<any>({})

/** 推进阶段（target = 当前 + 10；字段未填时后端拒绝并提示） */
const handleAdvance = async (row: any) => {
  const current = row.stage ?? CapaStage.CREATED
  const target = current + 10
  try {
    await message.confirm(
      `是否确认将 CAPA「${row.capaNo}」推进到「${CAPA_STAGE_LABEL[target] ?? target}」阶段？`
    )
    await CAPADocumentApi.transitionCAPADocument({ id: row.id, targetStage: target })
    message.success('阶段已推进')
    await getList()
  } catch {}
}

/** 退回纠正措施（50 → 30；后端会清空验证结果） */
const handleBackToCorrective = async (row: any) => {
  try {
    await message.confirm(`是否确认将 CAPA「${row.capaNo}」退回「纠正措施」阶段？验证结果将被清空。`)
    await CAPADocumentApi.transitionCAPADocument({
      id: row.id,
      targetStage: CapaStage.CORRECTIVE_ACTION
    })
    message.success('已退回纠正措施阶段')
    await getList()
  } catch {}
}

/** 提交验证结果（50 验证阶段） */
const verifyVisible = ref(false)
const verifyLoading = ref(false)
const verifyFormRef = ref()
const verifyForm = ref<any>({})
const verifyRules = reactive({
  verificationResult: [{ required: true, message: '验证结果不能为空', trigger: 'change' }],
  verificationComment: [{ required: true, message: '验证说明不能为空', trigger: 'blur' }]
})

const openVerify = (row: any) => {
  currentRow.value = row
  verifyForm.value = {
    verificationResult: undefined,
    verificationComment: undefined,
    verifiedBy: undefined
  }
  verifyVisible.value = true
}

const submitVerify = async () => {
  await verifyFormRef.value.validate()
  verifyLoading.value = true
  try {
    await CAPADocumentApi.verifyCAPADocument({
      id: currentRow.value.id,
      verificationResult: verifyForm.value.verificationResult,
      verificationComment: verifyForm.value.verificationComment,
      verifiedBy: verifyForm.value.verifiedBy || undefined
    })
    if (verifyForm.value.verificationResult === CAPAVerificationResult.FAILED) {
      message.success('验证不通过，已退回纠正措施阶段')
    } else {
      message.success('验证通过，可执行关闭')
    }
    verifyVisible.value = false
    await getList()
  } finally {
    verifyLoading.value = false
  }
}

/** 关闭（50 验证通过；电子签名） */
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
    await CAPADocumentApi.closeCAPADocument(currentRow.value.id, {
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
