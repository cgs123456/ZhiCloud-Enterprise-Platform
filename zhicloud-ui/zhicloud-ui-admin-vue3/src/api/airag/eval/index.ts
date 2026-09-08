import request from '@/config/axios'

// 评估数据集
export interface AiragEvalDatasetVO {
  id: number
  name: string
  description?: string
  knowledgeId: number
  questionCount: number
  status: number
  createTime: string
}

// 批量评估报告
export interface AiragEvalReportVO {
  id: number
  datasetId: number
  status: number // 0执行中 1已完成 2失败
  totalCount: number
  doneCount: number
  avgFaithfulness?: number
  avgAnswerRelevancy?: number
  avgContextPrecision?: number
  avgContextRecall?: number
  overallScore?: number
  overallDelta?: number
  regressedQuestions?: string
  improvedQuestions?: string
  resultsJson?: string
  errorMsg?: string
  createTime: string
}

// RAG 批量评估
export const AiragEvalApi = {
  // 创建评估数据集
  createDataset: async (data: { name: string; description?: string; knowledgeId: number }) => {
    return await request.post({ url: '/ai/rag/eval/dataset/create', data })
  },

  // 批量导入问题
  importQuestions: async (data: { datasetId: number; questions: any[] }) => {
    return await request.post({ url: '/ai/rag/eval/dataset/import', data })
  },

  // 查询数据集列表
  getDatasetList: async (knowledgeId?: number) => {
    return await request.get({ url: '/ai/rag/eval/dataset/list', params: { knowledgeId } })
  },

  // 触发批量评估（异步，返回报告编号）
  runBatch: async (datasetId: number) => {
    return await request.post({ url: `/ai/rag/eval/run?datasetId=${datasetId}` })
  },

  // 查询报告详情（含进度）
  getReport: async (id: number) => {
    return await request.get({ url: `/ai/rag/eval/report/get?id=${id}` })
  },

  // 最新已完成报告
  getLatestReport: async (datasetId: number) => {
    return await request.get({ url: `/ai/rag/eval/report/latest?datasetId=${datasetId}` })
  },

  // 历史趋势
  getReportTrend: async (datasetId: number, limit = 20) => {
    return await request.get({ url: `/ai/rag/eval/report/trend?datasetId=${datasetId}&limit=${limit}` })
  }
}
