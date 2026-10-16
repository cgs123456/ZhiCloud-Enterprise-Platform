<!-- QMS 8D报告 -->
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
      <el-form-item label="8D 报告编号" prop="reportNo">
        <el-input v-model="queryParams.reportNo" class="!w-240px" clearable placeholder="请输入8D 报告编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" class="!w-240px" clearable placeholder="请输入标题" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="关联 NCR 编号 ID" prop="ncrId">
        <el-input-number v-model="queryParams.ncrId" class="!w-240px" :controls="false" clearable placeholder="请输入关联 NCR 编号 ID" />
      </el-form-item>
      <el-form-item label="关联 CAPA 编号 ID" prop="capaId">
        <el-input-number v-model="queryParams.capaId" class="!w-240px" :controls="false" clearable placeholder="请输入关联 CAPA 编号 ID" />
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
          v-hasPermi="['qms:eight-d:create']"
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
      <el-table-column align="center" label="8D 报告编号" prop="reportNo" min-width="110" />
      <el-table-column align="center" label="标题" prop="title" min-width="110" />
      <el-table-column align="center" label="关联 NCR 编号 ID" prop="ncrId" min-width="110" />
      <el-table-column align="center" label="关联 CAPA 编号 ID" prop="capaId" min-width="110" />
      <el-table-column align="center" label="状态" min-width="110" prop="status">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.QMS_EIGHT_D_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="D1 团队成员" prop="d1TeamMembers" min-width="110" />
      <el-table-column align="center" label="D2 问题描述" prop="d2ProblemDescription" min-width="120" />
      <el-table-column align="center" label="D3 临时遏制措施" prop="d3InterimAction" min-width="120" />
      <el-table-column align="center" label="D4 根本原因分析" prop="d4RootCause" min-width="120" />
      <el-table-column align="center" label="D5 永久纠正措施" prop="d5PermanentAction" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="280">
        <template #default="scope">
          <!-- 未到 D7：推进阶段（D7 封顶后不再推进） -->
          <el-button
            v-if="(scope.row.status ?? EightDStatus.DRAFT) < EightDStatus.D7_PREVENT"
            v-hasPermi="['qms:eight-d:update']"
            link
            type="primary"
            @click="handleAdvanceStage(scope.row)"
          >
            推进阶段
          </el-button>
          <!-- 未关闭：关闭（电子签名） -->
          <el-button
            v-if="(scope.row.status ?? EightDStatus.DRAFT) < EightDStatus.D8_CLOSED"
            v-hasPermi="['qms:eight-d:update']"
            link
            type="success"
            @click="openClose(scope.row)"
          >
            关闭
          </el-button>
          <el-button
            v-hasPermi="['qms:eight-d:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:eight-d:delete']"
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
  <EightDReportForm ref="formRef" @success="getList" />

  <!-- 关闭 8D 报告（→ D8；受控操作：需电子签名） -->
  <Dialog v-model="closeVisible" title="关闭 8D 报告（电子签名）" width="480">
    <el-form ref="closeFormRef" :model="closeForm" :rules="closeRules" label-width="110px">
      <el-form-item label="8D 报告">
        <span>{{ currentRow?.reportNo }}（{{ currentRow?.title }}）</span>
      </el-form-item>
      <el-divider content-position="left">电子签名（21 CFR Part 11）</el-divider>
      <el-form-item label="用户名" prop="username">
        <el-input v-model="closeForm.username" placeholder="请输入当前登录用户名" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="closeForm.password" type="password" show-password placeholder="请输入密码" />
      </el-form-item>
      <el-form-item label="签名原因" prop="reason">
        <el-input v-model="closeForm.reason" type="textarea" :rows="2" placeholder="例如：8D 各步骤已完成，报告关闭" />
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
import { EightDReportApi } from '@/api/qms/eightd/report'
import { DICT_TYPE } from '@/utils/dict'
import EightDReportForm from './EightDReportForm.vue'

defineOptions({ name: 'EightDReport' })

const message = useMessage()

/** 8D 状态机（与后端 EightDStatusEnum 对齐；§12.239 接线） */
const EightDStatus = {
  DRAFT: 0,
  D1_TEAM: 10,
  D2_PROBLEM: 20,
  D3_INTERIM: 30,
  D4_ROOT_CAUSE: 40,
  D5_PERMANENT: 50,
  D6_IMPLEMENT: 60,
  D7_PREVENT: 70,
  D8_CLOSED: 80
} as const

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  reportNo: undefined,
  title: undefined,
  status: undefined,
  ncrId: undefined,
  capaId: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await EightDReportApi.getEightDReportPage(queryParams)
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
    await EightDReportApi.deleteEightDReport(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

const currentRow = ref<any>({})

/** 推进阶段（D(n) → D(n+1)） */
const handleAdvanceStage = async (row: any) => {
  try {
    await message.confirm(`是否确认推进 8D 报告「${row.reportNo}」到下一阶段？`)
    await EightDReportApi.advanceEightDReportStage(row.id)
    message.success('阶段已推进')
    await getList()
  } catch {}
}

/** 关闭（→ D8 已关闭，电子签名） */
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
    await EightDReportApi.closeEightDReport(currentRow.value.id, {
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
