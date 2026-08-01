package cn.iocoder.yudao.module.qms.dal.mysql.training;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.qms.controller.admin.training.vo.QualificationPageReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.training.QualificationDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * QMS 岗位资格 Mapper
 *
 * @author yudao
 */
@Mapper
public interface QualificationMapper extends BaseMapperX<QualificationDO> {

    default PageResult<QualificationDO> selectPage(QualificationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QualificationDO>()
                .eqIfPresent(QualificationDO::getUserId, reqVO.getUserId())
                .likeIfPresent(QualificationDO::getUserName, reqVO.getUserName())
                .eqIfPresent(QualificationDO::getPostId, reqVO.getPostId())
                .eqIfPresent(QualificationDO::getStatus, reqVO.getStatus())
                .orderByDesc(QualificationDO::getId));
    }

    /**
     * 查询即将到期的资格列表（到期日 <= 指定日期且状态为有效）
     */
    default List<QualificationDO> selectExpiringList(LocalDate expireDate) {
        return selectList(new LambdaQueryWrapperX<QualificationDO>()
                .le(QualificationDO::getExpireDate, expireDate));
    }

}