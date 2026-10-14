<!-- FMS 语境会计科目表单（ERP erp_gl_account 单一真相） -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="100px"
    >
      <el-form-item label="父级科目">
        <el-input :value="parentText" disabled placeholder="顶级科目（无父级）" />
      </el-form-item>
      <el-form-item label="科目编码" prop="code">
        <el-input v-model="formData.code" maxlength="32" placeholder="请输入科目编码，如 1001" />
      </el-form-item>
      <el-form-item label="科目名称" prop="name">
        <el-input v-model="formData.name" maxlength="128" placeholder="请输入科目名称" />
      </el-form-item>
      <el-form-item label="科目类型" prop="type">
        <el-select v-model="formData.type" class="w-100%" placeholder="请选择科目类型">
          <el-option v-for="item in SUBJECT_TYPES" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="余额方向" prop="balanceDirection">
        <el-select v-model="formData.balanceDirection" class="w-100%" placeholder="请选择余额方向">
          <el-option
            v-for="item in BALANCE_DIRECTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="是否末级" prop="isLeaf">
        <el-radio-group v-model="formData.isLeaf">
          <el-radio :value="true">是</el-radio>
          <el-radio :value="false">否</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :value="0">启用</el-radio>
          <el-radio :value="1">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" :controls="false" class="!w-160px" placeholder="请输入排序" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" maxlength="512" placeholder="请输入备注" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import type { FormInstance, FormRules } from 'element-plus'
import { FmsSubjectApi } from '@/api/fms/config/subject'
import type { FmsSubjectVO } from '@/api/fms/config/subject'

defineOptions({ name: 'FmsSubjectForm' })

const { t } = useI18n()
const message = useMessage()

/** 科目类型（与后端 ErpGlAccountTypeEnum 对齐：10 资产 / 20 负债 / 30 所有者权益 / 40 收入 / 50 费用 / 60 共同） */
const SUBJECT_TYPES = [
  { value: 10, label: '资产' },
  { value: 20, label: '负债' },
  { value: 30, label: '所有者权益' },
  { value: 40, label: '收入' },
  { value: 50, label: '费用' },
  { value: 60, label: '共同' }
]

/** 余额方向（与后端 ErpGlAccountBalanceDirectionEnum 对齐：10 借方 / 20 贷方 / 30 双向） */
const BALANCE_DIRECTIONS = [
  { value: 10, label: '借方' },
  { value: 20, label: '贷方' },
  { value: 30, label: '双向' }
]

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const parentId = ref<number>(0)
const parentText = ref('')
const formData = ref<Omit<FmsSubjectVO, 'id'> & { id?: number }>({
  id: undefined,
  parentId: 0,
  code: '',
  name: '',
  type: undefined,
  balanceDirection: undefined,
  isLeaf: true,
  status: 0,
  sort: undefined,
  remark: ''
})
const formRules = reactive<FormRules>({
  code: [{ required: true, message: '科目编码不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '科目名称不能为空', trigger: 'blur' }],
  type: [{ required: true, message: '科目类型不能为空', trigger: 'change' }],
  balanceDirection: [{ required: true, message: '余额方向不能为空', trigger: 'change' }]
})
const formRef = ref<FormInstance>()

/** 打开弹窗（type=create 时 parentRow 为父级科目行，null 表示顶级；type=update 时 row 为待编辑行） */
function open(type: string, row?: FmsSubjectVO | null) {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  if (type === 'update' && row) {
    parentId.value = row.parentId ?? 0
    parentText.value = ''
    formData.value = { ...row }
  } else {
    parentId.value = row?.id ?? 0
    parentText.value = row ? `${row.code} ${row.name}` : ''
    formData.value = {
      id: undefined,
      parentId: parentId.value,
      code: '',
      name: '',
      type: undefined,
      balanceDirection: undefined,
      isLeaf: true,
      status: 0,
      sort: undefined,
      remark: ''
    }
  }
  nextTick(() => formRef.value?.clearValidate())
}
defineExpose({ open })

const emit = defineEmits<{ success: [] }>()

/** 提交表单 */
async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    const data = formData.value as FmsSubjectVO
    if (formData.value.id) {
      await FmsSubjectApi.updateSubject(data)
      message.success(t('common.updateSuccess'))
    } else {
      await FmsSubjectApi.createSubject(data)
      message.success(t('common.createSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
