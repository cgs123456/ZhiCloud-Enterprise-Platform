import request from '@/config/axios'

// Wms AbcAnalysis API
export const AbcAnalysisApi = {
  AbcAnalysisAnalyze: async (data: any) => {
    // 触发 ABC 分类分析
    return await request.post({ url: `/wms/abc-analysis/analyze`, data })
  },
  AbcAnalysisReport: async (data: any) => {
    // 获取 ABC 分类报告
    return await request.get({ url: `/wms/abc-analysis/report`, data })
  },
}
