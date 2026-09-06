<!-- QMS 计量器具台账 -->
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
      <el-form-item label="器具编号" prop="code">
        <el-input v-model="queryParams.code" class="!w-240px" clearable placeholder="请输入器具编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="器具名称" prop="name">
        <el-input v-model="queryParams.name" class="!w-240px" clearable placeholder="请输入器具名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="类别" prop="category">
        <el-input-number v-model="queryParams.category" class="!w-240px" :controls="false" clearable placeholder="请输入类别" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="生产厂家" prop="manufacturer">
        <el-input v-model="queryParams.manufacturer" class="!w-240px" clearable placeholder="请输入生产厂家" @keyup.enter="handleQuery" />
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
          v-hasPermi="['qms:instrument:create']"
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
      <el-table-column align="center" label="器具编号" prop="code" min-width="110" />
      <el-table-column align="center" label="器具名称" prop="name" min-width="110" />
      <el-table-column align="center" label="型号规格" prop="model" min-width="110" />
      <el-table-column align="center" label="生产厂家" prop="manufacturer" min-width="110" />
      <el-table-column align="center" label="出厂编号" prop="serialNo" min-width="110" />
      <el-table-column align="center" label="类别" prop="category" min-width="110" />
      <el-table-column align="center" label="精度等级" prop="accuracy" min-width="110" />
      <el-table-column align="center" label="测量范围" prop="measureRange" min-width="110" />
      <el-table-column align="center" label="计量单位" prop="unit" min-width="110" />
      <el-table-column align="center" label="状态" prop="status" min-width="110" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['qms:instrument:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:instrument:delete']"
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
  <QmsInstrumentForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { QmsInstrumentApi } from '@/api/qms/instrument/list'
import QmsInstrumentForm from './QmsInstrumentForm.vue'

defineOptions({ name: 'QmsInstrument' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  code: undefined,
  name: undefined,
  category: undefined,
  status: undefined,
  manufacturer: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await QmsInstrumentApi.getQmsInstrumentPage(queryParams)
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
    await QmsInstrumentApi.deleteQmsInstrument(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
