package cn.zhicloud.module.promotion.service.discount;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.zhicloud.framework.common.enums.CommonStatusEnum;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.collection.CollectionUtils;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.product.api.sku.ProductSkuApi;
import cn.zhicloud.module.product.api.sku.dto.ProductSkuRespDTO;
import cn.zhicloud.module.promotion.controller.admin.discount.vo.DiscountActivityBaseVO;
import cn.zhicloud.module.promotion.controller.admin.discount.vo.DiscountActivityCreateReqVO;
import cn.zhicloud.module.promotion.controller.admin.discount.vo.DiscountActivityPageReqVO;
import cn.zhicloud.module.promotion.controller.admin.discount.vo.DiscountActivityUpdateReqVO;
import cn.zhicloud.module.promotion.convert.discount.DiscountActivityConvert;
import cn.zhicloud.module.promotion.dal.dataobject.discount.DiscountActivityDO;
import cn.zhicloud.module.promotion.dal.dataobject.discount.DiscountProductDO;
import cn.zhicloud.module.promotion.dal.mysql.discount.DiscountActivityMapper;
import cn.zhicloud.module.promotion.dal.mysql.discount.DiscountProductMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static cn.hutool.core.collection.CollUtil.intersectionDistinct;
import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.*;
import static cn.zhicloud.framework.common.util.collection.MapUtils.findAndThen;
import static cn.zhicloud.module.product.enums.ErrorCodeConstants.SKU_NOT_EXISTS;
import static cn.zhicloud.module.promotion.enums.ErrorCodeConstants.*;

