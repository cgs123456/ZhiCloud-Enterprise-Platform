<template>
  <doc-alert title="AI 手册" url="https://doc.zhicloud.cn/ai/build/" />

  <!-- 执行表单 -->
  <ContentWrap>
    <el-form :model="form" label-width="100px" :disabled="running">
      <el-form-item label="编排拓扑" prop="topologyId">
        <el-select
          v-model="form.topologyId"
          placeholder="请选择拓扑"
          filterable
          class="!w-320px"
          :loading="topologyLoading"
        >
          <el-option
            v-for="item in topologyList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <el-button class="ml-10px" @click="loadTopologies">
          <Icon icon="ep:refresh" class="mr-5px" /> 刷新
        </el-button>
      </el-form-item>
      <el-form-item label="任务输入" prop="userInput">
        <el-input
          v-model="form.userInput"
          type="textarea"
          :rows="3"
          placeholder="请输入要编排的任务，例如：帮我生成一份本月库存报告"
          class="!w-520px"
          @keyup.enter.ctrl="handleRun"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="running" @click="handleRun" v-hasPermi="['aimultiagent:execute:run']">
          <Icon icon="ep:video-play" class="mr-5px" /> 开始执行（SSE 实时）
        </el-button>
        <el-button v-if="running" type="danger" plain @click="handleStop">停止</el-button>
        <el-button
          v-if="!running && lastExecutionLogId != null"
          plain
          @click="handleResume"
          v-hasPermi="['aimultiagent:execute:run']"
        >
          从断点恢复
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 实时进度 -->
  <ContentWrap v-if="events.length > 0 || finalAnswer || errorMsg">
    <div class="mb-10px text-14px font-600">执行进度</div>
    <el-timeline>
      <el-timeline-item
        v-for="(evt, index) in events"
        :key="index"
        :type="eventType(evt)"
        :timestamp="formatTime(evt.timestamp)"
        placement="top"
      >
        {{ eventText(evt) }}
      </el-timeline-item>
    </el-timeline>
    <el-alert
      v-if="finalAnswer"
      title="最终结果"
      type="success"
      :closable="false"
      show-icon
    >
      <div class="whitespace-pre-wrap">{{ finalAnswer }}</div>
      <div v-if="totalTokens != null" class="mt-5px text-12px">Token 消耗：{{ totalTokens }}</div>
    </el-alert>
    <el-alert v-if="errorMsg" :title="errorMsg" type="error" :closable="false" show-icon />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { MultiAgentExecuteApi, MultiAgentTopologyApi, type AgentSseEventVO } from '@/api/aimultiagent/execute'

defineOptions({ name: 'AiMultiAgentExecute' })

const message = useMessage()

const form = reactive({
  topologyId: undefined as number | undefined,
  userInput: ''
})
const topologyList = ref<any[]>([])
const topologyLoading = ref(false)
const running = ref(false)
const events = ref<AgentSseEventVO[]>([])
const finalAnswer = ref('')
const errorMsg = ref('')
const totalTokens = ref<number | null>(null)
const lastExecutionLogId = ref<number | null>(null)
let ctrl: AbortController | null = null

/** 加载拓扑下拉列表（分页接口返回 PageResult，取 list） */
const loadTopologies = async () => {
  topologyLoading.value = true
  try {
    const data: any = await MultiAgentTopologyApi.getTopologyPage({ pageNo: 1, pageSize: 100 })
    topologyList.value = data?.list ?? data ?? []
  } finally {
    topologyLoading.value = false
  }
}

/** 解析事件内层 data（后端为 JSON 字符串） */
const parseData = (data?: string): any => {
  if (!data) return {}
  try {
    return JSON.parse(data)
  } catch {
    return { message: data }
  }
}

