package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.accountbook.ErpAccountBookPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.accountbook.ErpAccountBookSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountBookDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP 账簿 Service 接口（P1-多账簿）
 *
 * <p>支持多会计准则并行账簿，同一会计准则下最多一个主账簿。
 *
 * @author 芋道源码
 */
public interface ErpAccountBookService {

    /**
     * 创建账簿
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createAccountBook(@Valid ErpAccountBookSaveReqVO createReqVO);

    /**
     * 更新账簿
     *
     * @param updateReqVO 更新信息
     */
    void updateAccountBook(@Valid ErpAccountBookSaveReqVO updateReqVO);

    /**
     * 删除账簿（需校验是否被凭证引用）
     *
     * @param id 编号
     */
    void deleteAccountBook(Long id);

    /**
     * 获取账簿
     *
     * @param id 编号
     * @return 账簿
     */
    ErpAccountBookDO getAccountBook(Long id);

    /**
     * 分页查询账簿
     *
     * @param pageReqVO 分页查询
     * @return 账簿分页
     */
    PageResult<ErpAccountBookDO> getAccountBookPage(ErpAccountBookPageReqVO pageReqVO);

    /**
     * 获取启用的账簿列表（前端下拉用）
     *
     * @return 账簿列表
     */
    List<ErpAccountBookDO> getEnabledAccountBookList();

    /**
     * 切换主账簿
     *
     * <p>同一会计准则下只能有一个主账簿，调用此方法会将原主账簿置为非主账簿。
     *
     * @param id 编号
     */
    void setPrimary(Long id);

}
