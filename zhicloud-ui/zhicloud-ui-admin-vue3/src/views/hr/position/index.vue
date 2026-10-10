<!-- HR 岗位 -->
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
      <el-form-item label="职位编码" prop="code">
        <el-input v-model="queryParams.code" class="!w-240px" clearable placeholder="请输入职位编码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="职位名称" prop="name">
        <el-input v-model="queryParams.name" class="!w-240px" clearable placeholder="请输入职位名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="所属部门 ID" prop="deptId">
        <el-input-number v-model="queryParams.deptId" class="!w-240px" :controls="false" clearable placeholder="请输入所属部门 ID" />
      </el-form-item>
      <el-form-item label="职级" prop="level">
        <el-input-number v-model="queryParams.level" class="!w-240px" :controls="false" clearable placeholder="请输入职级" />
      </el-form-item>
      <el-form-item label="基本工资" prop="baseSalary">
        <el-input-number v-model="queryParams.baseSalary" class="!w-240px" :controls="false" clearable placeholder="请输入基本工资" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="queryParams.remark" class="!w-240px" clearable placeholder="请输入备注" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="queryParams.sort" class="!w-240px" :controls="false" clearable placeholder="请输入排序" />
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
          v-hasPermi="['hr:position:create']"
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
      <el-table-column align="center" label="职位编码" prop="code" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="职位名称" prop="name" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="所属部门 ID" prop="deptId" min-width="120" />
      <el-table-column align="center" label="职级" prop="level" min-width="120" />
      <el-table-column align="center" label="基本工资" prop="baseSalary" min-width="120" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="排序" prop="sort" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['hr:position:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['hr:position:delete']"
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
  <HrPositionForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { HrPositionApi } from '@/api/hr/position'
import HrPositionForm from './HrPositionForm.vue'

defineOptions({ name: 'HrPosition' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  code: undefined
  name: undefined
  deptId: undefined
  level: undefined
  baseSalary: undefined
  remark: undefined
  sort: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await HrPositionApi.getHrPositionPage(queryParams)
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
    await HrPositionApi.deleteHrPosition(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
