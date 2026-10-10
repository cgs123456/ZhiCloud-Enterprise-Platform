<!-- 系统 - 主数据跨域对照（MDM 映射登记，upsert 语义） -->
<template>
  <ContentWrap>
    <el-alert
      class="mb-12px"
      type="info"
      :closable="false"
      title="对照表表达「当前映射」：同一源键重复登记为覆盖（幂等 upsert）。失效为软删除，保留历史但不再参与解析。"
    />
    <el-form ref="queryFormRef" :inline="true" :model="queryParams" class="-mb-15px" label-width="100px">
      <el-form-item label="源系统" prop="sourceSystem">
        <el-input v-model="queryParams.sourceSystem" class="!w-180px" clearable placeholder="如 ERP" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="源实体" prop="sourceEntity">
        <el-input v-model="queryParams.sourceEntity" class="!w-180px" clearable placeholder="如 PRODUCT" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="目标系统" prop="targetSystem">
        <el-input v-model="queryParams.targetSystem" class="!w-180px" clearable placeholder="如 WMS" @keyup.enter="handleQuery" />
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
          v-hasPermi="['system:mdm-cross-ref:create']"
          plain
          type="primary"
          @click="openForm()"
        >
          <Icon class="mr-5px" icon="ep:plus" />
          登记对照
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" :stripe="true">
      <el-table-column align="center" label="源系统" prop="sourceSystem" min-width="110" />
      <el-table-column align="center" label="源实体" prop="sourceEntity" min-width="120" />
      <el-table-column align="center" label="源编号" prop="sourceId" min-width="130" />
      <el-table-column align="center" label="目标系统" prop="targetSystem" min-width="110" />
      <el-table-column align="center" label="目标实体" prop="targetEntity" min-width="120" />
      <el-table-column align="center" label="目标编号" prop="targetId" min-width="130" />
      <el-table-column align="center" label="状态" prop="status" min-width="90" />
      <el-table-column align="center" label="备注" prop="remark" min-width="140" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="100">
        <template #default="scope">
          <el-button
            v-hasPermi="['system:mdm-cross-ref:create']"
            link
            type="danger"
            @click="handleInvalidate(scope.row)"
          >
            失效
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <MdmCrossRefForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { MdmCrossRefApi } from '@/api/system/mdm-cross-ref'
import MdmCrossRefForm from './MdmCrossRefForm.vue'

defineOptions({ name: 'MdmCrossRef' })

const message = useMessage()
const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  sourceSystem: undefined,
  sourceEntity: undefined,
  targetSystem: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await MdmCrossRefApi.getCrossRefPage(queryParams)
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
const openForm = () => formRef.value.open()

const handleInvalidate = async (row: any) => {
  try {
    await message.delConfirm()
    await MdmCrossRefApi.invalidateCrossRef({
      sourceSystem: row.sourceSystem,
      sourceEntity: row.sourceEntity,
      sourceId: row.sourceId,
      targetSystem: row.targetSystem,
      targetEntity: row.targetEntity
    })
    message.success('已失效')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
