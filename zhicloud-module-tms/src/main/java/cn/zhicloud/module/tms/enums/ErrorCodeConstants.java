package cn.zhicloud.module.tms.enums;

import cn.zhicloud.framework.common.exception.ErrorCode;

/**
 * TMS 错误码枚举类
 * <p>
 * TMS 系统，使用 1-060-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== TMS 承运商（1-060-000-000） ==========
    ErrorCode TMS_CARRIER_NOT_EXISTS = new ErrorCode(1_047_000_000, "承运商不存在");
    ErrorCode TMS_CARRIER_CODE_DUPLICATE = new ErrorCode(1_047_000_001, "承运商编码已存在");
    ErrorCode TMS_CARRIER_NOT_ENABLE = new ErrorCode(1_047_000_002, "承运商({})未启用");

    // ========== TMS 车辆（1-060-001-000） ==========
    ErrorCode TMS_VEHICLE_NOT_EXISTS = new ErrorCode(1_047_000_003, "车辆不存在");
    ErrorCode TMS_VEHICLE_PLATE_NO_DUPLICATE = new ErrorCode(1_047_000_004, "车牌号已存在");
    ErrorCode TMS_VEHICLE_NOT_AVAILABLE = new ErrorCode(1_047_000_005, "车辆({})非可用状态，无法调度");

    // ========== TMS 司机（1-060-002-000） ==========
    ErrorCode TMS_DRIVER_NOT_EXISTS = new ErrorCode(1_047_000_006, "司机不存在");
    ErrorCode TMS_DRIVER_LICENSE_NO_DUPLICATE = new ErrorCode(1_047_000_007, "驾照号已存在");
    ErrorCode TMS_DRIVER_NOT_AVAILABLE = new ErrorCode(1_047_000_008, "司机({})非可用状态，无法调度");

    // ========== TMS 运单（1-060-003-000） ==========
    ErrorCode TMS_SHIPMENT_NOT_EXISTS = new ErrorCode(1_047_000_009, "运单不存在");
    ErrorCode TMS_SHIPMENT_NO_EXISTS = new ErrorCode(1_047_000_010, "生成运单号失败，请重新提交");
    ErrorCode TMS_SHIPMENT_STATUS_INVALID = new ErrorCode(1_047_000_011, "运单({})状态不支持当前操作");
    ErrorCode TMS_SHIPMENT_DISPATCH_FAIL = new ErrorCode(1_047_000_012, "运单({})调度失败，无可用车辆或司机");
    ErrorCode TMS_SHIPMENT_DISPATCH_FAIL_VEHICLE = new ErrorCode(1_060_003_004, "运单({})调度失败，车辆不可用");
    ErrorCode TMS_SHIPMENT_DISPATCH_FAIL_DRIVER = new ErrorCode(1_060_003_005, "运单({})调度失败，司机不可用");

    // ========== TMS 跟踪事件（1-060-004-000） ==========
    ErrorCode TMS_TRACKING_EVENT_NOT_EXISTS = new ErrorCode(1_047_000_013, "跟踪事件不存在");

    // ========== TMS 运费结算（1-060-005-000） ==========
    ErrorCode TMS_FREIGHT_NOT_EXISTS = new ErrorCode(1_047_000_014, "运费结算单不存在");
    ErrorCode TMS_FREIGHT_NO_DUPLICATE = new ErrorCode(1_060_005_001, "结算单号已存在");
    ErrorCode TMS_FREIGHT_STATUS_INVALID = new ErrorCode(1_060_005_002, "结算单({})状态不支持当前操作");
    ErrorCode TMS_FREIGHT_CALC_FAIL = new ErrorCode(1_060_005_003, "运单({})运费计算失败：{}");
    ErrorCode TMS_FREIGHT_SHIPMENT_NOT_SIGNED = new ErrorCode(1_060_005_004, "运单({})未签收，无法结算");

    // ========== TMS GPS 定位（1-060-006-000） ==========
    ErrorCode TMS_GPS_POSITION_NOT_EXISTS = new ErrorCode(1_047_000_015, "GPS定位记录不存在");
    ErrorCode TMS_GPS_VEHICLE_NOT_TRACKING = new ErrorCode(1_047_000_016, "车辆({})未开启GPS追踪");

    // ========== TMS 运费对账（1-060-007-000） ==========
    ErrorCode TMS_FREIGHT_RECONCILIATION_NOT_EXISTS = new ErrorCode(1_047_000_017, "运费对账单不存在");
    ErrorCode TMS_FREIGHT_RECONCILIATION_NO_DUPLICATE = new ErrorCode(1_047_000_018, "对账单号已存在");
    ErrorCode TMS_FREIGHT_RECONCILIATION_NOT_RECONCILED = new ErrorCode(1_047_000_019, "对账单尚未执行对账，无法确认");

    // ========== TMS 车队运营（1-060-008-000） ==========
    ErrorCode TMS_FLEET_OPERATION_NOT_EXISTS = new ErrorCode(1_047_000_020, "车队运营记录不存在");

}
