<!-- QMS 供应商评级 -->
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
      <el-form-item label="评级编号" prop="ratingNo">
        <el-input v-model="queryParams.ratingNo" class="!w-240px" clearable placeholder="请输入评级编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="供应商 ID" prop="supplierId">
        <el-input-number v-model="queryParams.supplierId" class="!w-240px" :controls="false" clearable placeholder="请输入供应商 ID" />
      </el-form-item>
      <el-form-item label="评级周期" prop="ratingPeriod">
        <el-input v-model="queryParams.ratingPeriod" class="!w-240px" clearable placeholder="请输入评级周期" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="供应商等级" prop="grade">
        <el-input v-model="queryParams.grade" class="!w-240px" clearable placeholder="请输入供应商等级" @keyup.enter="handleQuery" />
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
          v-hasPermi="['qms:supplier-rating:create']"
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
      <el-table-column align="center" label="评级编号" prop="ratingNo" min-width="110" />
      <el-table-column align="center" label="供应商 ID" prop="supplierId" min-width="110" />
      <el-table-column align="center" label="供应商名称" prop="supplierName" min-width="110" />
      <el-table-column align="center" label="评级周期" prop="ratingPeriod" min-width="110" />
      <el-table-column align="center" label="PPM 缺陷率" prop="ppm" min-width="110" />
      <el-table-column align="center" label="交期达成率" prop="onTimeRate" min-width="110" />
      <el-table-column align="center" label="质量合格率" prop="qualityRate" min-width="110" />
      <el-table-column align="center" label="供应商等级" prop="grade" min-width="110" />
      <el-table-column align="center" label="评级日期" prop="ratingDate" min-width="110" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['qms:supplier-rating:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:supplier-rating:delete']"
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
  <SupplierRatingForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { SupplierRatingApi } from '@/api/qms/sqm/supplier-rating'
import SupplierRatingForm from './SupplierRatingForm.vue'

defineOptions({ name: 'SupplierRating' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  ratingNo: undefined,
  supplierId: undefined,
  ratingPeriod: undefined,
  grade: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await SupplierRatingApi.getSupplierRatingPage(queryParams)
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
    await SupplierRatingApi.deleteSupplierRating(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
