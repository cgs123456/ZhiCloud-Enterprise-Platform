# Maven 内部 license 元数据补丁复验

- 生成时间：`2026-09-27T23:59:06+08:00`

- 结果：`passed_for_internal_license_metadata`。重新生成 CycloneDX aggregate BOM 后，36 个 `cn.zhicloud` 组件全部为 MIT，内部缺失为 0。

- 修改范围：仅 `pom.xml` 和 `zhicloud-dependencies/pom.xml` 增加 `<licenses>`；未改依赖坐标和版本。

- 新 BOM SHA-256：`29427017AAADEC5E317DA685FD674D79BCD84C8A045342A6E507DDC59629D974`

- 边界：不构成法律意见；外部 `net.jcip:jcip-annotations:1.0` 仍缺 license 标签；依赖升级、CVE 复扫、法务审批和 CI 许可证门禁未完成；T-107 保持 OPEN。

## Before / After

| 指标 | Before | After |
|---|---:|---:|
| BOM SHA-256 | `AEBA1568CDD26AD9C637B4E7C69C3692408903B13207B79ED5FBF8BCB4388696` | `29427017AAADEC5E317DA685FD674D79BCD84C8A045342A6E507DDC59629D974` |
| 总组件 | 863 | 863 |
| internal 组件 | 36 | 36 |
| internal 缺 license | 36 | 0 |
| external 组件 | 827 | 827 |
| external 缺 license | 1 | 1 |

## JSON 完整性

- SHA-256：`18662853A6ED2C57A58407595D14D31F3B2FB64EF6ACAEB619EF86430C22B919`

- Sidecar：`maven-license-internal-patch-verification-20260927_2352.json.sha256`
