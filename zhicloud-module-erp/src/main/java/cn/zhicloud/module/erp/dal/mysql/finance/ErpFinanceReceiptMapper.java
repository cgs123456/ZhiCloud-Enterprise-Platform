package cn.zhicloud.module.erp.dal.mysql.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFinanceReceiptItemDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 收款单 Mapper
 *
 * @author 智云
 */
@Mapper
public interface ErpFinanceReceiptMapper extends BaseMapperX<ErpFinanceReceiptDO> {

    default PageResult<ErpFinanceReceiptDO> selectPage(ErpFinanceReceiptPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpFinanceReceiptDO> query = new MPJLambdaWrapperX<ErpFinanceReceiptDO>()
                .likeIfPresent(ErpFinanceReceiptDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpFinanceReceiptDO::getReceiptTime, reqVO.getReceiptTime())
                .eqIfPresent(ErpFinanceReceiptDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpFinanceReceiptDO::getCreator, reqVO.getCreator())
                .eqIfPresent(ErpFinanceReceiptDO::getFinanceUserId, reqVO.getFinanceUserId())
                .eqIfPresent(ErpFinanceReceiptDO::getAccountId, reqVO.getAccountId())
                .eqIfPresent(ErpFinanceReceiptDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpFinanceReceiptDO::getRemark, reqVO.getRemark())
                .orderByDesc(ErpFinanceReceiptDO::getId);
        if (reqVO.getBizNo() != null) {
            query.leftJoin(ErpFinanceReceiptItemDO.class, ErpFinanceReceiptItemDO::getReceiptId, ErpFinanceReceiptDO::getId)
                    .eq(reqVO.getBizNo() != null, ErpFinanceReceiptItemDO::getBizNo, reqVO.getBizNo())
                    .groupBy(ErpFinanceReceiptDO::getId); // 避免 1 对多查询，产生相同的 1
        }
        return selectJoinPage(reqVO, ErpFinanceReceiptDO.class, query);
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpFinanceReceiptDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpFinanceReceiptDO>()
                .eq(ErpFinanceReceiptDO::getId, id).eq(ErpFinanceReceiptDO::getStatus, status));
    }

    default ErpFinanceReceiptDO selectByNo(String no) {
        return selectOne(ErpFinanceReceiptDO::getNo, no);
    }

    /**
     * 按客户端幂等键查已有收款单。
     *
     * <p>租户条件由 MP 租户拦截器自动拼接（DO 已继承 {@code TenantBaseDO}），
     * 因此同键在不同租户互不可见，与唯一键 {@code (tenant_id, idempotency_key)} 口径一致。</p>
     * <p><b>复制本模式前先确认锁序</b>：本建单路径「INSERT 是本事务第一处写库」，
     * 且 1.1~1.6 的预检全都不加锁（已逐个核实），所以输家撞唯一键时只持该唯一键的
     * insert intention 锁、不持业务行锁，输家之间不会与业务锁交叉成环。</p>
     *
     * <p><b>但这个前提只管「输家与业务锁不成环」，不管回查自身会死锁</b>：
     * 见 {@link #selectByIdempotencyKeyForUpdate}，多个输家排在同一行的 X 锁后面
     * 彼此成环，实测 348 次死锁。照搬本 catch 分支必须连带照搬
     * {@code ErpFinanceReceiptServiceImpl#awaitWinner} 的退避重试，只抄一半就是 500。</p>
     *
     * <p>若某条建单路径在 INSERT 之前已经 FOR UPDATE 过业务行
     * （例如 CRM 回款单先锁合同行与回款列表），本模式的前提不再成立，
     * 需重新实测，不能直接照搬。</p>
     */
    default ErpFinanceReceiptDO selectByIdempotencyKey(String idempotencyKey) {
        return selectOne(ErpFinanceReceiptDO::getIdempotencyKey, idempotencyKey);
    }

    /**
     * 锁读幂等键对应的行，用于唯一键冲突后等赢家提交。
     *
     * <p><b>本方法自身会死锁，调用方必须退避重试</b>（见 {@code awaitWinner}）。
     * 多个输家排在同一行 X 锁后面会成环，InnoDB 挑牺牲品回滚。实测：不退避重试的版本
     * 16 线程 × 20 轮 348 次死锁、每轮 3~4 个 500；退避后同样负载 0 次死锁、320/320 成功。</p>
     *
     * <p>回查必须锁读而不能用普通读 + 自旋：赢家未提交时普通读受 RR 快照限制读不到，
     * 会一直空转到上界后重抛（§12.233 实测 528 次无一命中）。锁读会阻塞到赢家提交后立即可见。</p>
     *
     * <p>不要把锁读挪到 1.1 预检处当占位锁：唯一索引上「查不到行」的 FOR UPDATE 只加间隙锁，
     * 而间隙锁之间互相兼容（§12.233.4 实测 B 在 A 持锁期间 0.27s 即返回），挡不住并发，
     * 全部线程照旧走到 INSERT，反而放大锁竞争。</p>
     */
    default ErpFinanceReceiptDO selectByIdempotencyKeyForUpdate(String idempotencyKey) {
        return selectOneForUpdate(ErpFinanceReceiptDO::getIdempotencyKey, idempotencyKey);
    }
}