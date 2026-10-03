package cn.zhicloud.module.report.service.goview;

import cn.zhicloud.module.report.controller.admin.goview.vo.data.GoViewDataRespVO;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.report.enums.ErrorCodeConstants.GO_VIEW_SQL_NOT_READ_ONLY;
import com.google.common.collect.Maps;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;

/**
 * GoView 数据 Service 实现类
 *
 * 补充说明：
 * 1. 目前默认使用 jdbcTemplate 查询项目配置的数据源。如果你想查询其它数据源，可以新建对应数据源的 jdbcTemplate 来实现。
 * 2. 默认数据源是 MySQL 关系数据源，可能数据量比较大的情况下，会比较慢，可以考虑后续使用 Click House 等等。
 *
 * @author 智云
 */
@Service
@Validated
public class GoViewDataServiceImpl implements GoViewDataService {

    /** 行注释：-- 到行尾 */
    private static final Pattern LINE_COMMENT = Pattern.compile("(?m)--[^\n]*");
    /** 块注释：C-style */
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*[\\s\\S]*?\\*/");
    /** 必须以 SELECT 或 WITH（只读 CTE）开头 */
    private static final Pattern SELECT_PREFIX = Pattern.compile("(?i)^(SELECT|WITH)\\b");
    /** 禁止 SELECT ... INTO（含 OUTFILE / DUMPFILE） */
    private static final Pattern INTO = Pattern.compile("(?i)\\bINTO\\b");

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public GoViewDataRespVO getDataBySQL(String sql) {
        // 0. 只读校验：本方法是「把调用方给的 SQL 直接交给 jdbcTemplate」的入口，
        //    不加守卫就等于给管理端一个任意 SQL 执行器（改数据、多语句、INTO OUTFILE 落盘）。
        validateReadOnlySql(sql);
        // 1. 执行查询
        SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet(sql);

        // 2. 构建返回结果
        GoViewDataRespVO respVO = new GoViewDataRespVO();
        // 2.1 解析元数据
        SqlRowSetMetaData metaData = sqlRowSet.getMetaData();
        String[] columnNames = metaData.getColumnNames();
        respVO.setDimensions(Arrays.asList(columnNames));
        // 2.2 解析数据明细
        respVO.setSource(new LinkedList<>()); // 由于数据量不确认，使用 LinkedList 虽然内存占用大一点，但是不存在扩容复制的问题
        while (sqlRowSet.next()) {
            Map<String, Object> data = Maps.newHashMapWithExpectedSize(columnNames.length);
            for (String columnName : columnNames) {
                data.put(columnName, sqlRowSet.getObject(columnName));
            }
            respVO.getSource().add(data);
        }
        return respVO;
    }

    /**
     * 只读 SQL 校验：先去注释再判断，避免 "-- 说明
DROP TABLE x" 这种绕法。
     *
     * @param sql 调用方提交的 SQL
     */
    static void validateReadOnlySql(String sql) {
        if (sql == null) {
            throw exception(GO_VIEW_SQL_NOT_READ_ONLY);
        }
        String stripped = LINE_COMMENT.matcher(BLOCK_COMMENT.matcher(sql).replaceAll(" ")).replaceAll(" ").trim();
        // 末尾分号允许；中间出现分号就是多条语句，JDBC 一次执行可以携带 DML
        String body = stripped.endsWith(";") ? stripped.substring(0, stripped.length() - 1).trim() : stripped;
        if (body.indexOf(';') >= 0 || !SELECT_PREFIX.matcher(body).find()) {
            throw exception(GO_VIEW_SQL_NOT_READ_ONLY);
        }
        // SELECT ... INTO（含 OUTFILE / DUMPFILE）会把数据写到数据库所在主机的磁盘
        if (INTO.matcher(body).find()) {
            throw exception(GO_VIEW_SQL_NOT_READ_ONLY);
        }
    }

}
