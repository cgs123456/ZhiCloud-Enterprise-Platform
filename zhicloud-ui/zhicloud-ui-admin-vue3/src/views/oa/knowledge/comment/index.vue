<!-- OA 知识文章评论（树形；删除为软删除） -->
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
      <el-form-item label="文章编号" prop="articleId">
        <el-input-number v-model="queryParams.articleId" class="!w-240px" :controls="false" clearable placeholder="请输入文章编号" />
      </el-form-item>
      <el-form-item label="评论人" prop="commentatorUserId">
        <el-input-number v-model="queryParams.commentatorUserId" class="!w-240px" :controls="false" clearable placeholder="请输入评论人 ID" />
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
          v-hasPermi="['oa:knowledge-comment:create']"
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
    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" :stripe="true" row-key="id" :tree-props="{ children: 'children' }">
      <el-table-column align="center" label="文章编号" prop="articleId" min-width="100" />
      <el-table-column align="center" label="评论人" prop="commentatorName" min-width="110" />
      <el-table-column align="left" label="评论内容" prop="content" min-width="260" show-overflow-tooltip />
      <el-table-column align="right" label="点赞" prop="likeCount" min-width="80" />
      <el-table-column align="center" label="状态" prop="status" min-width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'info' : 'success'" size="small">
            {{ scope.row.status === 1 ? '已删除' : '正常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column align="center" label="创建时间" prop="createTime" width="170" :formatter="dateFormatter" />
      <el-table-column align="center" fixed="right" label="操作" width="160">
        <template #default="scope">
          <el-button
            v-hasPermi="['oa:knowledge-comment:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['oa:knowledge-comment:delete']"
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 树形模式（按文章查询）不展示分页；平铺模式分页 -->
    <Pagination
      v-if="!isTreeMode"
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNo"
      :total="total"
      @pagination="getList"
    />
    <el-empty v-if="isTreeMode && list.length === 0" description="输入文章编号查看该文章的评论树" />
  </ContentWrap>

  <!-- 表单弹窗 -->
  <OaKnowledgeCommentForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { OaKnowledgeCommentApi } from '@/api/oa/knowledge-comment'
import OaKnowledgeCommentForm from './OaKnowledgeCommentForm.vue'

defineOptions({ name: 'OaKnowledgeComment' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
/** 树形模式：填了文章编号时走 /list 拿评论树（后端返回根评论列表 + children 嵌套） */
const isTreeMode = computed(() => !!queryParams.articleId)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  articleId: undefined,
  parentId: undefined,
  status: undefined,
  commentatorUserId: undefined
})
const queryFormRef = ref()

const getList = async () => {
  if (isTreeMode.value) {
    // 树形：按文章查全量评论树（无分页）
    loading.value = true
    try {
      list.value = (await OaKnowledgeCommentApi.getCommentTreeByArticle(queryParams.articleId!)) || []
    } finally {
      loading.value = false
    }
    return
  }
  // 平铺：全站分页
  loading.value = true
  try {
    const data = await OaKnowledgeCommentApi.getOaKnowledgeCommentPage(queryParams)
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
    await OaKnowledgeCommentApi.deleteOaKnowledgeComment(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
