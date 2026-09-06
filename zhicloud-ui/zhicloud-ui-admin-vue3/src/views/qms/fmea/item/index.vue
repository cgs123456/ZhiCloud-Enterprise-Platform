<!-- QMS FMEA分析项 -->
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
      <el-form-item label="FMEA 文档 ID" prop="fmeaId">
        <el-input-number v-model="queryParams.fmeaId" class="!w-240px" :controls="false" clearable placeholder="请输入FMEA 文档 ID" />
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
          v-hasPermi="['qms:fmea:create']"
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
      <el-table-column align="center" label="FMEA 文档 ID" prop="fmeaId" min-width="110" />
      <el-table-column align="center" label="功能" prop="function" min-width="110" />
      <el-table-column align="center" label="失效模式" prop="failureMode" min-width="110" />
      <el-table-column align="center" label="失效后果" prop="failureEffect" min-width="110" />
      <el-table-column align="center" label="严重度 S（1-10）" prop="severity" min-width="110" />
      <el-table-column align="center" label="潜在失效原因" prop="potentialCause" min-width="120" />
      <el-table-column align="center" label="频度 O（1-10）" prop="occurrence" min-width="110" />
      <el-table-column align="center" label="现行控制措施" prop="currentControls" min-width="120" />
      <el-table-column align="center" label="探测度 D（1-10）" prop="detection" min-width="110" />
      <el-table-column align="center" label="风险优先数 RPN = S * O * D" prop="rpn" min-width="110" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['qms:fmea:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:fmea:delete']"
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
  <FmeaItemForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { FmeaItemApi } from '@/api/qms/fmea/item'
import FmeaItemForm from './FmeaItemForm.vue'

defineOptions({ name: 'FmeaItem' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  fmeaId: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await FmeaItemApi.getFmeaItemPage(queryParams)
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
    await FmeaItemApi.deleteFmeaItem(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
