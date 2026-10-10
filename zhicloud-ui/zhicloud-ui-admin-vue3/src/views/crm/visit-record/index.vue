<!-- CRM 拜访签到 -->
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
      <el-form-item label="客户编号" prop="customerId">
        <el-input-number v-model="queryParams.customerId" class="!w-240px" :controls="false" clearable placeholder="请输入客户编号" />
      </el-form-item>
      <el-form-item label="客户名称" prop="customerName">
        <el-input v-model="queryParams.customerName" class="!w-240px" clearable placeholder="请输入客户名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="联系人编号" prop="contactId">
        <el-input-number v-model="queryParams.contactId" class="!w-240px" :controls="false" clearable placeholder="请输入联系人编号" />
      </el-form-item>
      <el-form-item label="联系人名称" prop="contactName">
        <el-input v-model="queryParams.contactName" class="!w-240px" clearable placeholder="请输入联系人名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="负责人的用户编号" prop="ownerUserId">
        <el-input-number v-model="queryParams.ownerUserId" class="!w-240px" :controls="false" clearable placeholder="请输入负责人的用户编号" />
      </el-form-item>
      <el-form-item label="负责人名字" prop="ownerUserName">
        <el-input v-model="queryParams.ownerUserName" class="!w-240px" clearable placeholder="请输入负责人名字" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="负责人部门" prop="ownerUserDeptName">
        <el-input v-model="queryParams.ownerUserDeptName" class="!w-240px" clearable placeholder="请输入负责人部门" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="签到纬度" prop="signInLatitude">
        <el-input-number v-model="queryParams.signInLatitude" class="!w-240px" :controls="false" clearable placeholder="请输入签到纬度" />
      </el-form-item>
      <el-form-item label="签到经度" prop="signInLongitude">
        <el-input-number v-model="queryParams.signInLongitude" class="!w-240px" :controls="false" clearable placeholder="请输入签到经度" />
      </el-form-item>
      <el-form-item label="签到地址" prop="signInAddress">
        <el-input v-model="queryParams.signInAddress" class="!w-240px" clearable placeholder="请输入签到地址" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="拜访类型" prop="visitType">
        <el-input-number v-model="queryParams.visitType" class="!w-240px" :controls="false" clearable placeholder="请输入拜访类型" />
      </el-form-item>
      <el-form-item label="拜访内容" prop="content">
        <el-input v-model="queryParams.content" class="!w-240px" clearable placeholder="请输入拜访内容" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="创建人名字" prop="creatorName">
        <el-input v-model="queryParams.creatorName" class="!w-240px" clearable placeholder="请输入创建人名字" @keyup.enter="handleQuery" />
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
          v-hasPermi="['crm:visit-record:create']"
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
      <el-table-column align="center" label="客户编号" prop="customerId" min-width="120" />
      <el-table-column align="center" label="客户名称" prop="customerName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="联系人编号" prop="contactId" min-width="120" />
      <el-table-column align="center" label="联系人名称" prop="contactName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="负责人的用户编号" prop="ownerUserId" min-width="120" />
      <el-table-column align="center" label="负责人名字" prop="ownerUserName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="负责人部门" prop="ownerUserDeptName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="拜访时间" prop="visitTime" width="170" />
      <el-table-column align="center" :formatter="dateFormatter" label="签到时间" prop="signInTime" width="170" />
      <el-table-column align="center" label="签到纬度" prop="signInLatitude" min-width="120" />
      <el-table-column align="center" label="签到经度" prop="signInLongitude" min-width="120" />
      <el-table-column align="center" label="签到地址" prop="signInAddress" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="拜访类型" prop="visitType" min-width="120" />
      <el-table-column align="center" label="拜访内容" prop="content" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="创建人名字" prop="creatorName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['crm:visit-record:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['crm:visit-record:delete']"
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
  <CrmVisitRecordForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { CrmVisitRecordApi } from '@/api/crm/visit-record'
import CrmVisitRecordForm from './CrmVisitRecordForm.vue'

defineOptions({ name: 'CrmVisitRecord' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  customerId: undefined
  customerName: undefined
  contactId: undefined
  contactName: undefined
  ownerUserId: undefined
  ownerUserName: undefined
  ownerUserDeptName: undefined
  visitTime: undefined
  signInTime: undefined
  signInLatitude: undefined
  signInLongitude: undefined
  signInAddress: undefined
  visitType: undefined
  content: undefined
  creatorName: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await CrmVisitRecordApi.getCrmVisitRecordPage(queryParams)
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
    await CrmVisitRecordApi.deleteCrmVisitRecord(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
