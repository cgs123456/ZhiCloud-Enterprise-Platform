<template>
  <ContentWrap>
    <!-- 返工单明细：按返工单加载不良项明细，支持增改删 -->
    <el-form class="-mb-15px" :model="form" :inline="true" label-width="90px">
      <el-form-item label="返工单" prop="reworkOrderId">
        <el-input-number v-model="form.reworkOrderId" :controls="false" placeholder="返工单 ID" class="!w-200px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load" :loading="loading"><Icon icon="ep:search" class="mr-5px" /> 加载明细</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['mes:pro:rework-order-detail:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="缺陷描述" align="center" prop="defectDescription" min-width="160" show-overflow-tooltip />
      <el-table-column label="缺陷数量" align="center" prop="defectQuantity" />
      <el-table-column label="缺陷类型" align="center" prop="defectType" />
      <el-table-column label="修复方法" align="center" prop="repairMethod" />
      <el-table-column label="修复描述" align="center" prop="repairDescription" min-width="160" show-overflow-tooltip />
      <el-table-column label="已修复数量" align="center" prop="repairedQuantity" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['mes:pro:rework-order-detail:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['mes:pro:rework-order-detail:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>

  <!-- 维护弹窗 -->
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="520">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px" v-loading="formLoading">
      <el-form-item label="返工单" prop="reworkOrderId">
        <el-input-number v-model="formData.reworkOrderId" :controls="false" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="缺陷描述" prop="defectDescription">
        <el-input v-model="formData.defectDescription" type="textarea" placeholder="缺陷描述" />
      </el-form-item>
      <el-form-item label="缺陷数量" prop="defectQuantity">
        <el-input-number v-model="formData.defectQuantity" :controls="false" :precision="2" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="修复描述" prop="repairDescription">
        <el-input v-model="formData.repairDescription" type="textarea" placeholder="修复描述" />
      </el-form-item>
      <el-form-item label="已修复数量" prop="repairedQuantity">
        <el-input-number v-model="formData.repairedQuantity" :controls="false" :precision="2" class="!w-1/1" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :disabled="formLoading" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { ReworkOrderDetailApi } from '@/api/mes/pro/rework-order-detail'

defineOptions({ name: 'MesReworkOrderDetail' })

const message = useMessage()

const loading = ref(false)
const list = ref<any[]>([])
const form = reactive({
  reworkOrderId: undefined as any
})

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formData = ref<any>({})
const formRules = reactive({
  reworkOrderId: [{ required: true, message: '返工单不能为空', trigger: 'blur' }]
})
const formRef = ref()

const load = async () => {
  if (!form.reworkOrderId) {
    message.warning('请输入返工单 ID')
    return
  }
  loading.value = true
  try {
    const res = await ReworkOrderDetailApi.ReworkOrderDetailListByReworkOrder(form.reworkOrderId)
    list.value = Array.isArray(res) ? res : []
  } finally {
    loading.value = false
  }
}

const openForm = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = type === 'create' ? '新增不良项' : '修改不良项'
  formType.value = type
  formData.value = { reworkOrderId: form.reworkOrderId }
  if (id) {
    formLoading.value = true
    try {
      formData.value = await ReworkOrderDetailApi.getReworkOrderDetail(id)
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
      await ReworkOrderDetailApi.createReworkOrderDetail(data)
      message.success('新增成功')
    } else {
      await ReworkOrderDetailApi.updateReworkOrderDetail(data)
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
    await ReworkOrderDetailApi.deleteReworkOrderDetail(id)
    message.success('删除成功')
    await load()
  } catch {}
}
</script>
