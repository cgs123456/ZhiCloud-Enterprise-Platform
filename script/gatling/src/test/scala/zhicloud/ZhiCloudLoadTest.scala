package zhicloud

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.util.Random

/**
 * 智云 zhicloud 性能压测脚本（Gatling）
 *
 * 覆盖五大场景：
 *   1. 登录场景（100 并发）
 *   2. 用户列表查询（200 并发，Ramp-up 30s）
 *   3. 字典数据查询（300 并发）
 *   4. AI 对话（50 并发）
 *   5. ERP 产品列表（100 并发）
 *
 * 业务端口：48080
 * 上线判定：P99 < 1s，成功率 > 99.5%
 *
 * 运行：
 *   mvn gatling:test -Dgatling.simulationClass=zhicloud.ZhiCloudLoadTest
 */
class ZhiCloudLoadTest extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:48080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .header("tenant-id", "1")
    .userAgentHeader("Gatling/zhicloud-load-test")
    .disableWarmUp
    .perUserResolutionTimeout(10000)
    .connectTimeout(10000)
    .readTimeout(30000)

  // ============================================================
  // Feeder：账号数据
  // ============================================================
  val userFeeder = Iterator.continually(
    Map(
      "username" -> "admin",
      "password" -> "admin123",
      "captchaVerification" -> ""
    )
  )

  // ============================================================
  // 场景一：登录场景
  // 提取 accessToken 供后续场景使用
  // ============================================================
  val loginScenario = scenario("登录场景")
    .feed(userFeeder)
    .exec(
      http("POST /admin-api/system/auth/login")
        .post("/admin-api/system/auth/login")
        .body(StringBody("""{"username":"#{username}","password":"#{password}","captchaVerification":""}"""))
        .asJson
        .check(status.is(200))
        .check(jsonPath("$.code").ofType[Int].is(0))
        .check(jsonPath("$.data.accessToken").saveAs("accessToken"))
    )
    .exec { session =>
      // 将 token 写入全局属性，便于跨场景共享
      val tokenOpt = session("accessToken").asOption[String]
      tokenOpt.foreach { token =>
        session.set("accessToken", token)
      }
      session
    }

  // ============================================================
  // 场景二：用户列表查询
  // ============================================================
  val userQueryScenario = scenario("用户列表查询")
    .exec(
      http("POST /admin-api/system/auth/login (获取Token)")
        .post("/admin-api/system/auth/login")
        .body(StringBody("""{"username":"admin","password":"admin123","captchaVerification":""}"""))
        .asJson
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").saveAs("accessToken"))
    )
    .exec(
      http("GET /admin-api/system/user/page")
        .get("/admin-api/system/user/page?pageNo=1&pageSize=10")
        .header("Authorization", "Bearer #{accessToken}")
        .check(status.is(200))
        .check(jsonPath("$.code").ofType[Int].is(0))
    )

  // ============================================================
  // 场景三：字典数据查询
  // ============================================================
  val dictQueryScenario = scenario("字典数据查询")
    .exec(
      http("POST /admin-api/system/auth/login (获取Token)")
        .post("/admin-api/system/auth/login")
        .body(StringBody("""{"username":"admin","password":"admin123","captchaVerification":""}"""))
        .asJson
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").saveAs("accessToken"))
    )
    .exec(
      http("GET /admin-api/system/dict-type/list-all-simple")
        .get("/admin-api/system/dict-type/list-all-simple")
        .header("Authorization", "Bearer #{accessToken}")
        .check(status.is(200))
        .check(jsonPath("$.code").ofType[Int].is(0))
    )

  // ============================================================
  // 场景四：AI 对话
  // ============================================================
  val aiChatScenario = scenario("AI对话")
    .exec(
      http("POST /admin-api/system/auth/login (获取Token)")
        .post("/admin-api/system/auth/login")
        .body(StringBody("""{"username":"admin","password":"admin123","captchaVerification":""}"""))
        .asJson
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").saveAs("accessToken"))
    )
    .exec(
      http("POST /admin-api/ai/chat/conversation/send-message")
        .post("/admin-api/ai/chat/conversation/send-message")
        .header("Authorization", "Bearer #{accessToken}")
        .body(StringBody("""{"conversationId":1,"content":"你好"}"""))
        .asJson
        .check(status.is(200))
    )

  // ============================================================
  // 场景五：ERP 产品列表
  // ============================================================
  val erpProductScenario = scenario("ERP产品列表")
    .exec(
      http("POST /admin-api/system/auth/login (获取Token)")
        .post("/admin-api/system/auth/login")
        .body(StringBody("""{"username":"admin","password":"admin123","captchaVerification":""}"""))
        .asJson
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").saveAs("accessToken"))
    )
    .exec(
      http("GET /admin-api/erp/product/product/page")
        .get("/admin-api/erp/product/product/page?pageNo=1&pageSize=10")
        .header("Authorization", "Bearer #{accessToken}")
        .check(status.is(200))
        .check(jsonPath("$.code").ofType[Int].is(0))
    )

  // ============================================================
  // setUp：注入策略与断言
  // ============================================================
  setUp(
    loginScenario.inject(
      rampUsers(100).during(30.seconds)
    ),
    userQueryScenario.inject(
      rampUsers(200).during(30.seconds)
    ),
    dictQueryScenario.inject(
      rampUsers(300).during(30.seconds)
    ),
    aiChatScenario.inject(
      rampUsers(50).during(30.seconds)
    ),
    erpProductScenario.inject(
      rampUsers(100).during(30.seconds)
    )
  ).protocols(httpProtocol)
    .maxDuration(300.seconds)
    .assertions(
      // P99 响应时间 < 1s
      global.responseTime.max.lt(1000),
      // 成功率 > 99.5%
      global.successfulRequests.percent.gt(99.5),
      // 每个场景均需通过
      forAll.responseTime.percentile3.lt(1000),
      forAll.successfulRequests.percent.gt(99.0)
    )
}
