package cn.zhicloud.module.report.service.goview;

import cn.zhicloud.framework.test.core.ut.BaseMockitoUnitTest;
import cn.zhicloud.module.report.controller.admin.goview.vo.data.GoViewDataRespVO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.report.enums.ErrorCodeConstants.GO_VIEW_SQL_NOT_READ_ONLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link GoViewDataServiceImpl} 的单元测试，聚焦「只读 SQL 守卫」。
 *
 * <p>{@code getDataBySQL} 是把调用方给的 SQL 直接交给 JdbcTemplate 的入口，
 * 所以守卫必须两个方向都验：只读查询放行、写操作/多语句/导出拒绝，
 * 而且被拒绝的 SQL 一条都不能已经落到 JdbcTemplate。
 *
 * @author 智云
 */
public class GoViewDataServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private GoViewDataServiceImpl dataService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private void mockReadOnlyResult(String... columnNames) {
        SqlRowSet rowSet = mock(SqlRowSet.class);
        SqlRowSetMetaData metaData = mock(SqlRowSetMetaData.class);
        when(metaData.getColumnNames()).thenReturn(columnNames);
        when(rowSet.getMetaData()).thenReturn(metaData);
        when(rowSet.next()).thenReturn(false);
        when(jdbcTemplate.queryForRowSet(anyString())).thenReturn(rowSet);
    }

    @Test
    public void testGetDataBySQL_selectAllowed() {
        mockReadOnlyResult("stat_date", "order_count");

        GoViewDataRespVO respVO = dataService.getDataBySQL(
                "SELECT stat_date, order_count FROM v_qms_inspection_status_daily");

        assertEquals(2, respVO.getDimensions().size());
        assertEquals("stat_date", respVO.getDimensions().get(0));
    }

    @Test
    public void testGetDataBySQL_withCteAllowed() {
        mockReadOnlyResult("c");

        GoViewDataRespVO respVO = dataService.getDataBySQL(
                "WITH t AS (SELECT 1 AS c) SELECT c FROM t");

        assertEquals(1, respVO.getDimensions().size());
    }

    @Test
    public void testGetDataBySQL_trailingSemicolonAllowed() {
        mockReadOnlyResult("c");

        dataService.getDataBySQL("SELECT 1 AS c;");

        // 末尾分号是报表里很常见的写法，不能被当成多语句拒掉
        verify(jdbcTemplate).queryForRowSet("SELECT 1 AS c;");
    }

    @Test
    public void testGetDataBySQL_dmlRejected() {
        assertServiceException(() -> dataService.getDataBySQL("UPDATE wms_inventory SET quantity = 0"),
                GO_VIEW_SQL_NOT_READ_ONLY);
        assertServiceException(() -> dataService.getDataBySQL("DROP TABLE wms_inventory"),
                GO_VIEW_SQL_NOT_READ_ONLY);
    }

    @Test
    public void testGetDataBySQL_multipleStatementsRejected() {
        // 只读开头也不能带第二条语句：JDBC 一次执行可以把 DML 捎在后面
        assertServiceException(() -> dataService.getDataBySQL("SELECT 1; DELETE FROM wms_inventory"),
                GO_VIEW_SQL_NOT_READ_ONLY);
    }

    @Test
    public void testGetDataBySQL_commentHiddenDmlRejected() {
        assertServiceException(() -> dataService.getDataBySQL("-- 只看报表\nDELETE FROM wms_inventory"),
                GO_VIEW_SQL_NOT_READ_ONLY);
        assertServiceException(() -> dataService.getDataBySQL("/* SELECT */ DROP TABLE wms_inventory"),
                GO_VIEW_SQL_NOT_READ_ONLY);
    }

    @Test
    public void testGetDataBySQL_selectIntoOutfileRejected() {
        // SELECT 开头，但会把数据写到数据库主机磁盘
        assertServiceException(() -> dataService.getDataBySQL(
                "SELECT * FROM system_users INTO OUTFILE '/tmp/dump.txt'"),
                GO_VIEW_SQL_NOT_READ_ONLY);
    }

    @Test
    public void testGetDataBySQL_nullRejected() {
        assertServiceException(() -> dataService.getDataBySQL(null), GO_VIEW_SQL_NOT_READ_ONLY);
    }

    @Test
    public void testGetDataBySQL_rejectedNeverReachesJdbc() {
        assertServiceException(() -> dataService.getDataBySQL("SELECT 1; TRUNCATE TABLE wms_inventory"),
                GO_VIEW_SQL_NOT_READ_ONLY);
        // 被拒绝的 SQL 不能已经执行过
        org.mockito.Mockito.verifyNoInteractions(jdbcTemplate);
    }

}
