<!-- MES 计件规则 -->
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
      <el-form-item label="规则名称" prop="ruleName">
        <el-input v-model="queryParams.ruleName" class="!w-240px" clearable placeholder="请输入规则名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="工序编号" prop="processId">
        <el-input-number v-model="queryParams.processId" class="!w-240px" :controls="false" clearable placeholder="请输入工序编号" />
      </el-form-item>
      <el-form-item label="工序名称" prop="processName">
        <el-input v-model="queryParams.processName" class="!w-240px" clearable placeholder="请输入工序名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="工艺路线编号" prop="routeId">
        <el-input-number v-model="queryParams.routeId" class="!w-240px" :controls="false" clearable placeholder="请输入工艺路线编号" />
      </el-form-item>
      <el-form-item label="产品物料编号" prop="itemId">
        <el-input-number v-model="queryParams.itemId" class="!w-240px" :controls="false" clearable placeholder="请输入产品物料编号" />
      </el-form-item>
      <el-form-item label="产品物料名称" prop="itemName">
        <el-input v-model="queryParams.itemName" class="!w-240px" clearable placeholder="请输入产品物料名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="工作站编号" prop="workstationId">
        <el-input-number v-model="queryParams.workstationId" class="!w-240px" :controls="false" clearable placeholder="请输入工作站编号" />
      </el-form-item>
      <el-form-item label="工作站名称" prop="workstationName">
        <el-input v-model="queryParams.workstationName" class="!w-240px" clearable placeholder="请输入工作站名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="合格品单价（元/件）" prop="qualifiedUnitPrice">
        <el-input-number v-model="queryParams.qualifiedUnitPrice" class="!w-240px" :controls="false" clearable placeholder="请输入合格品单价（元/件）" />
      </el-form-item>
      <el-form-item label="废品单价（元/件）" prop="scrapUnitPrice">
        <el-input-number v-model="queryParams.scrapUnitPrice" class="!w-240px" :controls="false" clearable placeholder="请输入废品单价（元/件）" />
      </el-form-item>
      <el-form-item label="阶梯单价配置（JSON）" prop="stepConfig">
        <el-input v-model="queryParams.stepConfig" class="!w-240px" clearable placeholder="请输入阶梯单价配置（JSON）" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="是否启用" prop="enabled">
        <el-input-number v-model="queryParams.enabled" class="!w-240px" :controls="false" clearable placeholder="请输入是否启用" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="queryParams.remark" class="!w-240px" clearable placeholder="请输入备注" @keyup.enter="handleQuery" />
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
          v-hasPermi="['mes:pro-piecework-rule:create']"
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
      <el-table-column align="center" label="规则名称" prop="ruleName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="工序编号" prop="processId" min-width="120" />
      <el-table-column align="center" label="工序名称" prop="processName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="工艺路线编号" prop="routeId" min-width="120" />
      <el-table-column align="center" label="产品物料编号" prop="itemId" min-width="120" />
      <el-table-column align="center" label="产品物料名称" prop="itemName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="工作站编号" prop="workstationId" min-width="120" />
      <el-table-column align="center" label="工作站名称" prop="workstationName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="合格品单价（元/件）" prop="qualifiedUnitPrice" min-width="120" />
      <el-table-column align="center" label="废品单价（元/件）" prop="scrapUnitPrice" min-width="120" />
      <el-table-column align="center" label="阶梯单价配置（JSON）" prop="stepConfig" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="生效日期" prop="effectiveDate" width="170" />
      <el-table-column align="center" :formatter="dateFormatter" label="失效日期" prop="expireDate" width="170" />
      <el-table-column align="center" label="状态" prop="status" min-width="120" />
      <el-table-column align="center" label="是否启用" prop="enabled" min-width="120" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['mes:pro-piecework-rule:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['mes:pro-piecework-rule:delete']"
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
  <MesProPieceworkRuleForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { MesProPieceworkRuleApi } from '@/api/mes/pro-piecework-rule'
import MesProPieceworkRuleForm from './MesProPieceworkRuleForm.vue'

defineOptions({ name: 'MesProPieceworkRule' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  ruleName: undefined
  processId: undefined
  processName: undefined
  routeId: undefined
  itemId: undefined
  itemName: undefined
  workstationId: undefined
  workstationName: undefined
  qualifiedUnitPrice: undefined
  scrapUnitPrice: undefined
  stepConfig: undefined
  effectiveDate: undefined
  expireDate: undefined
  status: undefined
  enabled: undefined
  remark: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await MesProPieceworkRuleApi.getMesProPieceworkRulePage(queryParams)
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
    await MesProPieceworkRuleApi.deleteMesProPieceworkRule(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
