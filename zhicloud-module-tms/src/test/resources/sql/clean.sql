-- 每个单元测试结束后，清理 DB（与 create_tables.sql 的表保持一致）

DELETE FROM "tms_carrier";
DELETE FROM "tms_driver";
DELETE FROM "tms_fleet_operation";
DELETE FROM "tms_freight";
DELETE FROM "tms_freight_reconciliation";
DELETE FROM "tms_gps_position";
DELETE FROM "tms_shipment";
DELETE FROM "tms_shipment_stop";
DELETE FROM "tms_tracking_event";
DELETE FROM "tms_vehicle";
