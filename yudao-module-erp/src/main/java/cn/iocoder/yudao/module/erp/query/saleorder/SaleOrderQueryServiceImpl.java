package cn.iocoder.yudao.module.erp.query.saleorder;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderMapper;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

/**
 * 销售订单查询服务实现（CQRS 试点读侧）
 *
 * <p>只读查询实现，复用现有 {@link ErpSaleOrderMapper} / {@link ErpSaleOrderItemMapper}，
 * 将 DO 投影为 {@link SaleOrderView}。投影过程中冗余客户名称、销售员名称、产品名称，
 * 避免前端二次查询。
 *
 * <p>与现有 {@code query.sale.SaleOrderQueryServiceImpl} 并存，互不影响。
 * 使用显式 Bean 名称 {@code dddSaleOrderQueryServiceImpl} 避免与现有 Bean 冲突。
 *
 * @author DDD 试点
 */
@Service("dddSaleOrderQueryServiceImpl")
@Validated
public class SaleOrderQueryServiceImpl implements SaleOrderQueryService {

    @Resource
    private ErpSaleOrderMapper saleOrderMapper;
    @Resource
    private ErpSaleOrderItemMapper saleOrderItemMapper;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpProductService productService;
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public SaleOrderView getSaleOrderView(Long id) {
        ErpSaleOrderDO order = saleOrderMapper.selectById(id);
        if (order == null) {
            return null;
        }
        List<ErpSaleOrderItemDO> itemDOs = saleOrderItemMapper.selectListByOrderId(id);
        return projectDetail(order, itemDOs);
    }

    @Override
    public PageResult<SaleOrderView> pageQuery(SaleOrderPageQuery query) {
        // 复用现有 Mapper 的分页查询（转换查询参数）
        ErpSaleOrderPageReqVO reqVO = convertToReqVO(query);
        PageResult<ErpSaleOrderDO> pageResult = saleOrderMapper.selectPage(reqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(pageResult.getTotal());
        }
        // 批量加载客户名称
        Set<Long> customerIds = convertSet(pageResult.getList(), ErpSaleOrderDO::getCustomerId);
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(customerIds);
        // 批量加载销售员名称
        Set<Long> saleUserIds = convertSet(pageResult.getList(), ErpSaleOrderDO::getSaleUserId);
        Map<Long, AdminUserRespDTO> userMap = saleUserIds.isEmpty()
                ? Map.of() : adminUserApi.getUserMap(saleUserIds);
        // 投影为视图（分页列表不含明细）
        List<SaleOrderView> list = convertList(pageResult.getList(), order ->
                projectSummary(order, customerMap, userMap));
        return new PageResult<>(list, pageResult.getTotal());
    }

    // ==================== 投影方法 ====================

    /**
     * 投影详情视图（含明细）
     */
    private SaleOrderView projectDetail(ErpSaleOrderDO order, List<ErpSaleOrderItemDO> itemDOs) {
        // 加载客户名称
        ErpCustomerDO customer = customerService.getCustomer(order.getCustomerId());
        // 加载销售员名称
        AdminUserRespDTO saleUser = order.getSaleUserId() == null
                ? null : adminUserApi.getUserMap(Set.of(order.getSaleUserId())).get(order.getSaleUserId());
        // 投影订单头
        SaleOrderView view = projectSummary(order,
                customer == null ? Map.of() : Map.of(customer.getId(), customer),
                saleUser == null ? Map.of() : Map.of(saleUser.getId(), saleUser));
        // 投影明细
        Set<Long> productIds = convertSet(itemDOs, ErpSaleOrderItemDO::getProductId);
        Map<Long, ErpProductRespVO> productMap = productIds.isEmpty()
                ? Map.of() : productService.getProductVOMap(productIds);
        List<SaleOrderView.Item> items = convertList(itemDOs, item -> {
            SaleOrderView.Item itemView = new SaleOrderView.Item();
            itemView.setId(item.getId());
            itemView.setProductId(item.getProductId());
            ErpProductRespVO product = productMap.get(item.getProductId());
            itemView.setProductName(product == null ? null : product.getName());
            itemView.setProductUnitId(item.getProductUnitId());
            itemView.setQuantity(item.getCount());
            itemView.setUnitPrice(item.getProductPrice());
            itemView.setTaxRate(item.getTaxPercent());
            itemView.setSubtotal(item.getTotalPrice());
            itemView.setTaxPrice(item.getTaxPrice());
            itemView.setOutCount(item.getOutCount());
            itemView.setReturnCount(item.getReturnCount());
            itemView.setRemark(item.getRemark());
            return itemView;
        });
        view.setItems(items);
        return view;
    }

    /**
     * 投影摘要视图（不含明细）
     */
    private SaleOrderView projectSummary(ErpSaleOrderDO order,
                                          Map<Long, ErpCustomerDO> customerMap,
                                          Map<Long, AdminUserRespDTO> userMap) {
        SaleOrderView view = new SaleOrderView();
        view.setId(order.getId());
        view.setNo(order.getNo());
        view.setStatus(order.getStatus());
        view.setCustomerId(order.getCustomerId());
        ErpCustomerDO customer = customerMap.get(order.getCustomerId());
        view.setCustomerName(customer == null ? null : customer.getName());
        view.setAccountId(order.getAccountId());
        view.setSaleUserId(order.getSaleUserId());
        AdminUserRespDTO saleUser = order.getSaleUserId() == null ? null : userMap.get(order.getSaleUserId());
        view.setSaleUserName(saleUser == null ? null : saleUser.getNickname());
        view.setOrderTime(order.getOrderTime());
        view.setTotalCount(order.getTotalCount());
        view.setTotalProductPrice(order.getTotalProductPrice());
        view.setTotalTaxPrice(order.getTotalTaxPrice());
        view.setDiscountPrice(order.getDiscountPrice());
        view.setTotalPrice(order.getTotalPrice());
        view.setDepositPrice(order.getDepositPrice());
        view.setCurrencyId(order.getCurrencyId());
        view.setExchangeRate(order.getExchangeRate());
        view.setBaseCurrencyTotalPrice(order.getBaseCurrencyTotalPrice());
        view.setOutCount(order.getOutCount());
        view.setReturnCount(order.getReturnCount());
        view.setFileUrl(order.getFileUrl());
        view.setRemark(order.getRemark());
        return view;
    }

    // ==================== 工具方法 ====================

    /**
     * 将 CQRS 查询参数转换为现有 Mapper 接受的 VO
     */
    private ErpSaleOrderPageReqVO convertToReqVO(SaleOrderPageQuery query) {
        ErpSaleOrderPageReqVO reqVO = new ErpSaleOrderPageReqVO();
        reqVO.setPageNo(query.getPageNo());
        reqVO.setPageSize(query.getPageSize());
        reqVO.setNo(query.getNo());
        reqVO.setCustomerId(query.getCustomerId());
        reqVO.setStatus(query.getStatus());
        reqVO.setOrderTime(query.getOrderTime());
        reqVO.setRemark(query.getRemark());
        return reqVO;
    }
}
