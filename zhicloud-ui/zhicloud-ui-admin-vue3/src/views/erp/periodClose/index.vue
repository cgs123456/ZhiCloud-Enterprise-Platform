<!-- ERP 期末处理 -->
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
      <el-form-item label="年度" prop="year">
        <el-input-number v-model="queryParams.year" class="!w-240px" :controls="false" clearable placeholder="请输入年度" />
      </el-form-item>
      <el-form-item label="月份" prop="month">
        <el-input-number v-model="queryParams.month" class="!w-240px" :controls="false" clearable placeholder="请输入月份" />
      </el-form-item>
      <el-form-item label="期间编码" prop="code">
        <el-input v-model="queryParams.code" class="!w-240px" clearable placeholder="请输入期间编码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="状态名称" prop="statusName">
        <el-input v-model="queryParams.statusName" class="!w-240px" clearable placeholder="请输入状态名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="关账人" prop="closedBy">
        <el-input v-model="queryParams.closedBy" class="!w-240px" clearable placeholder="请输入关账人" @keyup.enter="handleQuery" />
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
          v-hasPermi="['erp:period:create']"
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
      <el-table-column align="center" label="年度" prop="year" min-width="120" />
      <el-table-column align="center" label="月份" prop="month" min-width="120" />
      <el-table-column align="center" label="期间编码" prop="code" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="起始日期" prop="startDate" width="170" />
      <el-table-column align="center" :formatter="dateFormatter" label="结束日期" prop="endDate" width="170" />
      <el-table-column align="center" label="状态" prop="status" min-width="120" />
      <el-table-column align="center" label="状态名称" prop="statusName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="关账人" prop="closedBy" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="关账时间" prop="closedTime" width="170" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['erp:period:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['erp:period:delete']"
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
  <ErpPeriodForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { ErpPeriodApi } from '@/api/erp/period'
import ErpPeriodForm from './ErpPeriodForm.vue'

defineOptions({ name: 'ErpPeriod' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  year: undefined,
  month: undefined,
  code: undefined,
  startDate: undefined,
  endDate: undefined,
  status: undefined,
  statusName: undefined,
  closedBy: undefined,
  closedTime: undefined,
  remark: undefined,
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await ErpPeriodApi.getErpPeriodPage(queryParams)
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
    await ErpPeriodApi.deleteErpPeriod(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
