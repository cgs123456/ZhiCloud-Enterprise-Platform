<!-- HRM 职位管理 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="100px"
    >
      <el-form-item label="职位编码" prop="code">
        <el-input v-model="formData.code" class="!w-1/1" placeholder="请输入职位编码" />
      </el-form-item>
      <el-form-item label="职位名称" prop="name">
        <el-input v-model="formData.name" class="!w-1/1" placeholder="请输入职位名称" />
      </el-form-item>
      <el-form-item label="所属部门" prop="deptId">
        <el-tree-select
          v-model="formData.deptId"
          :data="deptTree"
          :props="defaultProps"
          check-strictly
          class="!w-1/1"
          clearable
          placeholder="请选择所属部门"
        />
      </el-form-item>
      <el-form-item label="职级" prop="level">
        <el-input-number v-model="formData.level" :controls="false" class="!w-1/1" placeholder="请输入职级" />
      </el-form-item>
      <el-form-item label="基本工资" prop="baseSalary">
        <el-input-number
          v-model="formData.baseSalary"
          :controls="false"
          class="!w-1/1"
          placeholder="请输入基本工资"
        />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" :controls="false" class="!w-1/1" placeholder="请输入排序" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" :rows="3" class="!w-1/1" placeholder="请输入备注" type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { defaultProps, handleTree } from '@/utils/tree'
import * as PositionApi from '@/api/hrm/position'
import * as DeptApi from '@/api/system/dept'

defineOptions({ name: 'HrmPositionForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const deptTree = ref<any[]>([])

const formData = ref<PositionApi.HrmPositionVO>({})
const formRules = reactive({
  code: [{ required: true, message: '职位编码不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '职位名称不能为空', trigger: 'blur' }],
  level: [{ required: true, message: '职级不能为空', trigger: 'blur' }]
})

/** 打开弹窗 */
async function open(type: string, id?: number) {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  // 加载部门树
  if (deptTree.value.length === 0) {
    deptTree.value = handleTree(await DeptApi.getSimpleDeptList())
  }
  if (id) {
    formLoading.value = true
    try {
      formData.value = await PositionApi.getPosition(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])

/** 提交表单 */
async function submitForm() {
  if (!formRef.value) {
    return
  }
  await formRef.value.validate()
  formLoading.value = true
  try {
    const data = formData.value
    if (formType.value === 'create') {
      await PositionApi.createPosition(data)
      message.success(t('common.createSuccess'))
    } else {
      await PositionApi.updatePosition(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
function resetForm() {
  formData.value = {
    code: undefined,
    name: undefined,
    deptId: undefined,
    level: undefined,
    baseSalary: undefined,
    sort: undefined,
    remark: undefined
  }
  formRef.value?.resetFields()
}
</script>
