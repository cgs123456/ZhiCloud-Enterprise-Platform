package cn.iocoder.yudao.module.oa.service.portal;

import cn.iocoder.yudao.module.oa.controller.admin.portal.vo.OaPortalDashboardVO;

/**
 * OA 工作台门户 Service 接口
 *
 * @author yudao
 */
public interface OaPortalService {

    /**
     * 获取当前登录用户的工作台数据
     *
     * @return 工作台聚合数据
     */
    OaPortalDashboardVO getDashboard();

}
