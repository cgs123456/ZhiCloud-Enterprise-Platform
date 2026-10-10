<!-- HR 部门管理（树形列表） -->
<template>
  <ContentWrap>
    <el-form :inline="true" class="mb-12px" label-width="80px">
      <el-form-item label="部门名称">
        <el-input v-model="filterName" class="!w-240px" clearable placeholder="请输入部门名称" @keyup.enter="getList" />
      </el-form-item>
      <el-form-item>
        <el-button @click="getList">
          <Icon class="mr-5px" icon="ep:search" />
          搜索
        </el-button>
        <el-button v-hasPermi="['hr:department:create']" plain type="primary" @click="openForm('create', undefined)">
          <Icon class="mr-5px" icon="ep:plus" />
          新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table
      v-loading="loading"
      :data="list"
      :show-overflow-tooltip="true"
      :stripe="true"
      row-key="id"
      default-expand-all
      :tree-props="{ children: 'children' }"
    >
      <el-table-column align="left" label="部门名称" prop="name" min-width="220" />
      <el-table-column align="center" label="部门编码" prop="code" min-width="140" />
      <el-table-column align="center" label="排序" prop="sort" min-width="90" />
      <el-table-column align="center" label="状态" prop="status" min-width="90" />
      <el-table-column align="center" label="备注" prop="remark" min-width="160" show-overflow-tooltip />
      <el-table-column align="center" fixed="right" label="操作" width="200">
        <template #default="scope">
          <el-button v-hasPermi="['hr:department:create']" link type="primary" @click="openForm('create', scope.row.id)">
            新增下级
          </el-button>
          <el-button v-hasPermi="['hr:department:update']" link type="primary" @click="openForm('update', scope.row.id)">
            修改
          </el-button>
          <el-button v-hasPermi="['hr:department:delete']" link type="danger" @click="handleDelete(scope.row.id)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>

  <HrDepartmentForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { HrDepartmentApi } from '@/api/hr/department'
import HrDepartmentForm from './HrDepartmentForm.vue'

defineOptions({ name: 'HrDepartment' })

const message = useMessage()
const loading = ref(true)
const list = ref<any[]>([])
const filterName = ref('')

const getList = async () => {
  loading.value = true
  try {
    const data = await HrDepartmentApi.getDepartmentList(
      filterName.value ? { name: filterName.value } : undefined
    )
    list.value = data || []
  } finally {
    loading.value = false
  }
}

const formRef = ref()
const openForm = (type: string, id?: number, parentId?: number) => {
  formRef.value.open(type, id, parentId)
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await HrDepartmentApi.deleteDepartment(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
