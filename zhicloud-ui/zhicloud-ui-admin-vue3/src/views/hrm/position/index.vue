<!-- HRM 职位管理（HR/HRM 融合：复用 hr_position 表） -->
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
        <el-input
          v-model="queryParams.code"
          class="!w-240px"
          clearable
          placeholder="请输入职位编码"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="职位名称" prop="name">
        <el-input
          v-model="queryParams.name"
          class="!w-240px"
          clearable
          placeholder="请输入职位名称"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="所属部门" prop="deptId">
        <el-tree-select
          v-model="queryParams.deptId"
          :data="deptTree"
          :props="defaultProps"
          check-strictly
          class="!w-240px"
          clearable
          placeholder="请选择所属部门"
        />
      </el-form-item>
      <el-form-item label="职级" prop="level">
        <el-input-number
          v-model="queryParams.level"
          :controls="false"
          class="!w-240px"
          clearable
          placeholder="请输入职级"
        />
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
          v-hasPermi="['hrm:position:create']"
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
      <el-table-column align="center" label="职位编码" min-width="120" prop="code" show-overflow-tooltip />
      <el-table-column align="center" label="职位名称" min-width="120" prop="name" show-overflow-tooltip />
      <el-table-column align="center" label="所属部门" min-width="140" prop="deptName" show-overflow-tooltip />
      <el-table-column align="center" label="职级" min-width="90" prop="level" />
      <el-table-column align="center" label="基本工资" min-width="120" prop="baseSalary" />
      <el-table-column align="center" label="备注" min-width="120" prop="remark" show-overflow-tooltip />
      <el-table-column align="center" label="排序" min-width="80" prop="sort" />
      <el-table-column
        :formatter="dateFormatter"
        align="center"
        label="创建时间"
        prop="createTime"
        width="170"
      />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['hrm:position:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['hrm:position:delete']"
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
  <HrmPositionForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { defaultProps, handleTree } from '@/utils/tree'
import * as PositionApi from '@/api/hrm/position'
import * as DeptApi from '@/api/system/dept'
import HrmPositionForm from './HrmPositionForm.vue'

defineOptions({ name: 'HrmPosition' })

const message = useMessage()

const loading = ref(true)
const list = ref<PositionApi.HrmPositionVO[]>([])
const total = ref(0)
const deptTree = ref<any[]>([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  code: undefined as string | undefined,
  name: undefined as string | undefined,
  deptId: undefined as number | undefined,
  level: undefined as number | undefined
})
const queryFormRef = ref()

/** 查询列表 */
async function getList() {
  loading.value = true
  try {
    const data = await PositionApi.getPositionPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  queryFormRef.value?.resetFields()
  handleQuery()
}

const formRef = ref()

/** 打开表单 */
function openForm(type: string, id?: number) {
  formRef.value.open(type, id)
}

/** 删除按钮操作 */
async function handleDelete(id: number) {
  try {
    await message.delConfirm()
    await PositionApi.deletePosition(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

/** 初始化 */
onMounted(async () => {
  deptTree.value = handleTree(await DeptApi.getSimpleDeptList())
  await getList()
})
</script>
