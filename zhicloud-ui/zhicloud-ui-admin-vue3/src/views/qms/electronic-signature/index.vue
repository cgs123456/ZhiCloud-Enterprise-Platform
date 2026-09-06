<!-- QMS 电子签名日志（只读） -->
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
      <el-form-item label="签名含义" prop="signatureMeaning">
        <el-input
          v-model="queryParams.signatureMeaning"
          class="!w-240px"
          clearable
          placeholder="请输入签名含义"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="操作类型" prop="operationType">
        <el-input
          v-model="queryParams.operationType"
          class="!w-240px"
          clearable
          placeholder="请输入操作类型"
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
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" :stripe="true">
      <el-table-column align="center" label="用户 ID" prop="userId" min-width="100" />
      <el-table-column align="center" label="签名含义" prop="signatureMeaning" min-width="110" />
      <el-table-column align="center" label="操作类型" prop="operationType" min-width="120" />
      <el-table-column align="center" label="操作内容" prop="operationContent" min-width="200" />
      <el-table-column
        align="center"
        :formatter="dateFormatter"
        label="签名时间"
        prop="signatureTime"
        width="170"
      />
      <el-table-column align="center" label="IP 地址" prop="ipAddress" min-width="130" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
    </el-table>
    <Pagination
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNo"
      :total="total"
      @pagination="getList"
    />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { ElectronicSignatureLogApi } from '@/api/qms/electronic-signature'

defineOptions({ name: 'QmsElectronicSignatureLog' })

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  signatureMeaning: undefined,
  operationType: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await ElectronicSignatureLogApi.getElectronicSignatureLogPage(queryParams)
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

onMounted(() => getList())
</script>
