-- ============================================================
-- V77 半失败恢复脚本（仅限 MySQL 8）
--
-- 背景：V77__aimultiagent_resilience.sql 使用裸 ALTER TABLE ADD COLUMN。
-- 如果迁移在“第一列已加、第二列未加”时失败，Flyway 会把 V77 记为 success=0，
-- 后续启动会被失败的迁移历史阻断；直接重跑又会撞 Duplicate column。
--
-- 本脚本不修改 V77 历史正文（避免已成功库 checksum 漂移），而是：
--   1. 对 V77 的两列做 information_schema 幂等补齐；
--   2. 仅当 flyway_schema_history 中存在精确的 V77 失败行时，
--      将该行 success 置 1，并写入 V77 的已发布 checksum；
--   3. 对成功行、缺失行、重复失败行和结构不一致 fail-closed。
--
-- 使用前请先备份目标库。该脚本不会 DROP 表、DELETE 业务数据或改写其他迁移行。
-- 必须使用 mysql 客户端执行（DELIMITER 是客户端指令）；不要直接交给 JDBC 执行。
-- ============================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS zhicloud_repair_v77$$
CREATE PROCEDURE zhicloud_repair_v77()
BEGIN
    DECLARE v_topology_exists INT DEFAULT 0;
    DECLARE v_execution_log_exists INT DEFAULT 0;
    DECLARE v_version_exists INT DEFAULT 0;
    DECLARE v_trace_id_exists INT DEFAULT 0;
    DECLARE v_history_exists INT DEFAULT 0;
    DECLARE v_v77_rows INT DEFAULT 0;
    DECLARE v_v77_failed_rows INT DEFAULT 0;
    DECLARE v_v77_success_rows INT DEFAULT 0;

    SELECT COUNT(*) INTO v_topology_exists
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'aimultiagent_topology';

    SELECT COUNT(*) INTO v_execution_log_exists
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'aimultiagent_execution_log';

    SELECT COUNT(*) INTO v_history_exists
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'flyway_schema_history';

    IF v_history_exists = 0 THEN
        SELECT 'V77_REPAIR_NOOP_NO_FLYWAY_HISTORY' AS result;
    ELSE
        SELECT COUNT(*) INTO v_v77_rows
          FROM flyway_schema_history
         WHERE version = '77'
           AND script = 'V77__aimultiagent_resilience.sql';

        SELECT COUNT(*) INTO v_v77_failed_rows
          FROM flyway_schema_history
         WHERE version = '77'
           AND script = 'V77__aimultiagent_resilience.sql'
           AND success = 0;

        SELECT COUNT(*) INTO v_v77_success_rows
          FROM flyway_schema_history
         WHERE version = '77'
           AND script = 'V77__aimultiagent_resilience.sql'
           AND success = 1;

        IF v_v77_rows > 1 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'V77 repair refused: duplicate V77 history rows';
        END IF;

        IF v_v77_rows = 0 THEN
            IF v_topology_exists = 1 THEN
                SELECT COUNT(*) INTO v_version_exists
                  FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name = 'aimultiagent_topology'
                   AND column_name = 'version';
                IF v_version_exists > 0 THEN
                    SIGNAL SQLSTATE '45000'
                        SET MESSAGE_TEXT = 'V77 repair refused: columns exist without V77 history';
                END IF;
            END IF;

            IF v_execution_log_exists = 1 THEN
                SELECT COUNT(*) INTO v_trace_id_exists
                  FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name = 'aimultiagent_execution_log'
                   AND column_name = 'trace_id';
                IF v_trace_id_exists > 0 THEN
                    SIGNAL SQLSTATE '45000'
                        SET MESSAGE_TEXT = 'V77 repair refused: columns exist without V77 history';
                END IF;
            END IF;

            SELECT 'V77_REPAIR_NOOP_NO_V77_HISTORY' AS result;
        ELSEIF v_v77_failed_rows = 1 THEN
            IF v_topology_exists = 0 OR v_execution_log_exists = 0 THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'V77 repair refused: target table missing';
            END IF;

            SELECT COUNT(*) INTO v_version_exists
              FROM information_schema.columns
             WHERE table_schema = DATABASE()
               AND table_name = 'aimultiagent_topology'
               AND column_name = 'version';

            IF v_version_exists = 0 THEN
                ALTER TABLE `aimultiagent_topology`
                    ADD COLUMN `version` VARCHAR(32) NULL COMMENT '配置 schema 版本（如 v1）' AFTER `status`;
            ELSEIF v_version_exists > 1 THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'V77 repair refused: duplicate version columns';
            END IF;

            SELECT COUNT(*) INTO v_trace_id_exists
              FROM information_schema.columns
             WHERE table_schema = DATABASE()
               AND table_name = 'aimultiagent_execution_log'
               AND column_name = 'trace_id';

            IF v_trace_id_exists = 0 THEN
                ALTER TABLE `aimultiagent_execution_log`
                    ADD COLUMN `trace_id` VARCHAR(64) NULL COMMENT '全链路追踪 ID（跨 Worker / Supervisor 排查）' AFTER `duration_ms`;
            ELSEIF v_trace_id_exists > 1 THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'V77 repair refused: duplicate trace_id columns';
            END IF;

            UPDATE flyway_schema_history
               SET success = 1,
                   checksum = -282762132
             WHERE version = '77'
               AND script = 'V77__aimultiagent_resilience.sql'
               AND success = 0;

            IF ROW_COUNT() <> 1 THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'V77 repair refused: failed history row not updated exactly once';
            END IF;

            SELECT 'V77_REPAIR_APPLIED' AS result;
        ELSEIF v_v77_success_rows = 1 THEN
            IF v_topology_exists = 0 OR v_execution_log_exists = 0 THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'V77 repair refused: successful history but target table missing';
            END IF;

            SELECT COUNT(*) INTO v_version_exists
              FROM information_schema.columns
             WHERE table_schema = DATABASE()
               AND table_name = 'aimultiagent_topology'
               AND column_name = 'version';

            SELECT COUNT(*) INTO v_trace_id_exists
              FROM information_schema.columns
             WHERE table_schema = DATABASE()
               AND table_name = 'aimultiagent_execution_log'
               AND column_name = 'trace_id';

            IF v_version_exists <> 1 OR v_trace_id_exists <> 1 THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'V77 repair refused: successful history but column missing';
            END IF;

            SELECT 'V77_REPAIR_ALREADY_CONSISTENT' AS result;
        ELSE
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'V77 repair refused: inconsistent V77 history state';
        END IF;
    END IF;
END$$

CALL zhicloud_repair_v77()$$
DROP PROCEDURE IF EXISTS zhicloud_repair_v77$$
DELIMITER ;