/**
 * 限时折扣 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class DiscountActivityServiceImpl implements DiscountActivityService {

    @Resource
    private DiscountActivityMapper discountActivityMapper;
    @Resource
    private DiscountProductMapper discountProductMapper;

    @Resource
    private ProductSkuApi productSkuApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDiscountActivity(DiscountActivityCreateReqVO createReqVO) {
        // 校验商品是否冲突
        validateDiscountActivityProductConflicts(null, createReqVO.getProducts());
        // 校验商品是否存在
        validateProductExists(createReqVO.getProducts());

        // 插入活动
        DiscountActivityDO discountActivity = DiscountActivityConvert.INSTANCE.convert(createReqVO)
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        discountActivityMapper.insert(discountActivity);
        // 插入商品
        List<DiscountProductDO> discountProducts = BeanUtils.toBean(createReqVO.getProducts(), DiscountProductDO.class,
                product -> product.setActivityId(discountActivity.getId())
                        .setActivityName(discountActivity.getName()).setActivityStatus(discountActivity.getStatus())
                        .setActivityStartTime(createReqVO.getStartTime()).setActivityEndTime(createReqVO.getEndTime()));
        discountProductMapper.insertBatch(discountProducts);
        // 返回
        return discountActivity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDiscountActivity(DiscountActivityUpdateReqVO updateReqVO) {
        // 校验存在
        DiscountActivityDO discountActivity = validateDiscountActivityExists(updateReqVO.getId());
        if (discountActivity.getStatus().equals(CommonStatusEnum.DISABLE.getStatus())) { // 已关闭的活动，不能修改噢
            throw exception(DISCOUNT_ACTIVITY_UPDATE_FAIL_STATUS_CLOSED);
        }
        // 校验商品是否冲突
        validateDiscountActivityProductConflicts(updateReqVO.getId(), updateReqVO.getProducts());
        // 校验商品是否存在
        validateProductExists(updateReqVO.getProducts());

        // 更新活动
        DiscountActivityDO updateObj = DiscountActivityConvert.INSTANCE.convert(updateReqVO);
        discountActivityMapper.updateById(updateObj);
        // 更新商品
        updateDiscountProduct(updateObj, updateReqVO.getProducts());
    }

    private void updateDiscountProduct(DiscountActivityDO activity, List<DiscountActivityCreateReqVO.Product> products) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<DiscountProductDO> newList = BeanUtils.toBean(products, DiscountProductDO.class,
                product -> product.setActivityId(activity.getId())
                        .setActivityName(activity.getName()).setActivityStatus(activity.getStatus())
                        .setActivityStartTime(activity.getStartTime()).setActivityEndTime(activity.getEndTime()));
        List<DiscountProductDO> oldList = discountProductMapper.selectListByActivityId(activity.getId());
        List<List<DiscountProductDO>> diffList = CollectionUtils.diffList(oldList, newList, (oldVal, newVal) -> {
            boolean same = ObjectUtil.equal(oldVal.getSkuId(), newVal.getSkuId());
            if (same) {
                newVal.setId(oldVal.getId());
            }
            return same;
        });

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            discountProductMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            discountProductMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            discountProductMapper.deleteByIds(convertList(diffList.get(2), DiscountProductDO::getId));
        }
    }

    /**
     * 校验商品是否冲突
     *
     * @param id       编号
     * @param products 商品列表
     */
    private void validateDiscountActivityProductConflicts(Long id, List<DiscountActivityBaseVO.Product> products) {
        // 1.1 查询所有开启的折扣活动
        List<DiscountActivityDO> activityList = discountActivityMapper.selectList(DiscountActivityDO::getStatus,
                CommonStatusEnum.ENABLE.getStatus());
        if (id != null) { // 时排除自己
            activityList.removeIf(item -> ObjectUtil.equal(item.getId(), id));
        }
        // 1.2 查询活动下的所有商品
        List<DiscountProductDO> productList = discountProductMapper.selectListByActivityId(
                convertList(activityList, DiscountActivityDO::getId));
        Map<Long, List<DiscountProductDO>> productListMap = convertMultiMap(productList, DiscountProductDO::getActivityId);

        // 2. 校验商品是否冲突
        activityList.forEach(item -> {
            findAndThen(productListMap, item.getId(), discountProducts -> {
                if (!intersectionDistinct(convertList(discountProducts, DiscountProductDO::getSpuId),
                        convertList(products, DiscountActivityBaseVO.Product::getSpuId)).isEmpty()) {
                    throw exception(DISCOUNT_ACTIVITY_SPU_CONFLICTS, item.getName());
                }
            });
        });
    }

    /**
     * 校验活动商品是否都存在
     *
     * @param products 活动商品
     */
    private void validateProductExists(List<DiscountActivityBaseVO.Product> products) {
        // 1.获得商品所有的 sku
        List<ProductSkuRespDTO> skus = productSkuApi.getSkuListBySpuId(
                convertList(products, DiscountActivityBaseVO.Product::getSpuId));
        Map<Long, ProductSkuRespDTO> skuMap = convertMap(skus, ProductSkuRespDTO::getId);
        // 2. 校验商品 sku 都存在
        products.forEach(product -> {
            if (!skuMap.containsKey(product.getSkuId())) {
                throw exception(SKU_NOT_EXISTS);
            }
        });
    }

    @Override
    // 资损风险修复：活动状态与活动商品状态两表联动更新，此前无事务。
    // 若第 2 步失败，活动显示「已关闭」但商品仍挂着「生效中」的限时折扣，
    // 用户下单时继续按折扣价成交，属直接资损，且运营在后台看到的是「已关闭」故不会察觉。
    @Transactional(rollbackFor = Exception.class)
    public void closeDiscountActivity(Long id) {
        // 校验存在
        DiscountActivityDO activity = validateDiscountActivityExists(id);
        if (activity.getStatus().equals(CommonStatusEnum.DISABLE.getStatus())) { // 已关闭的活动，不能关闭噢
            throw exception(DISCOUNT_ACTIVITY_CLOSE_FAIL_STATUS_CLOSED);
        }

        // 更新活动状态
        discountActivityMapper.updateById(new DiscountActivityDO().setId(id).setStatus(CommonStatusEnum.DISABLE.getStatus()));
        // 更新活动商品状态
        discountProductMapper.updateByActivityId(new DiscountProductDO().setActivityId(id).setActivityStatus(
                CommonStatusEnum.DISABLE.getStatus()));
    }

    @Override
    // 原子性修复：删除活动与删除活动商品两步无事务，
    // 中途失败会残留孤儿 discount_product 记录，且因主活动已删除无法再从后台清理。
    @Transactional(rollbackFor = Exception.class)
    public void deleteDiscountActivity(Long id) {
        // 校验存在
        DiscountActivityDO activity = validateDiscountActivityExists(id);
        if (CommonStatusEnum.isEnable(activity.getStatus())) { // 未关闭的活动，不能删除噢
            throw exception(DISCOUNT_ACTIVITY_DELETE_FAIL_STATUS_NOT_CLOSED);
        }

        // 删除活动
        discountActivityMapper.deleteById(id);
        // 删除活动商品
        discountProductMapper.deleteByActivityId(id);
    }

    private DiscountActivityDO validateDiscountActivityExists(Long id) {
        DiscountActivityDO discountActivity = discountActivityMapper.selectById(id);
        if (discountActivity == null) {
            throw exception(DISCOUNT_ACTIVITY_NOT_EXISTS);
        }
        return discountActivity;
    }

    @Override
    public DiscountActivityDO getDiscountActivity(Long id) {
        return discountActivityMapper.selectById(id);
    }

    @Override
    public PageResult<DiscountActivityDO> getDiscountActivityPage(DiscountActivityPageReqVO pageReqVO) {
        return discountActivityMapper.selectPage(pageReqVO);
    }

    @Override
    public List<DiscountProductDO> getDiscountProductsByActivityId(Long activityId) {
        return discountProductMapper.selectListByActivityId(activityId);
    }

    @Override
    public List<DiscountProductDO> getDiscountProductsByActivityId(Collection<Long> activityIds) {
        if (CollUtil.isEmpty(activityIds)) {
            return CollUtil.newArrayList();
        }
        return discountProductMapper.selectList(DiscountProductDO::getActivityId, activityIds);
    }

    @Override
    public List<DiscountProductDO> getMatchDiscountProductListBySkuIds(Collection<Long> skuIds) {
        if (CollUtil.isEmpty(skuIds)) {
            return CollUtil.newArrayList();
        }
        return discountProductMapper.selectListBySkuIdsAndStatusAndNow(skuIds, CommonStatusEnum.ENABLE.getStatus());
    }

}
