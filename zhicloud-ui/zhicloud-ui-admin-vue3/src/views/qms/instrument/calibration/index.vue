<!-- QMS 器具校准记录 -->
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
      <el-form-item label="器具 ID" prop="instrumentId">
        <el-input-number v-model="queryParams.instrumentId" class="!w-240px" :controls="false" clearable placeholder="请输入器具 ID" />
      </el-form-item>
      <el-form-item label="校准证书编号" prop="calibrationNo">
        <el-input v-model="queryParams.calibrationNo" class="!w-240px" clearable placeholder="请输入校准证书编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="校准结果" prop="calibrationResult">
        <el-input-number v-model="queryParams.calibrationResult" class="!w-240px" :controls="false" clearable placeholder="请输入校准结果" />
      </el-form-item>
      <el-form-item label="校准机构" prop="calibrationOrganization">
        <el-input v-model="queryParams.calibrationOrganization" class="!w-240px" clearable placeholder="请输入校准机构" @keyup.enter="handleQuery" />
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
          v-hasPermi="['qms:instrument-calibration:create']"
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
      <el-table-column align="center" label="器具 ID" prop="instrumentId" min-width="110" />
      <el-table-column align="center" label="校准证书编号" prop="calibrationNo" min-width="110" />
      <el-table-column align="center" label="校准日期" prop="calibrationDate" min-width="110" />
      <el-table-column align="center" label="校准机构" prop="calibrationOrganization" min-width="110" />
      <el-table-column align="center" label="校准结果" prop="calibrationResult" min-width="110" />
      <el-table-column align="center" label="校准证书附件 URL" prop="calibrationCertificateUrl" min-width="110" />
      <el-table-column align="center" label="偏差值" prop="deviation" min-width="110" />
      <el-table-column align="center" label="下次校准日期" prop="nextCalibrationDate" min-width="110" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" />
      <el-table-column align="center" label="排序" prop="sort" min-width="110" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['qms:instrument-calibration:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:instrument-calibration:delete']"
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
  <QmsInstrumentCalibrationForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { QmsInstrumentCalibrationApi } from '@/api/qms/instrument/calibration'
import QmsInstrumentCalibrationForm from './QmsInstrumentCalibrationForm.vue'

defineOptions({ name: 'QmsInstrumentCalibration' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  instrumentId: undefined,
  calibrationNo: undefined,
  calibrationResult: undefined,
  calibrationOrganization: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await QmsInstrumentCalibrationApi.getQmsInstrumentCalibrationPage(queryParams)
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
    await QmsInstrumentCalibrationApi.deleteQmsInstrumentCalibration(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
