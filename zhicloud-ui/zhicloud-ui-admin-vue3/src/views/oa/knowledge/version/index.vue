<!-- OA 知识版本（权限串套用文章 oa:knowledge-article:*；删除为物理删除） -->
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
      <el-form-item label="编辑人" prop="editorUserId">
        <el-input-number v-model="queryParams.editorUserId" class="!w-240px" :controls="false" clearable placeholder="请输入编辑人 ID" />
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
          v-hasPermi="['oa:knowledge-article:update']"
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
      <el-table-column align="center" label="文章编号" prop="articleId" min-width="100" />
      <el-table-column align="center" label="版本号" prop="versionNo" min-width="90" />
      <el-table-column align="left" label="标题" prop="title" min-width="200" show-overflow-tooltip />
      <el-table-column align="left" label="摘要" prop="summary" min-width="160" show-overflow-tooltip />
      <el-table-column align="left" label="变更说明" prop="changeLog" min-width="160" show-overflow-tooltip />
      <el-table-column align="center" label="编辑人" prop="editorName" min-width="110" />
      <el-table-column align="center" label="创建时间" prop="createTime" width="170" :formatter="dateFormatter" />
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
            v-hasPermi="['oa:knowledge-article:update']"
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
  <OaKnowledgeVersionForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { OaKnowledgeVersionApi } from '@/api/oa/knowledge-version'
import OaKnowledgeVersionForm from './OaKnowledgeVersionForm.vue'

defineOptions({ name: 'OaKnowledgeVersion' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  articleId: undefined,
  editorUserId: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await OaKnowledgeVersionApi.getOaKnowledgeVersionPage(queryParams)
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
    await OaKnowledgeVersionApi.deleteOaKnowledgeVersion(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
