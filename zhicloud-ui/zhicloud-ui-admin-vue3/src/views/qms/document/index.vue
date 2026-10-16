<!-- QMS 受控文档 -->
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
      <el-form-item label="文件编号" prop="docNo">
        <el-input v-model="queryParams.docNo" class="!w-240px" clearable placeholder="请输入文件编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" class="!w-240px" clearable placeholder="请输入标题" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="文件类型" prop="docType">
        <el-input-number v-model="queryParams.docType" class="!w-240px" :controls="false" clearable placeholder="请输入文件类型" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="归属部门 ID" prop="ownerDeptId">
        <el-input-number v-model="queryParams.ownerDeptId" class="!w-240px" :controls="false" clearable placeholder="请输入归属部门 ID" />
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
          v-hasPermi="['qms:document:create']"
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
      <el-table-column align="center" label="文件编号" prop="docNo" min-width="110" />
      <el-table-column align="center" label="标题" prop="title" min-width="110" />
      <el-table-column align="center" label="文件类型" prop="docType" min-width="110" />
      <el-table-column align="center" label="版本号" prop="version" min-width="110" />
      <el-table-column align="center" label="状态" min-width="110" prop="status">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.QMS_DOC_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="生效日期" prop="effectiveDate" min-width="110" />
      <el-table-column align="center" label="失效日期" prop="expiryDate" min-width="110" />
      <el-table-column align="center" label="审批人 ID" prop="approverId" min-width="110" />
      <el-table-column align="center" label="审批日期" prop="approveDate" width="170" />
      <el-table-column align="center" label="归属部门 ID" prop="ownerDeptId" min-width="110" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="300">
        <template #default="scope">
          <!-- 10 草稿：提交审批 -->
          <el-button
            v-if="scope.row.status === DocStatus.DRAFT"
            v-hasPermi="['qms:document:approve']"
            link
            type="primary"
            @click="handleSubmit(scope.row)"
          >
            提交审批
          </el-button>
          <!-- 20 待审批：通过（电子签名） -->
          <el-button
            v-if="scope.row.status === DocStatus.PENDING_APPROVAL"
            v-hasPermi="['qms:document:approve']"
            link
            type="success"
            @click="openApprove(scope.row)"
          >
            通过
          </el-button>
          <!-- 20 待审批：驳回（电子签名） -->
          <el-button
            v-if="scope.row.status === DocStatus.PENDING_APPROVAL"
            v-hasPermi="['qms:document:approve']"
            link
            type="warning"
            @click="openReject(scope.row)"
          >
            驳回
          </el-button>
          <!-- 30 已发布：作废 -->
          <el-button
            v-if="scope.row.status === DocStatus.PUBLISHED"
            v-hasPermi="['qms:document:revoke']"
            link
            type="danger"
            @click="handleRevoke(scope.row)"
          >
            作废
          </el-button>
          <el-button
            v-hasPermi="['qms:document:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:document:delete']"
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
  <QmsDocumentForm ref="formRef" @success="getList" />

  <!-- 审批通过并发布（20 待审批 → 30 已发布；受控操作：需电子签名） -->
  <Dialog v-model="approveVisible" title="审批通过并发布（电子签名）" width="520">
    <el-form ref="approveFormRef" :model="approveForm" :rules="approveRules" label-width="110px">
      <el-form-item label="受控文档">
        <span>{{ currentRow?.docNo }}（{{ currentRow?.title }}）</span>
      </el-form-item>
      <el-form-item label="定稿文件 URL" prop="fileUrl">
        <el-input v-model="approveForm.fileUrl" placeholder="可选：审批通过时同时更新文件地址" />
      </el-form-item>
      <el-divider content-position="left">电子签名（21 CFR Part 11）</el-divider>
      <el-form-item label="用户名" prop="username">
        <el-input v-model="approveForm.username" placeholder="请输入当前登录用户名" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="approveForm.password" type="password" show-password placeholder="请输入密码" />
      </el-form-item>
      <el-form-item label="签名原因" prop="reason">
        <el-input v-model="approveForm.reason" type="textarea" :rows="2" placeholder="例如：文件内容审核符合要求，批准发布" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="approveLoading" type="primary" @click="submitApprove">签名并发布</el-button>
      <el-button @click="approveVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 驳回（20 待审批 → 10 草稿；受控操作：需电子签名） -->
  <Dialog v-model="rejectVisible" title="驳回受控文档（电子签名）" width="520">
    <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="110px">
      <el-form-item label="受控文档">
        <span>{{ currentRow?.docNo }}（{{ currentRow?.title }}）</span>
      </el-form-item>
      <el-form-item label="驳回原因" prop="reason">
        <el-input v-model="rejectForm.reason" type="textarea" :rows="3" placeholder="请输入驳回原因（将写入文档备注）" />
      </el-form-item>
      <el-divider content-position="left">电子签名（21 CFR Part 11）</el-divider>
      <el-form-item label="用户名" prop="username">
        <el-input v-model="rejectForm.username" placeholder="请输入当前登录用户名" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="rejectForm.password" type="password" show-password placeholder="请输入密码" />
      </el-form-item>
      <el-form-item label="签名原因" prop="signReason">
        <el-input v-model="rejectForm.signReason" type="textarea" :rows="2" placeholder="例如：文件存在未关闭的修订项，予以驳回" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="rejectLoading" type="primary" @click="submitReject">签名并驳回</el-button>
      <el-button @click="rejectVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { QmsDocumentApi } from '@/api/qms/document'
