<!-- FMS 语境会计科目维护（ERP erp_gl_account 单一真相，直连 ERP CRUD 端点） -->
<template>
  <doc-alert title="【设置】会计科目" url="https://doc.zhicloud.cn/fms/config/subject/" />
  <!-- 操作栏 -->
  <ContentWrap>
    <el-form class="-mb-15px" :inline="true">
      <el-form-item>
        <el-button
          type="primary"
          plain
          @click="openForm('create', null)"
          v-hasPermi="['erp:gl-account:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增顶级科目
        </el-button>
        <el-button class="ml-8px" @click="loadList">
          <Icon icon="ep:refresh" class="mr-5px" /> 刷新
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 树形列表 -->
  <ContentWrap>
    <el-table
      v-loading="loading"
      :data="treeList"
      row-key="id"
      :tree-props="{ children: 'children' }"
      :default-expand-all="false"
      border
      stripe
    >
      <el-table-column label="科目编码" prop="code" min-width="140" />
      <el-table-column label="科目名称" prop="name" min-width="200" show-overflow-tooltip />
      <el-table-column label="类型" align="center" width="120" prop="typeName" />
      <el-table-column label="余额方向" align="center" width="100" prop="balanceDirectionName" />
      <el-table-column label="末级" align="center" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.isLeaf" type="success" size="small">是</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="期初借方" align="right" width="120" prop="openingDebit" />
      <el-table-column label="期初贷方" align="right" width="120" prop="openingCredit" />
      <el-table-column label="状态" align="center" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 0" type="success" size="small">启用</el-tag>
          <el-tag v-else type="info" size="small">禁用</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" prop="remark" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" align="center" fixed="right" width="240">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openForm('create', scope.row)"
            v-hasPermi="['erp:gl-account:create']"
            v-if="!scope.row.isLeaf"
          >
            新增下级
          </el-button>
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row)"
            v-hasPermi="['erp:gl-account:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row)"
            v-hasPermi="['erp:gl-account:delete']"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>

  <!-- 添加或修改科目 -->
  <FmsSubjectForm ref="formRef" @success="loadList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { FmsSubjectApi } from '@/api/fms/config/subject'
import type { FmsSubjectVO } from '@/api/fms/config/subject'
import FmsSubjectForm from './FmsSubjectForm.vue'

defineOptions({ name: 'FmsSubject' })

/** 科目树节点（全量科目 + children） */
interface SubjectTreeNode extends FmsSubjectVO {
  children?: SubjectTreeNode[]
}

const message = useMessage()

const loading = ref(true)
const list = ref<FmsSubjectVO[]>([]) // 全量科目（扁平）
const treeList = ref<SubjectTreeNode[]>([]) // 树形展示

/** 查询全量科目（ERP /erp/gl-account/list 用于前端构建树） */
async function loadList() {
  loading.value = true
  try {
    list.value = (await FmsSubjectApi.getSubjectList()) || []
    treeList.value = buildTree(list.value)
  } finally {
    loading.value = false
  }
}

/** 扁平列表 → 树（parentId=0 为顶级；防御：孤儿节点挂到顶级，防数据脏了整树丢） */
function buildTree(flat: FmsSubjectVO[]): SubjectTreeNode[] {
  const nodes: SubjectTreeNode[] = flat.map((item) => ({ ...item, children: [] }))
  const byId = new Map<number, SubjectTreeNode>()
  nodes.forEach((n) => byId.set(n.id!, n))
  const roots: SubjectTreeNode[] = []
  for (const n of nodes) {
    const parent = n.parentId ? byId.get(n.parentId) : undefined
    if (parent && parent !== n) {
      parent.children!.push(n)
    } else {
      roots.push(n)
    }
  }
  return roots
}

/** 添加/修改操作 */
const formRef = ref<InstanceType<typeof FmsSubjectForm>>()
async function openForm(type: string, row?: FmsSubjectVO | null) {
  // 编辑时回显行数据（列表已含全字段，无需再拉详情）
  formRef.value?.open(type, row)
}

/** 删除科目 */
async function handleDelete(row: FmsSubjectVO) {
  try {
    await message.confirm(`是否确认删除科目「${row.code} ${row.name}」？`)
    await FmsSubjectApi.deleteSubject(row.id!)
    message.success('删除成功')
    await loadList()
  } catch {}
}

onMounted(() => {
  loadList()
})
</script>
