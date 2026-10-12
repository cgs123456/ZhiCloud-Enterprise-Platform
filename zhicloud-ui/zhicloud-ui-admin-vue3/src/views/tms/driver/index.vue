<!-- TMS 司机 -->
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
      <el-form-item label="司机姓名" prop="name">
        <el-input v-model="queryParams.name" class="!w-240px" clearable placeholder="请输入司机姓名" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="queryParams.phone" class="!w-240px" clearable placeholder="请输入手机号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="驾照号" prop="licenseNo">
        <el-input v-model="queryParams.licenseNo" class="!w-240px" clearable placeholder="请输入驾照号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="准驾车型" prop="licenseType">
        <el-input v-model="queryParams.licenseType" class="!w-240px" clearable placeholder="请输入准驾车型" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="承运商编号" prop="carrierId">
        <el-input-number v-model="queryParams.carrierId" class="!w-240px" :controls="false" clearable placeholder="请输入承运商编号" />
      </el-form-item>
      <el-form-item label="状态（10 可用 / 20 运输中 / 30 休假）" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态（10 可用 / 20 运输中 / 30 休假）" />
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
          v-hasPermi="['tms:driver:create']"
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
      <el-table-column align="center" label="司机姓名" prop="name" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="手机号" prop="phone" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="驾照号" prop="licenseNo" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="准驾车型" prop="licenseType" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="承运商编号" prop="carrierId" min-width="120" />
      <el-table-column align="center" label="状态（10 可用 / 20 运输中 / 30 休假）" prop="status" min-width="120" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['tms:driver:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['tms:driver:delete']"
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
  <TmsDriverForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { TmsDriverApi } from '@/api/tms/driver'
import TmsDriverForm from './TmsDriverForm.vue'

defineOptions({ name: 'TmsDriver' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  phone: undefined,
  licenseNo: undefined,
  licenseType: undefined,
  carrierId: undefined,
  status: undefined,
  remark: undefined,
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await TmsDriverApi.getTmsDriverPage(queryParams)
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
    await TmsDriverApi.deleteTmsDriver(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
