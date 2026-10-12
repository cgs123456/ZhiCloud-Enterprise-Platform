<!-- OA 知识文章 -->
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
      <el-form-item label="分类 ID" prop="categoryId">
        <el-input-number v-model="queryParams.categoryId" class="!w-240px" :controls="false" clearable placeholder="请输入分类 ID" />
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" class="!w-240px" clearable placeholder="请输入标题" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="摘要" prop="summary">
        <el-input v-model="queryParams.summary" class="!w-240px" clearable placeholder="请输入摘要" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="正文（Markdown/HTML）" prop="content">
        <el-input v-model="queryParams.content" class="!w-240px" clearable placeholder="请输入正文（Markdown/HTML）" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="标签（逗号分隔）" prop="tags">
        <el-input v-model="queryParams.tags" class="!w-240px" clearable placeholder="请输入标签（逗号分隔）" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="作者 ID" prop="authorUserId">
        <el-input-number v-model="queryParams.authorUserId" class="!w-240px" :controls="false" clearable placeholder="请输入作者 ID" />
      </el-form-item>
      <el-form-item label="作者姓名" prop="authorName">
        <el-input v-model="queryParams.authorName" class="!w-240px" clearable placeholder="请输入作者姓名" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="阅读量" prop="viewCount">
        <el-input-number v-model="queryParams.viewCount" class="!w-240px" :controls="false" clearable placeholder="请输入阅读量" />
      </el-form-item>
      <el-form-item label="点赞数" prop="likeCount">
        <el-input-number v-model="queryParams.likeCount" class="!w-240px" :controls="false" clearable placeholder="请输入点赞数" />
      </el-form-item>
      <el-form-item label="评论数" prop="commentCount">
        <el-input-number v-model="queryParams.commentCount" class="!w-240px" :controls="false" clearable placeholder="请输入评论数" />
      </el-form-item>
      <el-form-item label="当前版本号" prop="currentVersion">
        <el-input-number v-model="queryParams.currentVersion" class="!w-240px" :controls="false" clearable placeholder="请输入当前版本号" />
      </el-form-item>
      <el-form-item label="状态（10 草稿 20 已发布 30 已下架）" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态（10 草稿 20 已发布 30 已下架）" />
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
          v-hasPermi="['oa:knowledge-article:create']"
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
      <el-table-column align="center" label="分类 ID" prop="categoryId" min-width="120" />
      <el-table-column align="center" label="标题" prop="title" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="摘要" prop="summary" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="正文（Markdown/HTML）" prop="content" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="标签（逗号分隔）" prop="tags" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="作者 ID" prop="authorUserId" min-width="120" />
      <el-table-column align="center" label="作者姓名" prop="authorName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="阅读量" prop="viewCount" min-width="120" />
      <el-table-column align="center" label="点赞数" prop="likeCount" min-width="120" />
      <el-table-column align="center" label="评论数" prop="commentCount" min-width="120" />
      <el-table-column align="center" label="当前版本号" prop="currentVersion" min-width="120" />
      <el-table-column align="center" label="状态（10 草稿 20 已发布 30 已下架）" prop="status" min-width="120" />
      <el-table-column align="center" label="是否置顶" prop="topFlag" min-width="90" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['oa:knowledge-article:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['oa:knowledge-article:delete']"
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
  <OaKnowledgeArticleForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { OaKnowledgeArticleApi } from '@/api/oa/knowledge-article'
import OaKnowledgeArticleForm from './OaKnowledgeArticleForm.vue'

defineOptions({ name: 'OaKnowledgeArticle' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  categoryId: undefined,
  title: undefined,
  summary: undefined,
  content: undefined,
  tags: undefined,
  authorUserId: undefined,
  authorName: undefined,
  viewCount: undefined,
  likeCount: undefined,
  commentCount: undefined,
  currentVersion: undefined,
  status: undefined,
  topFlag: undefined,
  remark: undefined,
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await OaKnowledgeArticleApi.getOaKnowledgeArticlePage(queryParams)
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
    await OaKnowledgeArticleApi.deleteOaKnowledgeArticle(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
