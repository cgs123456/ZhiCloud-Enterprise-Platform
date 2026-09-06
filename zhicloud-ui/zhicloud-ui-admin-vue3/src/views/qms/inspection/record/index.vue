<!-- QMS 检验记录 -->
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
      <el-form-item label="检验单 ID" prop="orderId">
        <el-input-number v-model="queryParams.orderId" class="!w-240px" :controls="false" clearable placeholder="请输入检验单 ID" />
      </el-form-item>
      <el-form-item label="检验项目 ID" prop="itemId">
        <el-input-number v-model="queryParams.itemId" class="!w-240px" :controls="false" clearable placeholder="请输入检验项目 ID" />
      </el-form-item>
      <el-form-item label="检验结果" prop="result">
        <el-input-number v-model="queryParams.result" class="!w-240px" :controls="false" clearable placeholder="请输入检验结果" />
      </el-form-item>
      <el-form-item label="检验员" prop="inspector">
        <el-input v-model="queryParams.inspector" class="!w-240px" clearable placeholder="请输入检验员" @keyup.enter="handleQuery" />
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
          v-hasPermi="['qms:inspection-record:create']"
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
      <el-table-column align="center" label="检验单 ID" prop="orderId" min-width="110" />
      <el-table-column align="center" label="检验项目 ID" prop="itemId" min-width="110" />
      <el-table-column align="center" label="实测值" prop="measuredValue" min-width="110" />
      <el-table-column align="center" label="检验结果" prop="result" min-width="110" />
      <el-table-column align="center" label="检验员" prop="inspector" min-width="110" />
      <el-table-column align="center" label="检验时间" prop="inspectTime" width="170" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" />
      <el-table-column align="center" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['qms:inspection-record:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:inspection-record:delete']"
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
  <InspectionRecordForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { InspectionRecordApi } from '@/api/qms/inspection/record'
import InspectionRecordForm from './InspectionRecordForm.vue'

defineOptions({ name: 'InspectionRecord' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  orderId: undefined,
  itemId: undefined,
  result: undefined,
  inspector: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await InspectionRecordApi.getInspectionRecordPage(queryParams)
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
    await InspectionRecordApi.deleteInspectionRecord(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
