package cn.iocoder.yudao.module.mes.service.md.bom.substitute;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.md.bom.substitute.vo.MesBomSubstitutePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.bom.substitute.vo.MesBomSubstituteSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.bom.MesBomDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.bom.MesBomSubstituteDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.bom.MesBomSubstituteMapper;
import cn.iocoder.yudao.module.mes.service.md.bom.MesBomDetailService;
import cn.iocoder.yudao.module.mes.service.md.bom.MesBomService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.BOM_SUBSTITUTE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.BOM_SUBSTITUTE_RATIO_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_BOM_DETAIL_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_ITEM_NOT_EXISTS;

/**
 * MES BOM 替代料 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class MesBomSubstituteServiceImpl implements MesBomSubstituteService {

    @Resource
    private MesBomSubstituteMapper bomSubstituteMapper;

    @Resource
    @Lazy // 延迟加载，避免与 MesBomServiceImpl 循环依赖
    private MesBomService bomService;

    @Resource
    @Lazy // 延迟加载，避免与 MesBomDetailService 循环依赖
    private MesBomDetailService bomDetailService;

    @Resource
    private MesMdItemService itemService;

    @Override
    public Long createBomSubstitute(MesBomSubstituteSaveReqVO createReqVO) {
        // 校验 BOM 主数据、明细、替代物料均存在，且明细归属于该 BOM
        validateBomAndDetail(createReqVO.getBomId(), createReqVO.getBomDetailId());
        validateSubstituteItem(createReqVO.getSubstituteItemId());
        validateRatio(createReqVO.getSubstituteRatio());
        // 插入
        MesBomSubstituteDO substitute = BeanUtils.toBean(createReqVO, MesBomSubstituteDO.class);
        if (substitute.getSubstituteRatio() == null) {
            substitute.setSubstituteRatio(BigDecimal.ONE);
        }
        if (substitute.getPriority() == null) {
            substitute.setPriority(1);
        }
        bomSubstituteMapper.insert(substitute);
        return substitute.getId();
    }

    @Override
    public void updateBomSubstitute(MesBomSubstituteSaveReqVO updateReqVO) {
        validateBomSubstituteExists(updateReqVO.getId());
        validateBomAndDetail(updateReqVO.getBomId(), updateReqVO.getBomDetailId());
        validateSubstituteItem(updateReqVO.getSubstituteItemId());
        validateRatio(updateReqVO.getSubstituteRatio());
        MesBomSubstituteDO updateObj = BeanUtils.toBean(updateReqVO, MesBomSubstituteDO.class);
        bomSubstituteMapper.updateById(updateObj);
    }

    @Override
    public void deleteBomSubstitute(Long id) {
        validateBomSubstituteExists(id);
        bomSubstituteMapper.deleteById(id);
    }

    @Override
    public MesBomSubstituteDO validateBomSubstituteExists(Long id) {
        MesBomSubstituteDO substitute = bomSubstituteMapper.selectById(id);
        if (substitute == null) {
            throw exception(BOM_SUBSTITUTE_NOT_EXISTS);
        }
        return substitute;
    }

    @Override
    public MesBomSubstituteDO getBomSubstitute(Long id) {
        return bomSubstituteMapper.selectById(id);
    }

    @Override
    public PageResult<MesBomSubstituteDO> getBomSubstitutePage(MesBomSubstitutePageReqVO pageReqVO) {
        return bomSubstituteMapper.selectPage(pageReqVO);
    }

    @Override
    public List<MesBomSubstituteDO> getSubstitutesByBomDetailId(Long bomDetailId) {
        if (bomDetailId == null) {
            return CollUtil.newArrayList();
        }
        return bomSubstituteMapper.selectListByBomDetailId(bomDetailId);
    }

    @Override
    public void deleteBomSubstituteByBomId(Long bomId) {
        if (bomId == null) {
            return;
        }
        bomSubstituteMapper.deleteByBomId(bomId);
    }

    // ==================== 校验方法 ====================

    /**
     * 校验 BOM 主数据存在 + 明细存在且归属于该 BOM
     */
    private void validateBomAndDetail(Long bomId, Long bomDetailId) {
        bomService.validateBomExists(bomId);
        MesBomDetailDO detail = bomDetailService.validateBomDetailExists(bomDetailId);
        if (!detail.getBomId().equals(bomId)) {
            throw exception(MD_BOM_DETAIL_NOT_EXISTS);
        }
    }

    private void validateSubstituteItem(Long substituteItemId) {
        if (itemService.getItem(substituteItemId) == null) {
            throw exception(MD_ITEM_NOT_EXISTS);
        }
    }

    /**
     * 校验替代比例：非空且必须大于 0
     */
    private void validateRatio(BigDecimal substituteRatio) {
        if (substituteRatio == null || substituteRatio.signum() <= 0) {
            throw exception(BOM_SUBSTITUTE_RATIO_INVALID);
        }
    }

}