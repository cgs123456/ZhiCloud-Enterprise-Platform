<template>
  <doc-alert title="AI 手册" url="https://doc.zhicloud.cn/ai/build/" />

  <!-- 技能目录（分组折叠面板 + 技能表格） -->
  <ContentWrap>
    <div class="mb-10px flex items-center justify-between">
      <div class="text-14px font-600">技能目录（Level-1 分组 → Level-2 技能）</div>
      <div>
        <el-button class="mr-10px" @click="loadCatalog" :loading="loading">
          <Icon icon="ep:refresh" class="mr-5px" /> 刷新
        </el-button>
        <el-button type="primary" @click="openCreate" v-hasPermi="['aimultiagent:skill:manage']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增技能
        </el-button>
      </div>
    </div>
    <el-collapse v-model="activeGroups" v-loading="loading">
      <el-collapse-item
        v-for="group in catalog"
        :key="group.id"
        :name="group.id"
        :title="`${group.name}（${group.code}）· ${group.skills?.length ?? 0} 个技能`"
      >
        <el-table :data="group.skills ?? []" stripe border>
          <el-table-column prop="code" label="技能编码" min-width="200" />
          <el-table-column prop="name" label="名称" width="140" />
          <el-table-column prop="toolName" label="绑定工具" min-width="200">
            <template #default="{ row }">
              <el-tag v-if="row.toolName" size="small" type="info">{{ row.toolName }}</el-tag>
              <span v-else class="text-12px text-gray-400">纯提示词技能</span>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-switch
                v-model="row.status"
                :active-value="0"
                :inactive-value="1"
                :disabled="!hasManagePerm"
                @change="(val: number) => handleStatus(row, val)"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                @click="openEdit(row)"
                v-hasPermi="['aimultiagent:skill:manage']"
              >
                编辑
              </el-button>
              <el-button
                link
                type="danger"
                @click="handleDelete(row)"
                v-hasPermi="['aimultiagent:skill:manage']"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-collapse-item>
    </el-collapse>
    <el-empty v-if="!loading && catalog.length === 0" description="暂无技能分组" />
  </ContentWrap>

  <!-- 新增 / 编辑弹窗 -->
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="560px"
    :close-on-click-modal="false"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
      <el-form-item label="所属分组" prop="groupId">
        <el-select v-model="form.groupId" placeholder="请选择分组" class="w-full">
          <el-option
            v-for="g in catalog"
            :key="g.id"
            :label="`${g.name}（${g.code}）`"
            :value="g.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="技能编码" prop="code">
        <el-input v-model="form.code" placeholder="如 wms:receipt_order_list" />
      </el-form-item>
      <el-form-item label="技能名称" prop="name">
        <el-input v-model="form.name" placeholder="如 收货单列表" />
      </el-form-item>
      <el-form-item label="绑定工具" prop="toolName">
        <el-input v-model="form.toolName" placeholder="WorkerToolExecutor 工具名，留空为纯提示词技能" />
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="配置 JSON" prop="configJson">
        <el-input
          v-model="form.configJson"
          type="textarea"
          :rows="3"
          placeholder='可选，如 {"endpoint_url":"https://api.example.com/data"}（保存时做 SSRF 校验）'
        />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="form.sort" :min="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { MultiAgentSkillApi, type MultiAgentSkillGroupVO } from '@/api/aimultiagent/skill'
import { checkPermi } from '@/utils/permission'

defineOptions({ name: 'AiMultiAgentSkill' })

const message = useMessage()
const hasManagePerm = checkPermi(['aimultiagent:skill:manage'])

const catalog = ref<MultiAgentSkillGroupVO[]>([])
const activeGroups = ref<number[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const saving = ref(false)
const formRef = ref()
const editingId = ref<number | null>(null)
const form = reactive({
  groupId: undefined as number | undefined,
  code: '',
  name: '',
  toolName: '',
  description: '',
  configJson: '',
  sort: 0
})
const rules = {
  groupId: [{ required: true, message: '请选择所属分组', trigger: 'change' }],
  code: [{ required: true, message: '请输入技能编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入技能名称', trigger: 'blur' }]
}

/** 加载技能目录 */
const loadCatalog = async () => {
  loading.value = true
  try {
    const data: any = await MultiAgentSkillApi.getCatalog()
    catalog.value = data ?? []
    if (activeGroups.value.length === 0 && catalog.value.length > 0) {
      activeGroups.value = catalog.value.map((g) => g.id)
    }
  } finally {
    loading.value = false
  }
}

/** 打开新增弹窗 */
const openCreate = () => {
  editingId.value = null
  dialogTitle.value = '新增技能'
  Object.assign(form, {
    groupId: catalog.value[0]?.id,
    code: '',
    name: '',
    toolName: '',
    description: '',
    configJson: '',
    sort: 0
  })
  dialogVisible.value = true
}

/** 打开编辑弹窗 */
const openEdit = (row: any) => {
  editingId.value = row.id
  dialogTitle.value = '编辑技能'
  Object.assign(form, {
    groupId: row.groupId,
    code: row.code,
    name: row.name,
    toolName: row.toolName ?? '',
    description: row.description ?? '',
    configJson: row.configJson ?? '',
    sort: row.sort ?? 0
  })
  dialogVisible.value = true
}

/** 保存（SSRF 不合法时后端拒绝并提示） */
const handleSave = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = { ...form, status: 0 }
    if (editingId.value == null) {
      await MultiAgentSkillApi.createSkill(payload)
      message.success('新增成功')
    } else {
      await MultiAgentSkillApi.updateSkill(editingId.value, payload)
      message.success('更新成功')
    }
    dialogVisible.value = false
    await loadCatalog()
  } finally {
    saving.value = false
  }
}

/** 启用 / 禁用 */
const handleStatus = async (row: any, status: number) => {
  try {
    await MultiAgentSkillApi.updateSkillStatus(row.id, status)
    message.success(status === 0 ? '已启用' : '已禁用')
  } catch {
    row.status = status === 0 ? 1 : 0 // 失败回滚开关
  }
}

/** 删除 */
const handleDelete = async (row: any) => {
  await message.confirm(`确认删除技能「${row.name}（${row.code}）」吗？`)
  await MultiAgentSkillApi.deleteSkill(row.id)
  message.success('删除成功')
  await loadCatalog()
}

onMounted(() => {
  loadCatalog()
})
</script>
