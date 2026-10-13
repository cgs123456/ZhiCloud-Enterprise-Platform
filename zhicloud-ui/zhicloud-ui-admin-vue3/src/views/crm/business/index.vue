<template>
  <doc-alert title="【商机】商机管理、商机状态" url="https://doc.zhicloud.cn/crm/business/" />
  <doc-alert title="【通用】数据权限" url="https://doc.zhicloud.cn/crm/permission/" />

  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      ref="queryFormRef"
      :inline="true"
      :model="queryParams"
      class="-mb-15px"
      label-width="68px"
    >
      <el-form-item label="商机名称" prop="name">
        <el-input
          v-model="queryParams.name"
          class="!w-240px"
          clearable
          placeholder="请输入商机名称"
          @keyup.enter="handleQuery"
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
        <el-button v-hasPermi="['crm:business:create']" type="primary" @click="openForm('create')">
          <Icon class="mr-5px" icon="ep:plus" />
          新增
        </el-button>
        <el-button
          v-hasPermi="['crm:business:export']"
          :loading="exportLoading"
          plain
          type="success"
          @click="handleExport"
        >
          <Icon class="mr-5px" icon="ep:download" />
          导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-tabs v-model="activeName" @tab-click="handleTabClick">
      <el-tab-pane label="我负责的" name="1" />
      <el-tab-pane label="我参与的" name="2" />
      <el-tab-pane label="下属负责的" name="3" />
    </el-tabs>
    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" :stripe="true">
      <el-table-column align="center" fixed="left" label="商机名称" prop="name" width="160">
        <template #default="scope">
          <el-link :underline="false" type="primary" @click="openDetail(scope.row.id)">
            {{ scope.row.name }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column align="center" fixed="left" label="客户名称" prop="customerName" width="120">
        <template #default="scope">
          <el-link
            :underline="false"
            type="primary"
            @click="openCustomerDetail(scope.row.customerId)"
          >
            {{ scope.row.customerName }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column
        :formatter="erpPriceTableColumnFormatter"
        align="center"
        label="商机金额（元）"
        prop="totalPrice"
        width="140"
      />
      <el-table-column
        :formatter="dateFormatter"
        align="center"
        label="预计成交日期"
        prop="dealTime"
        width="180px"
      />
      <el-table-column align="center" label="备注" prop="remark" width="200" />
      <el-table-column
        :formatter="dateFormatter"
        align="center"
        label="下次联系时间"
        prop="contactNextTime"
        width="180px"
      />
      <el-table-column align="center" label="负责人" prop="ownerUserName" width="100px" />
      <el-table-column align="center" label="所属部门" prop="ownerUserDeptName" width="100px" />
      <el-table-column
        :formatter="dateFormatter"
        align="center"
        label="最后跟进时间"
        prop="contactLastTime"
        width="180px"
      />
      <el-table-column
        :formatter="dateFormatter"
        align="center"
        label="更新时间"
        prop="updateTime"
        width="180px"
      />
      <el-table-column
        :formatter="dateFormatter"
        align="center"
        label="创建时间"
        prop="createTime"
        width="180px"
      />
      <el-table-column align="center" label="创建人" prop="creatorName" width="100px" />
      <el-table-column
        align="center"
        fixed="right"
        label="商机状态组"
        prop="statusTypeName"
        width="140"
      />
      <el-table-column
        align="center"
        fixed="right"
        label="商机阶段"
        prop="statusName"
        width="120"
      />
      <el-table-column align="center" fixed="right" label="操作" width="130px">
        <template #default="scope">
          <el-button
            v-hasPermi="['crm:business:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            编辑
          </el-button>
          <!-- 未结束商机才能推进阶段；已结束只读 -->
          <el-button
            v-if="scope.row.endStatus == null"
            v-hasPermi="['crm:business:update']"
            link
            type="success"
            @click="openStatusForm(scope.row)"
          >
            推进阶段
          </el-button>
          <el-button
            v-hasPermi="['crm:business:delete']"
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <Pagination
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNo"
      :total="total"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 表单弹窗：添加/修改 -->
  <BusinessForm ref="formRef" @success="getList" />

  <!-- 推进阶段弹窗：按状态组的可选阶段推进，或置为赢单/输单/无效 -->
  <Dialog v-model="statusFormVisible" title="推进商机阶段" width="480">
    <el-form
      ref="statusFormRef"
      v-loading="statusSubmitting"
      :model="statusForm"
      :rules="statusRules"
      label-width="110px"
    >
      <el-form-item label="商机">
        <el-input :model-value="currentRow?.name" disabled class="!w-1/1" />
      </el-form-item>
      <el-form-item label="当前阶段">
        <el-input :model-value="currentRow?.statusName ?? '未开始'" disabled class="!w-1/1" />
      </el-form-item>
      <el-form-item label="新阶段" prop="statusId">
        <el-select
          v-model="statusForm.statusId"
          placeholder="请选择新阶段"
          class="!w-1/1"
          @change="onStatusChange"
        >
          <el-option
            v-for="s in statusOptions"
            :key="s.id"
            :label="stageLabel(s)"
            :value="s.id!"
          />
        </el-select>
      </el-form-item>
      <!-- 赢单/输单是独立于阶段的结束标记（CrmBusinessUpdateStatusReqVO.endStatus） -->
      <el-form-item label="结束标记">
        <el-radio-group v-model="statusForm.endStatus">
          <el-radio :value="undefined">不结束</el-radio>
          <el-radio :value="1">赢单</el-radio>
          <el-radio :value="2">输单</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :disabled="statusSubmitting" @click="submitStatus">确认推进</el-button>
      <el-button @click="statusFormVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import * as BusinessApi from '@/api/crm/business'
import { getBusinessStatusSimpleList, BusinessStatusVO } from '@/api/crm/business/status'
import BusinessForm from './BusinessForm.vue'
import { erpPriceTableColumnFormatter } from '@/utils'
import { TabsPaneContext } from 'element-plus'

defineOptions({ name: 'CrmBusiness' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref([]) // 列表的数据
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  sceneType: '1', // 默认和 activeName 相等
  name: null
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中
const activeName = ref('1') // 列表 tab

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await BusinessApi.getBusinessPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

/** tab 切换 */
const handleTabClick = (tab: TabsPaneContext) => {
  if (tab.paneName === undefined) {
    return
  }
  queryParams.sceneType = String(tab.paneName)
  handleQuery()
}

/** 打开客户详情 */
const { push } = useRouter()
const openDetail = (id: number) => {
  push({ name: 'CrmBusinessDetail', params: { id } })
}

/** 打开客户详情 */
const openCustomerDetail = (id: number) => {
  push({ name: 'CrmCustomerDetail', params: { id } })
}

/** 添加/修改操作 */
const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

// ========== 推进阶段 ==========
const currentRow = ref<any>(null)
const statusFormVisible = ref(false)
const statusSubmitting = ref(false)
const statusFormRef = ref()
const statusOptions = ref<BusinessStatusVO[]>([])
const statusForm = ref<any>({})
const statusRules = reactive({
  statusId: [{ required: true, message: '新阶段不能为空', trigger: 'change' }]
})

/** 阶段标签：带进度百分比（阶段本身无 endStatus，结束态由独立字段标记） */
const stageLabel = (s: BusinessStatusVO) =>
  s.percent != null ? `${s.name}（${s.percent}%）` : `${s.name}（进行中）`

/** 选中 100% 阶段时默认带上赢单标记，其余清空 */
const onStatusChange = (statusId: number) => {
  const s = statusOptions.value.find((x) => x.id === statusId)
  statusForm.value.endStatus = s?.percent === 100 ? 1 : undefined
}

const openStatusForm = async (row: any) => {
  currentRow.value = row
  statusForm.value = { id: row.id, statusId: undefined, endStatus: undefined }
  statusFormVisible.value = true
  // 该商机所属状态组的可选阶段；无状态组时无可选项
  if (row.statusTypeId) {
    try {
      statusOptions.value = (await getBusinessStatusSimpleList(row.statusTypeId)) || []
    } catch {
      statusOptions.value = []
    }
  } else {
    statusOptions.value = []
  }
}

const submitStatus = async () => {
  await statusFormRef.value.validate()
  statusSubmitting.value = true
  try {
    await BusinessApi.updateBusinessStatus(statusForm.value)
    message.success('已推进商机阶段')
    statusFormVisible.value = false
    await getList()
  } finally {
    statusSubmitting.value = false
  }
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    // 发起删除
    await BusinessApi.deleteBusiness(id)
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    // 导出的二次确认
    await message.exportConfirm()
    // 发起导出
    exportLoading.value = true
    const data = await BusinessApi.exportBusiness(queryParams)
    download.excel(data, '商机.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>
