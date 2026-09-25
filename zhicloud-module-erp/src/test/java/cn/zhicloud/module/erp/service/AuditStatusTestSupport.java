package cn.zhicloud.module.erp.service;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.module.erp.enums.ErpAuditStatus;
import cn.zhicloud.module.erp.service.sale.ErpCustomerCreditService;
import cn.zhicloud.module.erp.service.sale.ErpCustomerService;
import cn.zhicloud.module.erp.service.sale.ErpSaleOrderServiceImpl;
import cn.zhicloud.module.erp.service.stock.ErpStockRecordService;
import org.junit.jupiter.api.DynamicTest;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * ERP 审核状态入口的统一行为测试支撑。
 *
 * <p>覆盖 12 个审核入口的三条公共契约：非法状态拒绝、重复审核幂等、CAS 失败后锁读确认并发结果。</p>
 */
final class AuditStatusTestSupport {

    private static final Long DOCUMENT_ID = 1L;

    private AuditStatusTestSupport() {
    }

    static Stream<DynamicTest> tests(List<AuditCase> cases) {
        return cases.stream().flatMap(auditCase -> Stream.of(
                dynamicTest(auditCase.name() + " rejects unknown status", () -> assertUnknownStatusRejected(auditCase)),
                dynamicTest(auditCase.name() + " repeated approval is idempotent", () -> assertRepeatedApprovalIdempotent(auditCase)),
                dynamicTest(auditCase.name() + " concurrent duplicate approval uses locking read", () -> assertConcurrentDuplicateApproval(auditCase))
        ));
    }

    private static void assertUnknownStatusRejected(AuditCase auditCase) {
        Fixture fixture = createFixture(auditCase);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> invokeStatus(fixture.service(), auditCase.methodName(), 999));

        assertTrue(exception.getMessage().contains("审核状态"));
        verifyNoInteractions(fixture.mapper());
        assertSideEffectNotInvoked(fixture);
    }

    private static void assertRepeatedApprovalIdempotent(AuditCase auditCase) {
        Fixture fixture = createFixture(auditCase);
        stubSelectById(fixture.mapper(), newEntity(auditCase.entityClass(), ErpAuditStatus.APPROVE.getStatus()));

        assertDoesNotThrow(() -> invokeStatus(fixture.service(), auditCase.methodName(),
                ErpAuditStatus.APPROVE.getStatus()));

        assertTrue(invocationCount(fixture.mapper(), "updateByIdAndStatus") == 0);
        assertTrue(invocationCount(fixture.mapper(), "selectOneForUpdate") == 0);
        assertSideEffectNotInvoked(fixture);
    }

    private static void assertConcurrentDuplicateApproval(AuditCase auditCase) {
        Fixture fixture = createFixture(auditCase);
        stubSelectById(fixture.mapper(), newEntity(auditCase.entityClass(), ErpAuditStatus.PROCESS.getStatus()));
        stubSelectOneForUpdate(fixture.mapper(), newEntity(auditCase.entityClass(), ErpAuditStatus.APPROVE.getStatus()));
        stubUpdateByIdAndStatus(auditCase, fixture.mapper(), ErpAuditStatus.PROCESS.getStatus());

        assertDoesNotThrow(() -> invokeStatus(fixture.service(), auditCase.methodName(),
                ErpAuditStatus.APPROVE.getStatus()));

        assertTrue(invocationCount(fixture.mapper(), "selectOneForUpdate") == 1);
        assertTrue(invocationCount(fixture.mapper(), "updateByIdAndStatus") == 1);
        assertSideEffectNotInvoked(fixture);
    }

    private static Fixture createFixture(AuditCase auditCase) {
        try {
            Object service = auditCase.serviceClass().getDeclaredConstructor().newInstance();
            Object mapper = Mockito.mock(auditCase.mapperClass());
            ReflectionTestUtils.setField(service, auditCase.mapperField(), mapper);
            if (service instanceof ErpSaleOrderServiceImpl) {
                ReflectionTestUtils.setField(service, "customerService", Mockito.mock(ErpCustomerService.class));
                ReflectionTestUtils.setField(service, "customerCreditService", Mockito.mock(ErpCustomerCreditService.class));
            }
            Object sideEffect = null;
            if (auditCase.sideEffectField() != null) {
                sideEffect = Mockito.mock(ErpStockRecordService.class);
                ReflectionTestUtils.setField(service, auditCase.sideEffectField(), sideEffect);
            }
            return new Fixture(service, mapper, sideEffect);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("创建审核入口测试夹具失败: " + auditCase.name(), exception);
        }
    }

    private static Object newEntity(Class<?> entityClass, Integer status) {
        try {
            Object entity = entityClass.getDeclaredConstructor().newInstance();
            entityClass.getMethod("setStatus", Integer.class).invoke(entity, status);
            return entity;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("创建审核实体失败: " + entityClass.getName(), exception);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void stubSelectById(Object mapper, Object entity) {
        when(((BaseMapperX) mapper).selectById(DOCUMENT_ID)).thenReturn(entity);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void stubSelectOneForUpdate(Object mapper, Object entity) {
        when(((BaseMapperX) mapper).selectOneForUpdate(any(), eq(DOCUMENT_ID))).thenReturn(entity);
    }

    private static void stubUpdateByIdAndStatus(AuditCase auditCase, Object mapper, Integer expectedStatus) {
        try {
            Method method = auditCase.mapperClass().getMethod("updateByIdAndStatus",
                    Long.class, Integer.class, auditCase.entityClass());
            when(method.invoke(mapper, eq(DOCUMENT_ID), eq(expectedStatus), any(auditCase.entityClass())))
                    .thenReturn(0);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("设置 CAS 更新桩失败: " + auditCase.name(), exception);
        }
    }

    private static void invokeStatus(Object service, String methodName, Integer status) {
        try {
            Method method = service.getClass().getMethod(methodName, Long.class, Integer.class);
            method.invoke(service, DOCUMENT_ID, status);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new AssertionError(cause);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("调用审核入口失败: " + methodName, exception);
        }
    }

    private static int invocationCount(Object mock, String methodName) {
        return (int) Mockito.mockingDetails(mock).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals(methodName))
                .count();
    }

    private static void assertSideEffectNotInvoked(Fixture fixture) {
        if (fixture.sideEffect() != null) {
            verifyNoInteractions(fixture.sideEffect());
        }
    }

    record AuditCase(String name,
                     Class<?> serviceClass,
                     Class<?> mapperClass,
                     String mapperField,
                     String methodName,
                     Class<?> entityClass,
                     String sideEffectField) {
    }

    private record Fixture(Object service, Object mapper, Object sideEffect) {
    }
}
