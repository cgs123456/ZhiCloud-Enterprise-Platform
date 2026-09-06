<!-- QMS 检验项目 -->
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
      <el-form-item label="检验项目编码" prop="code">
        <el-input v-model="queryParams.code" class="!w-240px" clearable placeholder="请输入检验项目编码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="检验项目名称" prop="name">
        <el-input v-model="queryParams.name" class="!w-240px" clearable placeholder="请输入检验项目名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="检验类型" prop="type">
        <el-input-number v-model="queryParams.type" class="!w-240px" :controls="false" clearable placeholder="请输入检验类型" />
      </el-form-item>
      <el-form-item label="检验方法" prop="method">
        <el-input-number v-model="queryParams.method" class="!w-240px" :controls="false" clearable placeholder="请输入检验方法" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
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
          v-hasPermi="['qms:inspection-item:create']"
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
      <el-table-column align="center" label="检验项目编码" prop="code" min-width="110" />
      <el-table-column align="center" label="检验项目名称" prop="name" min-width="110" />
      <el-table-column align="center" label="检验类型" prop="type" min-width="110" />
      <el-table-column align="center" label="检验方法" prop="method" min-width="110" />
      <el-table-column align="center" label="检验标准" prop="standard" min-width="110" />
      <el-table-column align="center" label="目标值" prop="target" min-width="110" />
      <el-table-column align="center" label="上限" prop="upperLimit" min-width="110" />
      <el-table-column align="center" label="下限" prop="lowerLimit" min-width="110" />
      <el-table-column align="center" label="单位" prop="unit" min-width="110" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['qms:inspection-item:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:inspection-item:delete']"
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
  <InspectionItemForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { InspectionItemApi } from '@/api/qms/inspection/item'
import InspectionItemForm from './InspectionItemForm.vue'

defineOptions({ name: 'InspectionItem' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  code: undefined,
  name: undefined,
  type: undefined,
  method: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await InspectionItemApi.getInspectionItemPage(queryParams)
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
    await InspectionItemApi.deleteInspectionItem(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
