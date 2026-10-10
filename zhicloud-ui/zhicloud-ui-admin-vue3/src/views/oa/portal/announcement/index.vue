<!-- OA 公告 -->
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
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" class="!w-240px" clearable placeholder="请输入标题" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="正文内容" prop="content">
        <el-input v-model="queryParams.content" class="!w-240px" clearable placeholder="请输入正文内容" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="公告分类（company 公司/dept 部门/policy 制度/notice 通知）" prop="category">
        <el-input v-model="queryParams.category" class="!w-240px" clearable placeholder="请输入公告分类（company 公司/dept 部门/policy 制度/notice 通知）" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="优先级（10 普通 20 重要 30 紧急）" prop="priority">
        <el-input-number v-model="queryParams.priority" class="!w-240px" :controls="false" clearable placeholder="请输入优先级（10 普通 20 重要 30 紧急）" />
      </el-form-item>
      <el-form-item label="发布人用户 ID" prop="publisherUserId">
        <el-input-number v-model="queryParams.publisherUserId" class="!w-240px" :controls="false" clearable placeholder="请输入发布人用户 ID" />
      </el-form-item>
      <el-form-item label="发布人姓名" prop="publisherName">
        <el-input v-model="queryParams.publisherName" class="!w-240px" clearable placeholder="请输入发布人姓名" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="目标范围（all 全员/dept_id 列表逗号分隔）" prop="targetScope">
        <el-input v-model="queryParams.targetScope" class="!w-240px" clearable placeholder="请输入目标范围（all 全员/dept_id 列表逗号分隔）" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态（10 草稿 20 已发布 30 已下架 40 已过期）" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态（10 草稿 20 已发布 30 已下架 40 已过期）" />
      </el-form-item>
      <el-form-item label="浏览次数" prop="viewCount">
        <el-input-number v-model="queryParams.viewCount" class="!w-240px" :controls="false" clearable placeholder="请输入浏览次数" />
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
          v-hasPermi="['oa:announcement:create']"
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
      <el-table-column align="center" label="标题" prop="title" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="正文内容" prop="content" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="公告分类（company 公司/dept 部门/policy 制度/notice 通知）" prop="category" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="优先级（10 普通 20 重要 30 紧急）" prop="priority" min-width="120" />
      <el-table-column align="center" label="发布人用户 ID" prop="publisherUserId" min-width="120" />
      <el-table-column align="center" label="发布人姓名" prop="publisherName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="目标范围（all 全员/dept_id 列表逗号分隔）" prop="targetScope" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="发布时间" prop="publishTime" width="170" />
      <el-table-column align="center" :formatter="dateFormatter" label="过期时间" prop="expireTime" width="170" />
      <el-table-column align="center" label="是否置顶" prop="topFlag" min-width="90" />
      <el-table-column align="center" label="状态（10 草稿 20 已发布 30 已下架 40 已过期）" prop="status" min-width="120" />
      <el-table-column align="center" label="浏览次数" prop="viewCount" min-width="120" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['oa:announcement:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['oa:announcement:delete']"
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
  <OaAnnouncementForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { OaAnnouncementApi } from '@/api/oa/announcement'
import OaAnnouncementForm from './OaAnnouncementForm.vue'

defineOptions({ name: 'OaAnnouncement' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  title: undefined
  content: undefined
  category: undefined
  priority: undefined
  publisherUserId: undefined
  publisherName: undefined
  targetScope: undefined
  publishTime: undefined
  expireTime: undefined
  topFlag: undefined
  status: undefined
  viewCount: undefined
  remark: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await OaAnnouncementApi.getOaAnnouncementPage(queryParams)
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
    await OaAnnouncementApi.deleteOaAnnouncement(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
