package cn.zhicloud.module.tms.dal.mysql.vehicle;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.tms.controller.admin.vehicle.vo.TmsVehiclePageReqVO;
import cn.zhicloud.module.tms.dal.dataobject.vehicle.TmsVehicleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * TMS 车辆 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface TmsVehicleMapper extends BaseMapperX<TmsVehicleDO> {

    default PageResult<TmsVehicleDO> selectPage(TmsVehiclePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TmsVehicleDO>()
                .likeIfPresent(TmsVehicleDO::getPlateNo, reqVO.getPlateNo())
                .eqIfPresent(TmsVehicleDO::getCarrierId, reqVO.getCarrierId())
                .eqIfPresent(TmsVehicleDO::getStatus, reqVO.getStatus())
                .orderByDesc(TmsVehicleDO::getId));
    }

    default TmsVehicleDO selectByPlateNo(String plateNo) {
        return selectOne(TmsVehicleDO::getPlateNo, plateNo);
    }

    default List<TmsVehicleDO> selectListByStatus(Integer status) {
        return selectList(TmsVehicleDO::getStatus, status);
    }

    /**
     * 锁读车辆，用于 CAS 改写状态前取版本号。
     *
     * <p>必须是 FOR UPDATE：RR 隔离下普通一致性读可能看不到并发事务刚提交的版本，
     * 拿旧版本去 CAS 会误报版本不符。事务外调用锁会立即释放，故调用方都带
     * {@code @Transactional}。</p>
     */
    default TmsVehicleDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<TmsVehicleDO>()
                .eq(TmsVehicleDO::getId, id)
                .last("FOR UPDATE"));
    }

    /**
     * 按已知版本 CAS 改写车辆状态。
     *
     * <p>不需要自定义 SQL：DO 的 {@code version} 上有 {@code @Version}，
     * OptimisticLockerInnerInterceptor 会自动拼出 {@code WHERE version = ?} 并把
     * {@code SET version = version + 1}。所以这里就是 {@@code updateById} + 返回值检查
     * （同 §12.228 MesProWorkOrderServiceImpl 的写法）。</p>
     *
     * <p>车辆状态由运单 dispatch/confirmArrival 推进时改写。返回 0 说明这辆车在事务外被改过，
     * 调用方应抛冲突而不是继续——否则一次并发的保养/停用会被静默覆盖回「运输中」。</p>
     */
    default int updateStatusWithVersion(TmsVehicleDO updateObj) {
        return updateById(updateObj);
    }

}
