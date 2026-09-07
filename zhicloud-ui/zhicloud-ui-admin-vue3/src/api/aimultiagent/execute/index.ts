import request from '@/config/axios'
import { fetchEventSource } from '@microsoft/fetch-event-source'
import { getAccessToken } from '@/utils/auth'
import { config } from '@/config/axios/config'

// SSE 进度事件（与后端 AgentSseEvent 对应）
export interface AgentSseEventVO {
  type: string // plan_completed | worker_started | worker_done | circuit_breaker | error | final
  taskIndex: number // 当前任务序号
  totalTasks: number // 总任务数
  workerName?: string // Worker 名称
  data?: string // 事件数据（JSON 字符串）
  timestamp: number
}

// 执行日志
export interface MultiAgentExecutionLogVO {
  id: number
  topologyId: number
  userInput: string
  supervisorPlan?: string
  workerResults?: string
  finalAnswer?: string
  totalTokens: number
  actualDepth: number
  status: number // 0进行中 1成功 2失败 3熔断
  errorMsg?: string
  durationMs: number
  createTime: string
}

// 检查点
export interface MultiAgentCheckpointVO {
  id: number
  executionLogId: number
  topologyId: number
  checkpointType: string // PLAN_COMPLETED / WORKER_DONE / SUMMARIZE_DONE
  workerName?: string
  taskIndex?: number
  createTime: string
}

// 多 Agent 编排执行
export const MultiAgentExecuteApi = {
  // 同步执行编排
  run: async (topologyId: number, userInput: string) => {
    return await request.post({
      url: '/aimultiagent/execute/run',
      data: { topologyId, userInput }
    })
  },

  // 从检查点恢复中断的执行
  resume: async (executionLogId: number) => {
    return await request.post({
      url: `/aimultiagent/execute/resume?executionLogId=${executionLogId}`
    })
  },

  // 查询某次执行的检查点列表
  getCheckpoints: async (executionLogId: number) => {
    return await request.get({
      url: `/aimultiagent/checkpoint/list?executionLogId=${executionLogId}`
    })
  },

  // 查询执行日志
  getLog: async (id: number) => {
    return await request.get({ url: `/aimultiagent/execute/log?id=${id}` })
  },

  // SSE 实时执行（fetch-event-source 支持 POST + Authorization Header，原生 EventSource 做不到）
  runStream: async (
    topologyId: number,
    userInput: string,
    ctrl: AbortController,
    onMessage: (evt: any) => void,
    onError: (err: any) => void,
    onClose?: () => void
  ) => {
    const token = getAccessToken()
    return fetchEventSource(`${config.base_url}/aimultiagent/execute/run-stream`, {
      method: 'post',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      openWhenHidden: true,
      body: JSON.stringify({ topologyId, userInput }),
      onmessage: onMessage,
      onerror: onError,
      onclose: onClose,
      signal: ctrl.signal
    })
  }
}

// 多 Agent 拓扑配置（执行页下拉选择用）
export const MultiAgentTopologyApi = {
  // 分页查询拓扑
  getTopologyPage: async (params: any) => {
    return await request.get({ url: '/aimultiagent/topology/page', params })
  }
}