/** 开始 SSE 执行 */
const handleRun = async () => {
  if (form.topologyId == null) {
    message.warning('请先选择编排拓扑')
    return
  }
  if (!form.userInput.trim()) {
    message.warning('请输入任务内容')
    return
  }
  resetRun()
  running.value = true
  ctrl = new AbortController()
  try {
    await MultiAgentExecuteApi.runStream(
      form.topologyId,
      form.userInput,
      ctrl,
      (evt: any) => {
        // fetch-event-source 回调：evt.event 为事件名，evt.data 为 JSON 字符串
        let payload: AgentSseEventVO
        try {
          payload = JSON.parse(evt.data)
        } catch {
          return
        }
        events.value.push(payload)
        const inner = parseData(payload.data)
        if (payload.type === 'final') {
          finalAnswer.value = inner.finalAnswer ?? ''
          totalTokens.value = inner.totalTokens ?? null
          lastExecutionLogId.value = inner.executionLogId ?? null
          running.value = false
        } else if (payload.type === 'error' || payload.type === 'circuit_breaker') {
          errorMsg.value = inner.message ?? '执行失败'
          lastExecutionLogId.value = inner.executionLogId ?? null
          running.value = false
        }
      },
      (err: any) => {
        // 连接异常：后端已落库，可稍后用“从断点恢复”继续
        if (running.value) {
          errorMsg.value = '连接中断，可点击“从断点恢复”继续执行'
          running.value = false
        }
        throw err
      },
      () => {
        running.value = false
      }
    )
  } catch (e) {
    // AbortController 主动停止不提示
    if (ctrl?.signal.aborted) {
      return
    }
    if (running.value) {
      errorMsg.value = '执行请求失败，请稍后重试'
      running.value = false
    }
  }
}

/** 停止推送（执行本身会在后端跑完并落库） */
const handleStop = () => {
  ctrl?.abort()
  running.value = false
}

/** 从断点恢复上次失败的执行 */
const handleResume = async () => {
  if (lastExecutionLogId.value == null) {
    return
  }
  resetRun(true)
  running.value = true
  try {
    const data: any = await MultiAgentExecuteApi.resume(lastExecutionLogId.value)
    finalAnswer.value = data?.finalAnswer ?? ''
    if (data?.status !== 1) {
      errorMsg.value = data?.errorMsg ?? '恢复后仍未成功'
    } else {
      totalTokens.value = data?.totalTokens ?? null
    }
  } catch {
    errorMsg.value = '恢复失败：无可恢复的检查点或状态已损坏'
  } finally {
    running.value = false
  }
}

const resetRun = (keepLogId = false) => {
  events.value = []
  finalAnswer.value = ''
  errorMsg.value = ''
  totalTokens.value = null
  if (!keepLogId) {
    lastExecutionLogId.value = null
  }
}

const eventType = (evt: AgentSseEventVO) => {
  if (evt.type === 'error') return 'danger'
  if (evt.type === 'circuit_breaker') return 'warning'
  if (evt.type === 'final') return 'success'
  if (evt.type === 'worker_done') return 'success'
  return 'primary'
}

const eventText = (evt: AgentSseEventVO) => {
  const inner = parseData(evt.data)
  switch (evt.type) {
    case 'plan_completed':
      return `任务拆解完成，共 ${inner.taskCount ?? evt.totalTasks} 个子任务`
    case 'worker_started':
      return `Worker 启动：${evt.workerName}（${evt.taskIndex + 1}/${evt.totalTasks}）`
    case 'worker_done':
      return `Worker 完成：${evt.workerName}（${inner.success ? '成功' : '失败'}）`
    case 'circuit_breaker':
      return `熔断：${inner.message ?? ''}`
    case 'error':
      return `失败：${inner.message ?? ''}`
    case 'final':
      return '执行完成'
    default:
      return evt.type
  }
}

const formatTime = (ts?: number) => {
  if (!ts) return ''
  return new Date(ts).toLocaleTimeString()
}

onMounted(() => {
  loadTopologies()
})
</script>
