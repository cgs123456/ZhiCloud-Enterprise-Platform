<!-- TMS 承运商 -->
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
      <el-form-item label="承运商名称" prop="name">
        <el-input v-model="queryParams.name" class="!w-240px" clearable placeholder="请输入承运商名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="承运商编码" prop="code">
        <el-input v-model="queryParams.code" class="!w-240px" clearable placeholder="请输入承运商编码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="服务类型（10 快递 / 20 零担 / 30 整车 / 40 空运 / 50 海运）" prop="serviceType">
        <el-input-number v-model="queryParams.serviceType" class="!w-240px" :controls="false" clearable placeholder="请输入服务类型（10 快递 / 20 零担 / 30 整车 / 40 空运 / 50 海运）" />
      </el-form-item>
      <el-form-item label="联系人" prop="contactPerson">
        <el-input v-model="queryParams.contactPerson" class="!w-240px" clearable placeholder="请输入联系人" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="联系电话" prop="contactPhone">
        <el-input v-model="queryParams.contactPhone" class="!w-240px" clearable placeholder="请输入联系电话" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="资质编号" prop="qualificationNo">
        <el-input v-model="queryParams.qualificationNo" class="!w-240px" clearable placeholder="请输入资质编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="评分（1-5）" prop="rating">
        <el-input-number v-model="queryParams.rating" class="!w-240px" :controls="false" clearable placeholder="请输入评分（1-5）" />
      </el-form-item>
      <el-form-item label="状态（10 启用 / 20 停用）" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态（10 启用 / 20 停用）" />
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
          v-hasPermi="['tms:carrier:create']"
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
      <el-table-column align="center" label="承运商名称" prop="name" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="承运商编码" prop="code" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="服务类型（10 快递 / 20 零担 / 30 整车 / 40 空运 / 50 海运）" prop="serviceType" min-width="120" />
      <el-table-column align="center" label="联系人" prop="contactPerson" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="联系电话" prop="contactPhone" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="资质编号" prop="qualificationNo" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="评分（1-5）" prop="rating" min-width="120" />
      <el-table-column align="center" label="状态（10 启用 / 20 停用）" prop="status" min-width="120" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['tms:carrier:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['tms:carrier:delete']"
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
  <TmsCarrierForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { TmsCarrierApi } from '@/api/tms/carrier'
import TmsCarrierForm from './TmsCarrierForm.vue'

defineOptions({ name: 'TmsCarrier' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined
  code: undefined
  serviceType: undefined
  contactPerson: undefined
  contactPhone: undefined
  qualificationNo: undefined
  rating: undefined
  status: undefined
  remark: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await TmsCarrierApi.getTmsCarrierPage(queryParams)
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
    await TmsCarrierApi.deleteTmsCarrier(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
