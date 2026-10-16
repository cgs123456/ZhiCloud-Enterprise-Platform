package cn.zhicloud.module.tms.dal.mysql.shipment;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.tms.controller.admin.shipment.vo.TmsShipmentPageReqVO;
import cn.zhicloud.module.tms.dal.dataobject.shipment.TmsShipmentDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * TMS 运单 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface TmsShipmentMapper extends BaseMapperX<TmsShipmentDO> {

    default PageResult<TmsShipmentDO> selectPage(TmsShipmentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TmsShipmentDO>()
                .likeIfPresent(TmsShipmentDO::getNo, reqVO.getNo())
                .eqIfPresent(TmsShipmentDO::getCarrierId, reqVO.getCarrierId())
                .eqIfPresent(TmsShipmentDO::getShipmentType, reqVO.getShipmentType())
                .eqIfPresent(TmsShipmentDO::getStatus, reqVO.getStatus())
                .likeIfPresent(TmsShipmentDO::getSourceOrderNo, reqVO.getSourceOrderNo())
                .orderByDesc(TmsShipmentDO::getId));
    }

    default TmsShipmentDO selectByNo(String no) {
        return selectOne(TmsShipmentDO::getNo, no);
    }

    /**
     * 状态机 CAS：仅当运单仍在 originStatus 时才推进到 targetStatus，并一并写入业务字段。
     *
     * <p>状态机单调推进（待发车 → 运输中 → 已到达 → 已签收），没有任何路径把运单改回待发车，
     * 所以「status=待发车」本身就是排他的派车资格。两个并发 dispatch 里只有一个能匹配到这行，
     * 另一个拿到 0 行。</p>
     *
     * <p>「校验源状态」和「写入新状态」必须同一条语句完成，不能拆成先查再改：拆开的话失败方
     * 在第 2 步查完了、第 5 步改完了，车辆/司机的状态也已经被改成「运输中」，那台车在窗口里是脏的。</p>
     *
     * <p>version 必须在 SQL 里显式 +1：MP 的 OptimisticLockerInnerInterceptor **在所有 update 路径上
     * 都会检查 entity 的 version 字段**（javap 反编译确认：进入该方法的条件只有 {@code et != null}
     * 与 {@code et.version != null}，与是否走 wrapper 无关）。它非空时会往 SET 里塞一个
     * {@code version = <original>}，再靠 {@code Field.set} 把 entity 里的值改成 original+1 去拼 WHERE。</p>
     *
     * <p>所以本方法**要求传入的 entity 不带 version**（方法体第一行强制置空，以防调用方误填）。
     * 否则：SET 子句里出现两个 version 赋值互相覆盖，而 WHERE 变成 {@code version = original+1}——
     * 库里那一行根本匹配不上，更新静默失效或语义错乱。当初的「假防护」就是因为没在 wrapper 里
     * 显式 +1，version 永远停在 0；现在反过来的坑是 entity 带了 version，两者都写在这里。</p>
     *
     * @param id           运单编号
     * @param originStatus 期望的源状态
     * @param targetStatus 推进到的目标状态
     * @param entity       要一并写入的业务字段（只填需要改的，未填的字段保持原值）
     * @return 1 推进成功，0 源状态不匹配（已被其他请求推进过）
     */
    default int transitStatusIfChanged(Long id, Integer originStatus, Integer targetStatus,
                                        TmsShipmentDO entity) {
        // 不变式提升为代码：拦截器只看 et.version，非空就会注入 SET version 与错位的 WHERE，
        // 与下面 wrapper 里显式的 version = version + 1 互相打架。见方法 Javadoc。
        if (entity == null) {
            throw new IllegalArgumentException("entity 不能为 null：wrapper 至少要带一个 SET 字段");
        }
        entity.setVersion(null);
        return update(entity, new LambdaUpdateWrapper<TmsShipmentDO>()
                .eq(TmsShipmentDO::getId, id)
                .eq(TmsShipmentDO::getStatus, originStatus)
                .set(TmsShipmentDO::getStatus, targetStatus)
                .setSql("version = version + 1"));
    }

    /**
     * 按已知版本 CAS 改写运单。
     *
     * <p>用于 updateShipment 这类「调用方整单提交」的路径：DO 的 {@code version} 上有
     * {@code @Version}，OptimisticLockerInnerInterceptor 会自动拼 {@code WHERE version = ?}，
     * 所以这里就是 updateById + 返回值检查。</p>
     *
     * <p>dispatch/confirmArrival/confirmSign 不走这里——它们要的是「校验源状态并推进」，
     * 见 {@link #transitStatusIfChanged}。</p>
     *
     * @param updateObj 要改的字段 + id + version
     * @return 1 成功，0 版本不匹配
     */
    default int updateByIdWithVersion(TmsShipmentDO updateObj) {
        return updateById(updateObj);
    }

}