import { DICT_TYPE } from '@/utils/dict'
import QmsDocumentForm from './QmsDocumentForm.vue'

defineOptions({ name: 'QmsDocument' })

const message = useMessage()

/** 文档状态机（与后端 QmsDocStatusEnum 对齐；§12.239 接线审批闭环） */
const DocStatus = {
  DRAFT: 10,
  PENDING_APPROVAL: 20,
  PUBLISHED: 30,
  OBSOLETE: 40
} as const

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  docNo: undefined,
  title: undefined,
  docType: undefined,
  status: undefined,
  ownerDeptId: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await QmsDocumentApi.getQmsDocumentPage(queryParams)
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
    await QmsDocumentApi.deleteQmsDocument(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

const currentRow = ref<any>({})

/** 提交审批（10 草稿 → 20 待审批） */
const handleSubmit = async (row: any) => {
  try {
    await message.confirm(`是否确认提交受控文档「${row.docNo}」进入审批？`)
    await QmsDocumentApi.submitQmsDocument(row.id)
    message.success('已提交审批')
    await getList()
  } catch {}
}

/** 审批通过并发布（20 待审批 → 30 已发布，电子签名） */
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
  approveForm.value = {
    fileUrl: row.fileUrl ?? undefined,
    username: undefined,
    password: undefined,
    reason: undefined
  }
  approveVisible.value = true
}

const submitApprove = async () => {
  await approveFormRef.value.validate()
  approveLoading.value = true
  try {
    const { fileUrl, username, password, reason } = approveForm.value
    await QmsDocumentApi.approveQmsDocument(currentRow.value.id, fileUrl || undefined, {
      'X-Electronic-Signature-Username': encodeURIComponent(username),
      'X-Electronic-Signature-Password': encodeURIComponent(password),
      'X-Electronic-Signature-Reason': encodeURIComponent(reason)
    })
    message.success('已签名并发布')
    approveVisible.value = false
    await getList()
  } finally {
    approveLoading.value = false
  }
}

/** 驳回（20 待审批 → 10 草稿，电子签名 + 驳回原因） */
const rejectVisible = ref(false)
const rejectLoading = ref(false)
const rejectFormRef = ref()
const rejectForm = ref<any>({})
const rejectRules = reactive({
  reason: [{ required: true, message: '驳回原因不能为空', trigger: 'blur' }],
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }],
  signReason: [{ required: true, message: '签名原因不能为空', trigger: 'blur' }]
})

const openReject = (row: any) => {
  currentRow.value = row
  rejectForm.value = {
    reason: undefined,
    username: undefined,
    password: undefined,
    signReason: undefined
  }
  rejectVisible.value = true
}

const submitReject = async () => {
  await rejectFormRef.value.validate()
  rejectLoading.value = true
  try {
    const { reason, username, password, signReason } = rejectForm.value
    await QmsDocumentApi.rejectQmsDocument(
      { id: currentRow.value.id, reason },
      {
        'X-Electronic-Signature-Username': encodeURIComponent(username),
        'X-Electronic-Signature-Password': encodeURIComponent(password),
        'X-Electronic-Signature-Reason': encodeURIComponent(signReason)
      }
    )
    message.success('已签名并驳回')
    rejectVisible.value = false
    await getList()
  } finally {
    rejectLoading.value = false
  }
}

/** 作废（30 已发布 → 40 已作废） */
const handleRevoke = async (row: any) => {
  try {
    await message.confirm(`是否确认作废受控文档「${row.docNo}」？作废后不可再使用。`)
    await QmsDocumentApi.revokeQmsDocument(row.id)
    message.success('已作废')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
