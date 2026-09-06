<!-- QMS 质量成本 -->
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
      <el-form-item label="成本类型" prop="costType">
        <el-input v-model="queryParams.costType" class="!w-240px" clearable placeholder="请输入成本类型" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="成本类别" prop="costCategory">
        <el-input v-model="queryParams.costCategory" class="!w-240px" clearable placeholder="请输入成本类别" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="成本项目" prop="costItem">
        <el-input v-model="queryParams.costItem" class="!w-240px" clearable placeholder="请输入成本项目" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="年度" prop="periodYear">
        <el-input-number v-model="queryParams.periodYear" class="!w-240px" :controls="false" clearable placeholder="请输入年度" />
      </el-form-item>
      <el-form-item label="月份（1-12）" prop="periodMonth">
        <el-input-number v-model="queryParams.periodMonth" class="!w-240px" :controls="false" clearable placeholder="请输入月份（1-12）" />
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
          v-hasPermi="['qms:quality-cost:create']"
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
      <el-table-column align="center" label="成本类型" prop="costType" min-width="110" />
      <el-table-column align="center" label="成本类别" prop="costCategory" min-width="110" />
      <el-table-column align="center" label="成本项目" prop="costItem" min-width="110" />
      <el-table-column align="center" label="金额" prop="amount" min-width="110" />
      <el-table-column align="center" label="年度" prop="periodYear" min-width="110" />
      <el-table-column align="center" label="月份（1-12）" prop="periodMonth" min-width="110" />
      <el-table-column align="center" label="关联业务 ID" prop="relatedId" min-width="110" />
      <el-table-column align="center" label="关联业务类型（EIGHT_D/NCR/CAPA）" prop="relatedType" min-width="110" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" />
      <el-table-column align="center" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['qms:quality-cost:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:quality-cost:delete']"
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
  <QmsQualityCostForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { QmsQualityCostApi } from '@/api/qms/quality-cost'
import QmsQualityCostForm from './QmsQualityCostForm.vue'

defineOptions({ name: 'QmsQualityCost' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  costType: undefined,
  costCategory: undefined,
  costItem: undefined,
  periodYear: undefined,
  periodMonth: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await QmsQualityCostApi.getQmsQualityCostPage(queryParams)
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
    await QmsQualityCostApi.deleteQmsQualityCost(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
