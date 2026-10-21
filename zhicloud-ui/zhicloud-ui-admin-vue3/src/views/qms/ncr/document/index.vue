<!-- QMS 不合格品NCR -->
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
      <el-form-item label="NCR 单号" prop="ncrNo">
        <el-input v-model="queryParams.ncrNo" class="!w-240px" clearable placeholder="请输入NCR 单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="来源" prop="source">
        <el-input-number v-model="queryParams.source" class="!w-240px" :controls="false" clearable placeholder="请输入来源" />
      </el-form-item>
      <el-form-item label="缺陷等级" prop="defectLevel">
        <el-input-number v-model="queryParams.defectLevel" class="!w-240px" :controls="false" clearable placeholder="请输入缺陷等级" />
      </el-form-item>
      <el-form-item label="处置方式" prop="disposition">
        <el-input-number v-model="queryParams.disposition" class="!w-240px" :controls="false" clearable placeholder="请输入处置方式" />
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
          v-hasPermi="['qms:ncr:create']"
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
      <el-table-column align="center" label="NCR 单号" prop="ncrNo" min-width="110" />
      <el-table-column align="center" label="来源" prop="source" min-width="110" />
      <el-table-column align="center" label="缺陷描述" prop="defectDescription" min-width="120" />
      <el-table-column align="center" label="缺陷等级" prop="defectLevel" min-width="110" />
      <el-table-column align="center" label="不合格数量" prop="quantity" min-width="110" />
      <el-table-column align="center" label="处置方式" min-width="110" prop="disposition">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.QMS_NCR_DISPOSITION" :value="scope.row.disposition" />
        </template>
      </el-table-column>
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="300">
        <template #default="scope">
          <el-button
            v-hasPermi="['qms:ncr:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-if="scope.row.status === 10"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:ncr:disposition']"
            link
            type="warning"
            @click="handleSubmitMrb(scope.row)"
            v-if="scope.row.status === 10"
          >
            提交 MRB 评审
          </el-button>
          <el-button
            v-hasPermi="['qms:ncr:disposition']"
            link
            type="primary"
            @click="openDisposition(scope.row)"
            v-if="scope.row.status === 20"
          >
            记录处置决议
          </el-button>
          <el-button
            v-hasPermi="['qms:ncr:close']"
            link
            type="success"
            @click="openSign(scope.row)"
            v-if="scope.row.status === 30"
          >
            关闭报告
          </el-button>
          <el-button
            v-hasPermi="['qms:ncr:query']"
            link
            type="info"
            @click="openMrbRecords(scope.row)"
          >
            MRB 记录
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
  <NcrDocumentForm ref="formRef" @success="getList" />

  <!-- MRB 处置决议弹窗（status = MRB 评审中 时可用） -->
  <Dialog v-model="dispositionVisible" title="记录 MRB 处置决议" width="560">
    <el-form ref="dispositionFormRef" :model="dispositionForm" :rules="dispositionRules" label-width="120px" v-loading="dispositionLoading">
      <el-form-item label="NCR 单号">
        <el-input :model-value="currentRow?.ncrNo" disabled class="!w-1/1" />
      </el-form-item>
      <el-form-item label="评审日期" prop="mrbDate">
        <el-date-picker v-model="dispositionForm.mrbDate" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="评审日期" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="评审成员" prop="mrbMembers">
        <el-input v-model="dispositionForm.mrbMembers" placeholder="参会评审成员，逗号分隔" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="处置决议" prop="decision">
        <el-radio-group v-model="dispositionForm.decision">
          <el-radio :value="10">同意返工</el-radio>
          <el-radio :value="20">同意返修</el-radio>
          <el-radio :value="30">同意降级</el-radio>
          <el-radio :value="40">报废</el-radio>
          <el-radio :value="50">让步接收</el-radio>
          <el-radio :value="60">退货</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="条件条款" prop="conditionTerms">
        <el-input v-model="dispositionForm.conditionTerms" type="textarea" :rows="2" placeholder="让步接收/返工的具体条件与要求" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="dispositionForm.remark" type="textarea" :rows="2" placeholder="备注" class="!w-1/1" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :disabled="dispositionLoading" @click="submitDisposition">提交决议</el-button>
      <el-button @click="dispositionVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- MRB 评审记录弹窗 -->
  <Dialog v-model="mrbVisible" title="MRB 评审记录" width="640">
    <el-table :data="mrbRecords" v-loading="mrbLoading" stripe>
      <el-table-column align="center" label="评审日期" prop="mrbDate" width="170" />
      <el-table-column align="center" label="评审成员" prop="mrbMembers" min-width="160" show-overflow-tooltip />
      <el-table-column align="center" label="决议" prop="decision" width="100">
        <template #default="scope">
          <span>{{ formatDecision(scope.row.decision) }}</span>
        </template>
      </el-table-column>
      <el-table-column align="center" label="条件条款" prop="conditionTerms" min-width="160" show-overflow-tooltip />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
    </el-table>
    <el-empty v-if="!mrbLoading && mrbRecords.length === 0" description="暂无 MRB 评审记录" />
  </Dialog>

  <!-- 电子签名弹窗：关闭 NCR 属于受控操作，需按 21 CFR Part 11 留下签名人身份 -->
  <Dialog v-model="signVisible" title="电子签名确认" width="460">
    <el-alert type="info" :closable="false" show-icon class="mb-10px"
      title="关闭不合格品报告需要电子签名，签名人必须为当前登录用户本人" />
    <el-form ref="signFormRef" :model="signForm" :rules="signRules" label-width="110px" v-loading="signLoading">
      <el-form-item label="当前操作">
        <el-input :model-value="currentRow?.ncrNo" disabled class="!w-1/1" />
      </el-form-item>
      <el-form-item label="用户名" prop="username">
        <el-input v-model="signForm.username" placeholder="请输入当前登录用户名" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="signForm.password" type="password" show-password placeholder="请输入密码" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="签名原因" prop="reason">
        <el-input v-model="signForm.reason" type="textarea" :rows="2" placeholder="例如：不合格品报告复核通过，予以关闭" class="!w-1/1" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :disabled="signLoading" @click="submitSign">签名并关闭</el-button>
      <el-button @click="signVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { NcrDocumentApi } from '@/api/qms/ncr/document'
