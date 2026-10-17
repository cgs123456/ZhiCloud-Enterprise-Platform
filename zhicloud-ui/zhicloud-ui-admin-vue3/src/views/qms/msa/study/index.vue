<!-- QMS MSA研究 -->
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
      <el-form-item label="研究编号" prop="studyNo">
        <el-input v-model="queryParams.studyNo" class="!w-240px" clearable placeholder="请输入研究编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="研究类型" prop="studyType">
        <el-input-number v-model="queryParams.studyType" class="!w-240px" :controls="false" clearable placeholder="请输入研究类型" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
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
          v-hasPermi="['qms:msa:create']"
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
      <el-table-column align="center" label="研究编号" prop="studyNo" min-width="110" />
      <el-table-column align="center" label="研究类型" prop="studyType" min-width="110" />
      <el-table-column align="center" label="特性名称" prop="characteristicName" min-width="110" />
      <el-table-column align="center" label="测量设备 ID" prop="equipmentId" min-width="110" />
      <el-table-column align="center" label="评价人数量" prop="appraiserCount" min-width="120" />
      <el-table-column align="center" label="试验次数" prop="trialCount" min-width="110" />
      <el-table-column align="center" label="零件数量" prop="partCount" min-width="110" />
      <el-table-column align="center" label="状态" min-width="110" prop="status">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.QMS_MSA_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="备注" prop="remark" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="320">
        <template #default="scope">
          <!-- 测量数据（录入） -->
          <el-button
            v-hasPermi="['qms:msa:update']"
            link
            type="primary"
            @click="openMeasure(scope.row)"
          >
            测量数据
          </el-button>
          <!-- 计算 Gage R&R（研究状态将置为已完成） -->
          <el-button
            v-hasPermi="['qms:msa:query']"
            link
            type="warning"
            @click="openGrr(scope.row)"
          >
            计算 GRR
          </el-button>
          <el-button
            v-hasPermi="['qms:msa:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:msa:delete']"
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
  <MsaStudyForm ref="formRef" @success="getList" />

  <!-- 测量数据（列表 + 新增） -->
  <Dialog v-model="measureVisible" title="测量数据" width="760">
    <el-form label-width="110px">
      <el-form-item label="研究编号">
        <span>{{ currentRow?.studyNo }}（共 {{ measureList.length }} 条）</span>
      </el-form-item>
    </el-form>
    <el-table v-loading="measureLoading" :data="measureList" max-height="260" :stripe="true">
      <el-table-column align="center" label="零件 ID" prop="partId" min-width="90" />
      <el-table-column align="center" label="评价人 ID" prop="appraiserId" min-width="90" />
      <el-table-column align="center" label="试验序号" prop="trialNo" min-width="90" />
      <el-table-column align="center" label="测量值" prop="measurementValue" min-width="90" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
    </el-table>
    <el-divider content-position="left">新增测量值</el-divider>
    <el-form
      ref="measureFormRef"
      :model="measureForm"
      :rules="measureRules"
      label-width="90px"
      :inline="true"
    >
      <el-form-item label="测量值" prop="measurementValue">
        <el-input-number v-model="measureForm.measurementValue" class="!w-130px" :controls="false" placeholder="必填" />
      </el-form-item>
      <el-form-item label="零件 ID" prop="partId">
        <el-input-number v-model="measureForm.partId" class="!w-110px" :controls="false" placeholder="可选" />
      </el-form-item>
      <el-form-item label="评价人 ID" prop="appraiserId">
        <el-input-number v-model="measureForm.appraiserId" class="!w-110px" :controls="false" placeholder="可选" />
      </el-form-item>
      <el-form-item label="试验序号" prop="trialNo">
        <el-input-number v-model="measureForm.trialNo" class="!w-110px" :controls="false" placeholder="可选" />
      </el-form-item>
      <el-form-item>
        <el-button :disabled="measureSaving" type="primary" @click="submitMeasure">保存测量值</el-button>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="measureVisible = false">关 闭</el-button>
    </template>
  </Dialog>

  <!-- Gage R&R 计算结果 -->
  <Dialog v-model="grrVisible" title="Gage R&R 计算" width="560">
    <el-descriptions v-loading="grrLoading" :column="2" border>
      <el-descriptions-item label="样本数">{{ grrResult.sampleCount ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="结论">{{ grrResult.conclusion ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="重复性 EV">{{ grrResult.ev ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="再现性 AV">{{ grrResult.av ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="Gage R&R">{{ grrResult.gageRR ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="零件变差 PV">{{ grrResult.pv ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="总变差 TV">{{ grrResult.tv ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="%Gage R&R">{{ grrResult.percentGageRR ?? '-' }}%</el-descriptions-item>
      <el-descriptions-item label="%EV">{{ grrResult.percentEV ?? '-' }}%</el-descriptions-item>
      <el-descriptions-item label="%AV">{{ grrResult.percentAV ?? '-' }}%</el-descriptions-item>
      <el-descriptions-item label="%PV">{{ grrResult.percentPV ?? '-' }}%</el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="grrVisible = false">关 闭</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { MsaStudyApi } from '@/api/qms/msa/study'
import { DICT_TYPE } from '@/utils/dict'
import MsaStudyForm from './MsaStudyForm.vue'

defineOptions({ name: 'MsaStudy' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  studyNo: undefined,
  studyType: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await MsaStudyApi.getMsaStudyPage(queryParams)
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
    await MsaStudyApi.deleteMsaStudy(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

const currentRow = ref<any>({})

/** 测量数据（列表 + 新增） */
const measureVisible = ref(false)
const measureLoading = ref(false)
const measureSaving = ref(false)
const measureList = ref<any[]>([])
const measureFormRef = ref()
const measureForm = ref<any>({})
const measureRules = reactive({
  measurementValue: [{ required: true, message: '测量值不能为空', trigger: 'blur' }]
})

const resetMeasureForm = () => {
  measureForm.value = {
    measurementValue: undefined,
    partId: undefined,
    appraiserId: undefined,
    trialNo: undefined
  }
}

const loadMeasurements = async () => {
  measureLoading.value = true
  try {
    measureList.value = (await MsaStudyApi.getMsaMeasurementList(currentRow.value.id)) ?? []
  } finally {
    measureLoading.value = false
  }
}

const openMeasure = async (row: any) => {
  currentRow.value = row
  resetMeasureForm()
  measureVisible.value = true
  await loadMeasurements()
}

const submitMeasure = async () => {
  await measureFormRef.value.validate()
  measureSaving.value = true
  try {
    await MsaStudyApi.saveMsaMeasurement({
      studyId: currentRow.value.id,
      partId: measureForm.value.partId ?? undefined,
      appraiserId: measureForm.value.appraiserId ?? undefined,
      trialNo: measureForm.value.trialNo ?? undefined,
      measurementValue: measureForm.value.measurementValue
    })
    message.success('测量值已保存')
    resetMeasureForm()
    await loadMeasurements()
  } finally {
    measureSaving.value = false
  }
}

/** Gage R&R 计算（会同时把研究状态置为已完成 30，故回读列表） */
const grrVisible = ref(false)
const grrLoading = ref(false)
const grrResult = ref<any>({})

const openGrr = async (row: any) => {
  currentRow.value = row
  grrResult.value = {}
  grrVisible.value = true
  grrLoading.value = true
  try {
    grrResult.value = (await MsaStudyApi.calculateMsaGageRR(row.id)) ?? {}
    await getList()
  } finally {
    grrLoading.value = false
  }
}

onMounted(() => getList())
</script>
