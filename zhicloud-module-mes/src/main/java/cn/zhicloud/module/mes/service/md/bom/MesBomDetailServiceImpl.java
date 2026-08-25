package cn.zhicloud.module.mes.service.md.bom;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.mes.controller.admin.md.bom.vo.MesBomDetailPageReqVO;
import cn.zhicloud.module.mes.controller.admin.md.bom.vo.MesBomDetailSaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.md.bom.MesBomDetailDO;
import cn.zhicloud.module.mes.dal.mysql.md.bom.MesBomDetailMapper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.MD_BOM_DETAIL_NOT_EXISTS;

/**
 * MES BOM 明细 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class MesBomDetailServiceImpl implements MesBomDetailService {

    @Resource
    private MesBomDetailMapper bomDetailMapper;

    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private MesBomService bomService;

    @Override
    public Long createBomDetail(MesBomDetailSaveReqVO createReqVO) {
        // 校验 BOM 主数据存在
        bomService.validateBomExists(createReqVO.getBomId());
        // 插入
        MesBomDetailDO detail = BeanUtils.toBean(createReqVO, MesBomDetailDO.class);
        bomDetailMapper.insert(detail);
        return detail.getId();
    }

    @Override
    public void updateBomDetail(MesBomDetailSaveReqVO updateReqVO) {
        validateBomDetailExists(updateReqVO.getId());
        bomService.validateBomExists(updateReqVO.getBomId());
        MesBomDetailDO updateObj = BeanUtils.toBean(updateReqVO, MesBomDetailDO.class);
        bomDetailMapper.updateById(updateObj);
    }

    @Override
    public void deleteBomDetail(Long id) {
        validateBomDetailExists(id);
        bomDetailMapper.deleteById(id);
    }

    @Override
    public MesBomDetailDO validateBomDetailExists(Long id) {
        MesBomDetailDO detail = bomDetailMapper.selectById(id);
        if (detail == null) {
            throw exception(MD_BOM_DETAIL_NOT_EXISTS);
        }
        return detail;
    }

    @Override
    public PageResult<MesBomDetailDO> getBomDetailPage(MesBomDetailPageReqVO pageReqVO) {
        return bomDetailMapper.selectPage(pageReqVO);
    }

    @Override
    public List<MesBomDetailDO> getBomDetailListByBomId(Long bomId) {
        return bomDetailMapper.selectListByBomId(bomId);
    }

    @Override
    public List<MesBomDetailDO> getBomDetailListByBomIds(Collection<Long> bomIds) {
        if (CollUtil.isEmpty(bomIds)) {
            return Collections.emptyList();
        }
        return bomDetailMapper.selectListByBomIds(bomIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBomDetailList(Long bomId, List<MesBomDetailSaveReqVO> details) {
        // 先删除原明细，再批量插入
        bomDetailMapper.deleteByBomId(bomId);
        if (CollUtil.isEmpty(details)) {
            return;
        }
        List<MesBomDetailDO> detailList = BeanUtils.toBean(details, MesBomDetailDO.class);
        detailList.forEach(d -> d.setBomId(bomId));
        bomDetailMapper.insertBatch(detailList);
    }

    @Override
    public void deleteBomDetailByBomId(Long bomId) {
        bomDetailMapper.deleteByBomId(bomId);
    }

}