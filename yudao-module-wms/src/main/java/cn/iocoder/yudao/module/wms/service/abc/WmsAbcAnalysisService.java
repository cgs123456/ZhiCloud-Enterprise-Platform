package cn.iocoder.yudao.module.wms.service.abc;

import cn.iocoder.yudao.module.wms.controller.admin.abc.vo.WmsAbcAnalysisReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.abc.vo.WmsAbcReportRespVO;

import jakarta.validation.Valid;

/**
 * WMS ABC 分类分析 Service 接口
 *
 * @author 芋道源码
 */
public interface WmsAbcAnalysisService {

    /**
     * 执行 ABC 分类分析
     *
     * @param reqVO 分析请求（统计时间窗口）
     * @return ABC 分类报告
     */
    WmsAbcReportRespVO analyzeAbcClassification(@Valid WmsAbcAnalysisReqVO reqVO);

    /**
     * 获取最近一次 ABC 分类报告
     *
     * @return ABC 分类报告
     */
    WmsAbcReportRespVO getAbcReport();

}
