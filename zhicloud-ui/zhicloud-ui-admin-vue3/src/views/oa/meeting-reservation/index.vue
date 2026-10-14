<!-- OA 会议预约（create/update 服务端悲观锁会议室 + 时段冲突校验；check-conflict 为只读预检） -->
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
      <el-form-item label="会议室" prop="roomId">
        <el-input-number v-model="queryParams.roomId" class="!w-240px" :controls="false" clearable placeholder="请输入会议室编号" />
      </el-form-item>
      <el-form-item label="组织人" prop="organizerUserId">
        <el-input-number v-model="queryParams.organizerUserId" class="!w-240px" :controls="false" clearable placeholder="请输入组织人 ID" />
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
          v-hasPermi="['oa:meeting-reservation:create']"
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
      <el-table-column align="left" label="会议主题" prop="title" min-width="180" show-overflow-tooltip />
      <el-table-column align="center" label="会议室" prop="roomId" min-width="100" />
      <el-table-column align="center" label="组织人" prop="organizerUserId" min-width="100" />
      <el-table-column align="center" label="开始时间" prop="startTime" width="180" />
      <el-table-column align="center" label="结束时间" prop="endTime" width="180" />
      <el-table-column align="center" label="状态" prop="status" min-width="100">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="small">
            {{ statusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column align="center" label="提醒" prop="reminderEnabled" min-width="90">
        <template #default="scope">
          <span v-if="scope.row.reminderEnabled">提前 {{ scope.row.reminderMinutes ?? 0 }} 分钟</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="创建时间" prop="createTime" width="170" :formatter="dateFormatter" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['oa:meeting-reservation:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['oa:meeting-reservation:delete']"
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

  <!-- 表单弹窗（含冲突预检） -->
  <OaMeetingReservationForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { OaMeetingReservationApi, OaMeetingReservationStatus } from '@/api/oa/meeting-reservation'
import OaMeetingReservationForm from './OaMeetingReservationForm.vue'

defineOptions({ name: 'OaMeetingReservation' })

const message = useMessage()

const STATUS_OPTIONS = [
  { value: OaMeetingReservationStatus.PENDING, label: '待确认' },
  { value: OaMeetingReservationStatus.CONFIRMED, label: '已确认' },
  { value: OaMeetingReservationStatus.CANCELLED, label: '已取消' },
  { value: OaMeetingReservationStatus.COMPLETED, label: '已完成' }
]

const statusText = (v?: number) => STATUS_OPTIONS.find((i) => i.value === v)?.label ?? '-'
const statusTagType = (v?: number) => {
  if (v === 40) return 'success'
  if (v === 30) return 'info'
  if (v === 20) return 'primary'
  return 'warning'
}

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  title: undefined,
  roomId: undefined,
  organizerUserId: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await OaMeetingReservationApi.getOaMeetingReservationPage(queryParams)
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
    await OaMeetingReservationApi.deleteOaMeetingReservation(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
