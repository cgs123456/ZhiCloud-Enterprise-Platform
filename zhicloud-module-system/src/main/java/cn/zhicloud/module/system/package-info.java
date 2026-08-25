/**
 * System 模块：系统管理（用户/角色/权限/部门/字典/租户/OAuth2/SMS/Mail/Notify 等）
 *
 * <p>Spring Modulith 模块声明（A3）：通过 package-info.java 显式声明模块边界，
 * 配合 {@code spring.modulith.detection-strategy=explicitly-annotated} 启用模块边界校验。
 *
 * <p>模块对外暴露的 API（其它模块只能依赖本模块的 {@code api} 子包）：
 * <ul>
 *   <li>{@code cn.zhicloud.module.system.api.*} — 内部 API 接口与 DTO</li>
 * </ul>
 *
 * <p>注：@ApplicationModule 注解需在 zhicloud-server 集成测试中通过 ModulithVerificationTest 校验。
 * 模块依赖图：system ← infra ← erp/mes/wms/crm/qms ← ai ← airag/aimultiagent
 *
 * @author zhicloud
 */
@org.springframework.modulith.ApplicationModule(displayName = "System 系统管理模块")
package cn.zhicloud.module.system;
