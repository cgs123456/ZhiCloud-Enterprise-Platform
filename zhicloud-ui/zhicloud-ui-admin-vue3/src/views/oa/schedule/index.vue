<!-- OA 日程（状态只能通过完成/取消端点流转，通用更新禁止改状态） -->
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
      <el-form-item label="用户编号" prop="userId">
        <el-input-number v-model="queryParams.userId" class="!w-240px" :controls="false" clearable placeholder="请输入用户编号" />
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" class="!w-240px" clearable placeholder="请输入标题" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-select v-model="queryParams.type" class="!w-240px" clearable placeholder="请选择类型">
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
          v-hasPermi="['oa:schedule:create']"
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
      <el-table-column align="left" label="标题" prop="title" min-width="180" show-overflow-tooltip />
      <el-table-column align="center" label="用户编号" prop="userId" min-width="100" />
      <el-table-column align="center" label="类型" prop="type" min-width="100">
        <template #default="scope">{{ typeText(scope.row.type) }}</template>
      </el-table-column>
      <el-table-column align="center" label="开始时间" prop="startTime" width="180" />
      <el-table-column align="center" label="结束时间" prop="endTime" width="180" />
      <el-table-column align="center" label="全天" prop="allDay" min-width="70">
        <template #default="scope">{{ scope.row.allDay ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column align="center" label="地点" prop="location" min-width="130" show-overflow-tooltip />
      <el-table-column align="center" label="重复" prop="repeatType" min-width="90">
        <template #default="scope">{{ repeatText(scope.row.repeatType) }}</template>
      </el-table-column>
      <el-table-column align="center" label="状态" prop="status" min-width="100">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="small">
            {{ statusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column align="fixed right" label="操作" fixed="right" width="260">
        <template #default="scope">
          <!-- 未完成：可完成/取消 -->
          <template v-if="scope.row.status === STATUS.UNFINISHED">
            <el-button
              v-hasPermi="['oa:schedule:update']"
              link
              type="success"
              @click="handleComplete(scope.row)"
            >
              完成
            </el-button>
            <el-button
              v-hasPermi="['oa:schedule:update']"
              link
              type="warning"
              @click="handleCancel(scope.row)"
            >
              取消
            </el-button>
          </template>
          <el-button
            v-hasPermi="['oa:schedule:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['oa:schedule:delete']"
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
  <OaScheduleForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { OaScheduleApi, OaScheduleStatus } from '@/api/oa/schedule'
import OaScheduleForm from './OaScheduleForm.vue'

defineOptions({ name: 'OaSchedule' })

const message = useMessage()

const STATUS = OaScheduleStatus

const TYPE_OPTIONS = [
  { value: 10, label: '日程' },
  { value: 20, label: '任务' },
  { value: 30, label: '纪念日' },
  { value: 40, label: '会议' }
]

const STATUS_OPTIONS = [
  { value: OaScheduleStatus.UNFINISHED, label: '未完成' },
  { value: OaScheduleStatus.COMPLETED, label: '已完成' },
  { value: OaScheduleStatus.CANCELLED, label: '已取消' }
]

const REPEAT_OPTIONS = [
  { value: 0, label: '不重复' },
  { value: 10, label: '每天' },
  { value: 20, label: '每周' },
  { value: 30, label: '每月' },
  { value: 40, label: '每年' }
]

const typeText = (v?: number) => TYPE_OPTIONS.find((i) => i.value === v)?.label ?? '-'
const repeatText = (v?: number) => REPEAT_OPTIONS.find((i) => i.value === v)?.label ?? '-'
const statusText = (v?: number) => STATUS_OPTIONS.find((i) => i.value === v)?.label ?? '-'
const statusTagType = (v?: number) => {
  if (v === OaScheduleStatus.COMPLETED) return 'success'
  if (v === OaScheduleStatus.CANCELLED) return 'info'
  return 'warning'
}

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  userId: undefined,
  title: undefined,
  type: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await OaScheduleApi.getOaSchedulePage(queryParams)
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
    await OaScheduleApi.deleteOaSchedule(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

const handleComplete = async (row: any) => {
  await message.confirm(`是否确认完成日程「${row.title}」？`)
  try {
    await OaScheduleApi.completeOaSchedule(row.id)
    message.success('已完成')
    await getList()
  } catch {}
}

const handleCancel = async (row: any) => {
  await message.confirm(`是否确认取消日程「${row.title}」？`)
  try {
    await OaScheduleApi.cancelOaSchedule(row.id)
    message.success('已取消')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
