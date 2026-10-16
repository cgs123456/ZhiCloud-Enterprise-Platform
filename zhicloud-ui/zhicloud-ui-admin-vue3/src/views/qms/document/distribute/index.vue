<!-- QMS 文档分发 -->
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
      <el-form-item label="受控文档 ID" prop="documentId">
        <el-input-number v-model="queryParams.documentId" class="!w-240px" :controls="false" clearable placeholder="请输入受控文档 ID" />
      </el-form-item>
      <el-form-item label="分发对象" prop="distributeTo">
        <el-input v-model="queryParams.distributeTo" class="!w-240px" clearable placeholder="请输入分发对象" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="签收人" prop="receivedBy">
        <el-input v-model="queryParams.receivedBy" class="!w-240px" clearable placeholder="请输入签收人" @keyup.enter="handleQuery" />
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
          v-hasPermi="['qms:document:distribute']"
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
      <el-table-column align="center" label="受控文档 ID" prop="documentId" min-width="110" />
      <el-table-column align="center" label="分发对象" prop="distributeTo" min-width="110" />
      <el-table-column align="center" label="分发份数" prop="distributeQty" min-width="110" />
      <el-table-column align="center" label="分发日期" prop="distributeDate" min-width="110" />
      <el-table-column align="center" label="签收人" prop="receivedBy" min-width="110" />
      <el-table-column align="center" label="签收日期" prop="receivedDate" min-width="110" />
      <el-table-column align="center" label="回收份数" prop="returnedQty" min-width="110" />
      <el-table-column align="center" label="回收日期" prop="returnedDate" min-width="110" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" />
      <el-table-column align="center" label="排序" prop="sort" min-width="110" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="240">
        <template #default="scope">
          <!-- 归还（登记归还数量与日期） -->
          <el-button
            v-hasPermi="['qms:document:distribute']"
            link
            type="warning"
            @click="openReturn(scope.row)"
          >
            归还
          </el-button>
          <el-button
            v-hasPermi="['qms:document:distribute']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:document:distribute']"
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
  <QmsDocumentDistributeForm ref="formRef" @success="getList" />

  <!-- 归还（登记归还数量与日期） -->
  <Dialog v-model="returnVisible" title="归还" width="480">
    <el-form ref="returnFormRef" :model="returnForm" :rules="returnRules" label-width="110px">
      <el-form-item label="分发对象">
        <span>{{ currentRow?.distributeTo }}（共 {{ currentRow?.distributeQty ?? 0 }} 份）</span>
      </el-form-item>
      <el-form-item label="归还数量" prop="returnedQty">
        <el-input-number v-model="returnForm.returnedQty" class="!w-1/1" :min="1" :controls="false" placeholder="请输入归还数量" />
      </el-form-item>
      <el-form-item label="归还日期" prop="returnedDate">
        <el-date-picker v-model="returnForm.returnedDate" class="!w-1/1" value-format="YYYY-MM-DD" type="date" placeholder="请选择归还日期" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="returnLoading" type="primary" @click="submitReturn">确 定</el-button>
      <el-button @click="returnVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { QmsDocumentDistributeApi } from '@/api/qms/document/distribute'
import QmsDocumentDistributeForm from './QmsDocumentDistributeForm.vue'

defineOptions({ name: 'QmsDocumentDistribute' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  documentId: undefined,
  distributeTo: undefined,
  receivedBy: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await QmsDocumentDistributeApi.getQmsDocumentDistributePage(queryParams)
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
    await QmsDocumentDistributeApi.deleteQmsDocumentDistribute(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

const currentRow = ref<any>({})

/** 归还（登记归还数量与日期） */
const returnVisible = ref(false)
const returnLoading = ref(false)
const returnFormRef = ref()
const returnForm = ref<any>({})
const returnRules = reactive({
  returnedQty: [{ required: true, message: '归还数量不能为空', trigger: 'blur' }],
  returnedDate: [{ required: true, message: '归还日期不能为空', trigger: 'change' }]
})

const openReturn = (row: any) => {
  currentRow.value = row
  returnForm.value = { returnedQty: row.distributeQty ?? undefined, returnedDate: undefined }
  returnVisible.value = true
}

const submitReturn = async () => {
  await returnFormRef.value.validate()
  returnLoading.value = true
  try {
    await QmsDocumentDistributeApi.returnQmsDocumentDistribute({
      id: currentRow.value.id,
      returnedQty: returnForm.value.returnedQty,
      returnedDate: returnForm.value.returnedDate
    })
    message.success('已登记归还')
    returnVisible.value = false
    await getList()
  } finally {
    returnLoading.value = false
  }
}

onMounted(() => getList())
</script>
