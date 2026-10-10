<!-- HR 假期类型 -->
<template>
  <ContentWrap>
    <el-form :inline="true" class="mb-12px" label-width="90px">
      <el-form-item label="名称">
        <el-input v-model="filterName" class="!w-240px" clearable placeholder="请输入假期类型名称" @keyup.enter="getList" />
      </el-form-item>
      <el-form-item>
        <el-button @click="getList">
          <Icon class="mr-5px" icon="ep:search" />
          搜索
        </el-button>
        <el-button v-hasPermi="['hr:leave-type:create']" plain type="primary" @click="openForm('create')">
          <Icon class="mr-5px" icon="ep:plus" />
          新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" :stripe="true">
      <el-table-column align="center" label="名称" prop="name" min-width="160" />
      <el-table-column align="center" label="编码" prop="code" min-width="140" />
      <el-table-column align="center" label="是否带薪" prop="isPaid" min-width="110" />
      <el-table-column align="center" label="是否扣薪" prop="deductSalary" min-width="110" />
      <el-table-column align="center" label="备注" prop="remark" min-width="180" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button v-hasPermi="['hr:leave-type:update']" link type="primary" @click="openForm('update', scope.row.id)">修改</el-button>
          <el-button v-hasPermi="['hr:leave-type:delete']" link type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>

  <HrLeaveTypeForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { HrLeaveTypeApi } from '@/api/hr/leave-type'
import HrLeaveTypeForm from './HrLeaveTypeForm.vue'

defineOptions({ name: 'HrLeaveType' })

const message = useMessage()
const loading = ref(true)
const list = ref<any[]>([])
const filterName = ref('')

const getList = async () => {
  loading.value = true
  try {
    list.value = (await HrLeaveTypeApi.getLeaveTypeList(filterName.value ? { name: filterName.value } : undefined)) || []
  } finally {
    loading.value = false
  }
}

const formRef = ref()
const openForm = (type: string, id?: number) => formRef.value.open(type, id)

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await HrLeaveTypeApi.deleteLeaveType(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
