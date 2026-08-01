package cn.iocoder.yudao.module.qms.dal.mysql.electronicsignature;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.qms.controller.admin.electronicsignature.vo.ElectronicSignatureLogPageReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.electronicsignature.ElectronicSignatureLogDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * QMS 电子签名记录 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ElectronicSignatureLogMapper extends BaseMapperX<ElectronicSignatureLogDO> {

    default PageResult<ElectronicSignatureLogDO> selectPage(ElectronicSignatureLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ElectronicSignatureLogDO>()
                .eqIfPresent(ElectronicSignatureLogDO::getUserId, reqVO.getUserId())
                .likeIfPresent(ElectronicSignatureLogDO::getSignatureMeaning, reqVO.getSignatureMeaning())
                .likeIfPresent(ElectronicSignatureLogDO::getOperationType, reqVO.getOperationType())
                .betweenIfPresent(ElectronicSignatureLogDO::getSignatureTime, reqVO.getSignatureTime())
                .orderByDesc(ElectronicSignatureLogDO::getId));
    }

}