import { DICT_TYPE } from '@/utils/dict'
import NcrDocumentForm from './NcrDocumentForm.vue'

defineOptions({ name: 'NcrDocument' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  ncrNo: undefined,
  source: undefined,
  defectLevel: undefined,
  disposition: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await NcrDocumentApi.getNcrDocumentPage(queryParams)
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

// ========== NCR 状态机：10 待处理 -> 20 MRB评审中 -> 30 已处置 -> 40 已关闭 ==========
const currentRow = ref<any>(null)

/** 提交 MRB 评审（10 -> 20） */
const handleSubmitMrb = async (row: any) => {
  try {
    await message.confirm(`确认将 NCR「${row.ncrNo}」提交 MRB 评审？`)
    await NcrDocumentApi.submitForMrb(row.id)
    message.success('已提交 MRB 评审')
    await getList()
  } catch {}
}

// ---- 记录处置决议（20 -> 30） ----
const dispositionVisible = ref(false)
const dispositionLoading = ref(false)
const dispositionFormRef = ref()
const dispositionForm = ref<any>({})
const dispositionRules = reactive({
  mrbDate: [{ required: true, message: '评审日期不能为空', trigger: 'change' }],
  mrbMembers: [{ required: true, message: '评审成员不能为空', trigger: 'blur' }],
  decision: [{ required: true, message: '处置决议不能为空', trigger: 'change' }]
})

const openDisposition = (row: any) => {
  currentRow.value = row
  dispositionForm.value = {
    ncrId: row.id,
    mrbDate: undefined,
    mrbMembers: undefined,
    decision: undefined,
    conditionTerms: undefined,
    remark: undefined
  }
  dispositionVisible.value = true
}

const submitDisposition = async () => {
  await dispositionFormRef.value.validate()
  dispositionLoading.value = true
  try {
    await NcrDocumentApi.recordDisposition(dispositionForm.value)
    message.success('已记录处置决议')
    dispositionVisible.value = false
    await getList()
  } finally {
    dispositionLoading.value = false
  }
}

/** 关闭报告（30 -> 40） */
// ---- 关闭（受控操作：需电子签名，21 CFR Part 11） ----
const signVisible = ref(false)
const signLoading = ref(false)
const signFormRef = ref()
const signForm = ref<any>({})
const signRules = reactive({
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }],
  reason: [{ required: true, message: '签名原因不能为空', trigger: 'blur' }]
})

const openSign = (row: any) => {
  currentRow.value = row
  signForm.value = { username: undefined, password: undefined, reason: undefined }
  signVisible.value = true
}

const submitSign = async () => {
  await signFormRef.value.validate()
  signLoading.value = true
  try {
    await NcrDocumentApi.closeNcrDocument(currentRow.value.id, signForm.value)
    message.success('已签名并关闭')
    signVisible.value = false
    await getList()
  } finally {
    signLoading.value = false
  }
}

// ---- MRB 评审记录 ----
const mrbVisible = ref(false)
const mrbLoading = ref(false)
const mrbRecords = ref<any[]>([])

const openMrbRecords = async (row: any) => {
  currentRow.value = row
  mrbVisible.value = true
  mrbLoading.value = true
  try {
    mrbRecords.value = (await NcrDocumentApi.getMrbRecordList(row.id)) || []
  } finally {
    mrbLoading.value = false
  }
}

const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}


/** MRB 处置决议文案（后端 decision：1 让步接收 / 2 挑选返工 / 3 报废退换） */
const DISPOSITION_TEXT: Record<number, string> = { 10: '同意返工', 20: '同意返修', 30: '同意降级', 40: '报废', 50: '让步接收', 60: '退货' }
const formatDecision = (v: any) => DISPOSITION_TEXT[v] ?? '-'

onMounted(() => getList())
</script>
