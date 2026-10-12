<template>
  <ContentWrap>
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="设备" prop="equipmentId">
        <el-input-number v-model="queryParams.equipmentId" placeholder="请输入设备" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="计划编号" prop="planNo">
        <el-input v-model="queryParams.planNo" placeholder="请输入计划编号" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['mes:dv:tp-plan:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="设备" align="center" prop="equipmentId" />
      <el-table-column label="计划编号" align="center" prop="planNo" />
      <el-table-column label="计划类型" align="center" prop="planType" />
      <el-table-column label="周期类型" align="center" prop="cycleType" />
      <el-table-column label="周期值" align="center" prop="cycleValue" />
      <el-table-column label="下次执行日期" align="center" prop="nextExecuteDate" />
      <el-table-column label="状态" align="center" prop="status" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="排序" align="center" prop="sort" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="260">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-if="scope.row.status === 20" v-hasPermi="['mes:dv:tp-plan:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-if="scope.row.status === 20" v-hasPermi="['mes:dv:tp-plan:delete']">删除</el-button>
          <el-button link type="success" @click="handleEnable(scope.row)" v-if="scope.row.status === 20" v-hasPermi="['mes:dv:tp-plan:update']">启用</el-button>
          <el-button link type="warning" @click="handleDisable(scope.row)" v-if="scope.row.status === 10" v-hasPermi="['mes:dv:tp-plan:update']">禁用</el-button>
          <el-button link type="info" @click="handleExecute(scope.row)" v-if="scope.row.status === 10" v-hasPermi="['mes:dv:tp-plan:update']">立即执行</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <TpPlanForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { TpPlanApi } from '@/api/mes/dv/tp-plan'
import TpPlanForm from './tpPlanForm.vue'

defineOptions({ name: 'MesTpPlan' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  equipmentId: undefined,
  planNo: undefined,
})
const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await TpPlanApi.getTpPlanPage(queryParams)
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

const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await TpPlanApi.deleteTpPlan(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

/** 启用计划（状态 20 -> 10） */
const handleEnable = async (row: any) => {
  try {
    await message.confirm(`确认启用计划「${row.planNo}」？`)
    await TpPlanApi.TpPlanEnable(row.id)
    message.success('已启用')
    await getList()
  } catch {}
}

/** 禁用计划（状态 10 -> 20） */
const handleDisable = async (row: any) => {
  try {
    await message.confirm(`确认禁用计划「${row.planNo}」？`)
    await TpPlanApi.TpPlanDisable(row.id)
    message.success('已禁用')
    await getList()
  } catch {}
}

/** 立即执行一次计划 */
const handleExecute = async (row: any) => {
  try {
    await message.confirm(`确认立即执行计划「${row.planNo}」？`)
    await TpPlanApi.TpPlanExecute(row.id)
    message.success('已触发生成执行记录')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
