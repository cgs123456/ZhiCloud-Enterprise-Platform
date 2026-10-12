<template>
  <ContentWrap>
    <!-- 点检保养计划项目：按计划加载项目明细，支持增改删 -->
    <el-form class="-mb-15px" :model="form" :inline="true" label-width="90px">
      <el-form-item label="计划" prop="planId">
        <el-input-number v-model="form.planId" :controls="false" placeholder="计划 ID" class="!w-200px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load" :loading="loading"><Icon icon="ep:search" class="mr-5px" /> 加载项目</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['mes:dv:tp-plan-item:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="项目名" align="center" prop="itemName" min-width="140" show-overflow-tooltip />
      <el-table-column label="项目内容" align="center" prop="itemContent" min-width="160" show-overflow-tooltip />
      <el-table-column label="标准" align="center" prop="standard" min-width="140" show-overflow-tooltip />
      <el-table-column label="方法" align="center" prop="method" />
      <el-table-column label="排序" align="center" prop="sort" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['mes:dv:tp-plan-item:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['mes:dv:tp-plan-item:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>

  <!-- 维护弹窗 -->
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="500">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px" v-loading="formLoading">
      <el-form-item label="计划" prop="planId">
        <el-input-number v-model="formData.planId" :controls="false" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="项目名" prop="itemName">
        <el-input v-model="formData.itemName" placeholder="项目名" />
      </el-form-item>
      <el-form-item label="项目内容" prop="itemContent">
        <el-input v-model="formData.itemContent" type="textarea" placeholder="项目内容" />
      </el-form-item>
      <el-form-item label="标准" prop="standard">
        <el-input v-model="formData.standard" placeholder="标准" />
      </el-form-item>
      <el-form-item label="方法" prop="method">
        <el-input-number v-model="formData.method" :controls="false" placeholder="方法" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" :controls="false" placeholder="排序" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :disabled="formLoading" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { TpPlanItemApi } from '@/api/mes/dv/tp-plan-item'

defineOptions({ name: 'MesTpPlanItem' })

const message = useMessage()

const loading = ref(false)
const list = ref<any[]>([])
const form = reactive({
  planId: undefined as any
})

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formData = ref<any>({})
const formRules = reactive({
  planId: [{ required: true, message: '计划不能为空', trigger: 'blur' }],
  itemName: [{ required: true, message: '项目名不能为空', trigger: 'blur' }]
})
const formRef = ref()

const load = async () => {
  if (!form.planId) {
    message.warning('请输入计划 ID')
    return
  }
  loading.value = true
  try {
    const res = await TpPlanItemApi.getTpPlanItemList(form.planId)
    list.value = Array.isArray(res) ? res : []
  } finally {
    loading.value = false
  }
}

const openForm = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = type === 'create' ? '新增项目' : '修改项目'
  formType.value = type
  formData.value = { planId: form.planId }
  if (id) {
    formLoading.value = true
    try {
      formData.value = await TpPlanItemApi.getTpPlanItem(id)
    } finally {
      formLoading.value = false
    }
  }
}

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const data = formData.value
    if (formType.value === 'create') {
      await TpPlanItemApi.createTpPlanItem(data)
      message.success('新增成功')
    } else {
      await TpPlanItemApi.updateTpPlanItem(data)
      message.success('修改成功')
    }
    dialogVisible.value = false
    await load()
  } finally {
    formLoading.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await TpPlanItemApi.deleteTpPlanItem(id)
    message.success('删除成功')
    await load()
  } catch {}
}
</script>
