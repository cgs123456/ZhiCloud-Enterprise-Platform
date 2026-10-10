<!-- OA 会议室 -->
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
      <el-form-item label="会议主题" prop="title">
        <el-input v-model="queryParams.title" class="!w-240px" clearable placeholder="请输入会议主题" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="会议室 ID" prop="roomId">
        <el-input-number v-model="queryParams.roomId" class="!w-240px" :controls="false" clearable placeholder="请输入会议室 ID" />
      </el-form-item>
      <el-form-item label="组织人 ID" prop="organizerUserId">
        <el-input-number v-model="queryParams.organizerUserId" class="!w-240px" :controls="false" clearable placeholder="请输入组织人 ID" />
      </el-form-item>
      <el-form-item label="参会人（逗号分隔）" prop="attendeeUserIds">
        <el-input v-model="queryParams.attendeeUserIds" class="!w-240px" clearable placeholder="请输入参会人（逗号分隔）" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态（10 待确认 20 已确认 30 已取消 40 已完成）" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态（10 待确认 20 已确认 30 已取消 40 已完成）" />
      </el-form-item>
      <el-form-item label="提前提醒分钟" prop="reminderMinutes">
        <el-input-number v-model="queryParams.reminderMinutes" class="!w-240px" :controls="false" clearable placeholder="请输入提前提醒分钟" />
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
          v-hasPermi="['oa:meeting-room:create']"
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
      <el-table-column align="center" label="会议主题" prop="title" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="会议室 ID" prop="roomId" min-width="120" />
      <el-table-column align="center" label="组织人 ID" prop="organizerUserId" min-width="120" />
      <el-table-column align="center" label="参会人（逗号分隔）" prop="attendeeUserIds" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="开始时间" prop="startTime" width="170" />
      <el-table-column align="center" :formatter="dateFormatter" label="结束时间" prop="endTime" width="170" />
      <el-table-column align="center" label="状态（10 待确认 20 已确认 30 已取消 40 已完成）" prop="status" min-width="120" />
      <el-table-column align="center" label="是否提醒" prop="reminderEnabled" min-width="90" />
      <el-table-column align="center" label="提前提醒分钟" prop="reminderMinutes" min-width="120" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['oa:meeting-room:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['oa:meeting-room:delete']"
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
  <OaMeetingRoomForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { OaMeetingRoomApi } from '@/api/oa/meeting-room'
import OaMeetingRoomForm from './OaMeetingRoomForm.vue'

defineOptions({ name: 'OaMeetingRoom' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  title: undefined
  roomId: undefined
  organizerUserId: undefined
  attendeeUserIds: undefined
  startTime: undefined
  endTime: undefined
  status: undefined
  reminderEnabled: undefined
  reminderMinutes: undefined
  remark: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await OaMeetingRoomApi.getOaMeetingRoomPage(queryParams)
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
    await OaMeetingRoomApi.deleteOaMeetingRoom(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
