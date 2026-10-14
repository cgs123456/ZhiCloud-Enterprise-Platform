<!-- CRM 线索渠道 -->
<template>
  <ContentWrap>
    <doc-alert title="【线索】渠道管理" url="https://doc.zhicloud.cn/crm/clue-channel/" />
    <!-- 搜索工作栏 -->
    <el-form
      ref="queryFormRef"
      :inline="true"
      :model="queryParams"
      class="-mb-15px"
      label-width="100px"
    >
      <el-form-item label="渠道名称" prop="channelName">
        <el-input v-model="queryParams.channelName" class="!w-240px" clearable placeholder="请输入渠道名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="渠道编码" prop="channelCode">
        <el-input v-model="queryParams.channelCode" class="!w-240px" clearable placeholder="请输入渠道编码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="渠道类型" prop="channelType">
        <el-select v-model="queryParams.channelType" class="!w-240px" clearable placeholder="请选择渠道类型">
          <el-option v-for="item in TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-240px" clearable placeholder="请选择状态">
          <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
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
          v-hasPermi="['crm:clue-channel:create']"
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
      <el-table-column align="center" label="渠道编码" prop="channelCode" min-width="130" show-overflow-tooltip />
      <el-table-column align="left" label="渠道名称" prop="channelName" min-width="150" show-overflow-tooltip />
      <el-table-column align="center" label="渠道类型" prop="channelType" min-width="110">
        <template #default="scope">{{ typeText(scope.row.channelType) }}</template>
      </el-table-column>
      <el-table-column align="left" label="API 地址" prop="apiUrl" min-width="160" show-overflow-tooltip />
      <el-table-column align="center" label="自动分配" prop="autoAssignUserName" min-width="110" />
      <el-table-column align="center" label="状态" prop="status" min-width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === 10 ? 'success' : 'info'" size="small">
            {{ scope.row.status === 10 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="创建时间" prop="createTime" width="170" :formatter="dateFormatter" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['crm:clue-channel:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['crm:clue-channel:delete']"
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
  <CrmClueChannelForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { CrmClueChannelApi } from '@/api/crm/clue/channel'
import CrmClueChannelForm from './CrmClueChannelForm.vue'

defineOptions({ name: 'CrmClueChannel' })

const message = useMessage()

const TYPE_OPTIONS = [
  { value: 10, label: '官网' },
  { value: 20, label: '广告' },
  { value: 30, label: '企微' },
  { value: 40, label: '小程序' },
  { value: 50, label: 'API' },
  { value: 60, label: '线下' }
]

const STATUS_OPTIONS = [
  { value: 10, label: '启用' },
  { value: 20, label: '停用' }
]

const typeText = (v?: number) => TYPE_OPTIONS.find((i) => i.value === v)?.label ?? '-'

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  channelName: undefined,
  channelType: undefined,
  channelCode: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await CrmClueChannelApi.getCrmClueChannelPage(queryParams)
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
    await CrmClueChannelApi.deleteCrmClueChannel(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
