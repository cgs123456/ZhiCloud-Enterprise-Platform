<!-- OA 公文 -->
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
      <el-form-item label="公文编号" prop="no">
        <el-input v-model="queryParams.no" class="!w-240px" clearable placeholder="请输入公文编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" class="!w-240px" clearable placeholder="请输入标题" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="公文类型（10 通知 20 通报 30 报告 40 请示 50 决定 60 批复）" prop="documentType">
        <el-input-number v-model="queryParams.documentType" class="!w-240px" :controls="false" clearable placeholder="请输入公文类型（10 通知 20 通报 30 报告 40 请示 50 决定 60 批复）" />
      </el-form-item>
      <el-form-item label="紧急程度（10 普通 20 紧急 30 特急）" prop="urgency">
        <el-input-number v-model="queryParams.urgency" class="!w-240px" :controls="false" clearable placeholder="请输入紧急程度（10 普通 20 紧急 30 特急）" />
      </el-form-item>
      <el-form-item label="保密级别（10 公开 20 内部 30 秘密）" prop="confidentiality">
        <el-input-number v-model="queryParams.confidentiality" class="!w-240px" :controls="false" clearable placeholder="请输入保密级别（10 公开 20 内部 30 秘密）" />
      </el-form-item>
      <el-form-item label="发文人 ID" prop="issuerUserId">
        <el-input-number v-model="queryParams.issuerUserId" class="!w-240px" :controls="false" clearable placeholder="请输入发文人 ID" />
      </el-form-item>
      <el-form-item label="发文部门 ID" prop="issueDeptId">
        <el-input-number v-model="queryParams.issueDeptId" class="!w-240px" :controls="false" clearable placeholder="请输入发文部门 ID" />
      </el-form-item>
      <el-form-item label="正文内容" prop="content">
        <el-input v-model="queryParams.content" class="!w-240px" clearable placeholder="请输入正文内容" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="工作流编号" prop="processInstanceId">
        <el-input v-model="queryParams.processInstanceId" class="!w-240px" clearable placeholder="请输入工作流编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态（10 草稿 20 审核中 30 已发布 40 已废止）" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态（10 草稿 20 审核中 30 已发布 40 已废止）" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="queryParams.remark" class="!w-240px" clearable placeholder="请输入备注" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="核稿人 ID" prop="reviewerUserId">
        <el-input-number v-model="queryParams.reviewerUserId" class="!w-240px" :controls="false" clearable placeholder="请输入核稿人 ID" />
      </el-form-item>
      <el-form-item label="核稿人姓名" prop="reviewerName">
        <el-input v-model="queryParams.reviewerName" class="!w-240px" clearable placeholder="请输入核稿人姓名" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="核稿意见" prop="reviewOpinion">
        <el-input v-model="queryParams.reviewOpinion" class="!w-240px" clearable placeholder="请输入核稿意见" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="签发人 ID" prop="signerUserId">
        <el-input-number v-model="queryParams.signerUserId" class="!w-240px" :controls="false" clearable placeholder="请输入签发人 ID" />
      </el-form-item>
      <el-form-item label="签发人姓名" prop="signerName">
        <el-input v-model="queryParams.signerName" class="!w-240px" clearable placeholder="请输入签发人姓名" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="签发意见" prop="signOpinion">
        <el-input v-model="queryParams.signOpinion" class="!w-240px" clearable placeholder="请输入签发意见" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="归档人 ID" prop="archiverUserId">
        <el-input-number v-model="queryParams.archiverUserId" class="!w-240px" :controls="false" clearable placeholder="请输入归档人 ID" />
      </el-form-item>
      <el-form-item label="归档人姓名" prop="archiverName">
        <el-input v-model="queryParams.archiverName" class="!w-240px" clearable placeholder="请输入归档人姓名" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="归档编号" prop="archiveNo">
        <el-input v-model="queryParams.archiveNo" class="!w-240px" clearable placeholder="请输入归档编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="主送部门（逗号分隔 ID）" prop="mainSendDepts">
        <el-input v-model="queryParams.mainSendDepts" class="!w-240px" clearable placeholder="请输入主送部门（逗号分隔 ID）" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="抄送部门（逗号分隔 ID）" prop="copySendDepts">
        <el-input v-model="queryParams.copySendDepts" class="!w-240px" clearable placeholder="请输入抄送部门（逗号分隔 ID）" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="阅读量" prop="readCount">
        <el-input-number v-model="queryParams.readCount" class="!w-240px" :controls="false" clearable placeholder="请输入阅读量" />
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
          v-hasPermi="['oa:document:create']"
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
      <el-table-column align="center" label="公文编号" prop="no" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="标题" prop="title" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="公文类型（10 通知 20 通报 30 报告 40 请示 50 决定 60 批复）" prop="documentType" min-width="120" />
      <el-table-column align="center" label="紧急程度（10 普通 20 紧急 30 特急）" prop="urgency" min-width="120" />
      <el-table-column align="center" label="保密级别（10 公开 20 内部 30 秘密）" prop="confidentiality" min-width="120" />
      <el-table-column align="center" label="发文人 ID" prop="issuerUserId" min-width="120" />
      <el-table-column align="center" label="发文部门 ID" prop="issueDeptId" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="发文日期" prop="issueDate" width="170" />
      <el-table-column align="center" label="正文内容" prop="content" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="工作流编号" prop="processInstanceId" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="状态（10 草稿 20 审核中 30 已发布 40 已废止）" prop="status" min-width="120" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="核稿人 ID" prop="reviewerUserId" min-width="120" />
      <el-table-column align="center" label="核稿人姓名" prop="reviewerName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="核稿时间" prop="reviewTime" width="170" />
      <el-table-column align="center" label="核稿意见" prop="reviewOpinion" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="签发人 ID" prop="signerUserId" min-width="120" />
      <el-table-column align="center" label="签发人姓名" prop="signerName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="签发时间" prop="signTime" width="170" />
      <el-table-column align="center" label="签发意见" prop="signOpinion" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="归档人 ID" prop="archiverUserId" min-width="120" />
      <el-table-column align="center" label="归档人姓名" prop="archiverName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="归档时间" prop="archiveTime" width="170" />
      <el-table-column align="center" label="归档编号" prop="archiveNo" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="主送部门（逗号分隔 ID）" prop="mainSendDepts" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="抄送部门（逗号分隔 ID）" prop="copySendDepts" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="阅读量" prop="readCount" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['oa:document:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['oa:document:delete']"
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
  <OaDocumentForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { OaDocumentApi } from '@/api/oa/document'
import OaDocumentForm from './OaDocumentForm.vue'

defineOptions({ name: 'OaDocument' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  no: undefined
  title: undefined
  documentType: undefined
  urgency: undefined
  confidentiality: undefined
  issuerUserId: undefined
  issueDeptId: undefined
  issueDate: undefined
  content: undefined
  processInstanceId: undefined
  status: undefined
  remark: undefined
  reviewerUserId: undefined
  reviewerName: undefined
  reviewTime: undefined
  reviewOpinion: undefined
  signerUserId: undefined
  signerName: undefined
  signTime: undefined
  signOpinion: undefined
  archiverUserId: undefined
  archiverName: undefined
  archiveTime: undefined
  archiveNo: undefined
  mainSendDepts: undefined
  copySendDepts: undefined
  readCount: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await OaDocumentApi.getOaDocumentPage(queryParams)
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
    await OaDocumentApi.deleteOaDocument(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
