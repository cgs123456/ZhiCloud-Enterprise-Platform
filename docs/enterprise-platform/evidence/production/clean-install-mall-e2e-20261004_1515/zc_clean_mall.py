"""全新安装端到端：干净库（dump 导入 + 应用自带 Flyway 建到 V135）跑 mall→pay→trade 回调。

与上一轮的差别只在"数据来源"：
  - pay_app / pay_channel 由 V135 迁移产出（creator='1'），不再是我手工建的两行；
  - mock 渠道按种子设计是**停用**的，所以先跑一次应当被业务码拒（负向对照），
    再做"运营启用渠道"这一步，流程才应该打通。
断言全部落在行上（trade_order / pay_order / pay_notify_task / pay_notify_log），
不看接口自报的 code。
"""

import json
import re
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

BASE = "http://127.0.0.1:48080"
DB = "zhicloud_sandbox_clean"
SEED_FILE = r"C:\Users\Administrator\AppData\Local\Temp\zc-mall-seed-clean.sql"


def mysql(sql_text):
    proc = subprocess.run(
        ["docker", "exec", "-i", "zc-live-mysql", "bash", "-c",
         'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 -t %s' % DB],
        input=sql_text.encode("utf-8"), capture_output=True)
    out = proc.stdout.decode("utf-8", "replace")
    if proc.returncode != 0:
        out += "SQL_ERROR " + proc.stderr.decode("utf-8", "replace")[:400]
    print(out.strip())
    return out


def rows(sql_text, want_cols):
    """从 -t 输出里取纯数字/文本混合的数据行，返回 list[list[str]]。"""
    got = []
    for line in sql_text.splitlines():
        if "|" not in line or set(line.strip()) <= set("|-+ "):
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) == want_cols and not cells[0].upper() in ("ID", "SPU_ID"):
            got.append(cells)
    return got


def call(method, path, token, body=None, params=None):
    url = BASE + path
    if params:
        url += "?" + urllib.parse.urlencode(params)
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Authorization", token)
    req.add_header("tenant-id", "1")
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            payload = resp.read().decode("utf-8")
    except urllib.error.HTTPError as ex:
        payload = ex.read().decode("utf-8", "replace")
    try:
        parsed = json.loads(payload)
    except Exception:
        print("NON_JSON %s -> %s" % (path, payload[:200]))
        return {"code": -1}
    print("%s %s -> code=%s msg=%s data=%s" % (
        method, path, parsed.get("code"), parsed.get("msg"),
        json.dumps(parsed.get("data"), ensure_ascii=False)[:180]))
    return parsed


LOOKUP = """
SELECT (SELECT id FROM product_spu WHERE name='LiveSpu' AND tenant_id=1 LIMIT 1) spu_id,
       (SELECT id FROM product_sku WHERE spu_id=(SELECT id FROM product_spu WHERE name='LiveSpu' AND tenant_id=1 LIMIT 1) LIMIT 1) sku_id,
       (SELECT id FROM member_user WHERE mobile='13900001111' AND tenant_id=1 LIMIT 1) member_id,
       (SELECT id FROM member_address WHERE user_id=(SELECT id FROM member_user WHERE mobile='13900001111' AND tenant_id=1 LIMIT 1) LIMIT 1) address_id,
       (SELECT id FROM trade_delivery_pick_up_store WHERE name='LiveStore' AND tenant_id=1 LIMIT 1) store_id;
"""


def main():
    print("########## 0. 种子行确实来自迁移，不是手工 ##########")
    out = mysql("SELECT id, app_key, status, creator, tenant_id FROM pay_app "
                "WHERE deleted=b'0' AND tenant_id=1 ORDER BY id;")
    seed_apps = rows(out, 5)
    print("pay_app(tenant 1)=", seed_apps)
    out = mysql("SELECT id, code, status, app_id, creator FROM pay_channel "
                "WHERE deleted=b'0' AND tenant_id=1 ORDER BY id;")
    seed_channels = rows(out, 5)
    print("pay_channel(tenant 1)=", seed_channels)
    if not seed_apps or seed_apps[0][3] != "1":
        print("RESULT: PRECHECK_FAILED 应用行不是迁移产出（creator!=1）")
        return 2

    print("########## 1. 参照数据（运营录入的等价物）##########")
    out = mysql(LOOKUP)
    ids = rows(out, 5)
    if not ids or not all(c.isdigit() for c in ids[0]):
        print("导入 seed …")
        with open(SEED_FILE, "rb") as f:
            out = mysql(f.read().decode("utf-8"))
        ids = rows(mysql(LOOKUP), 5)
    if not ids or not all(c.isdigit() for c in ids[0]):
        print("RESULT: SEED_FAILED")
        return 3
    spu_id, sku_id, member_id, address_id, store_id = [int(c) for c in ids[0]]
    tok = "Bearer zc-live-mock-%d" % member_id
    print("spu=%s sku=%s member=%s address=%s store=%s" % (spu_id, sku_id, member_id, address_id, store_id))

    mysql("UPDATE pay_channel SET status=1 WHERE tenant_id=1 AND code='mock' AND deleted=b'0';")
    print("  （已把 mock 渠道还原为种子默认的停用态，负向对照才有前提）")
    print("########## 2. 下单（真实路径：加购 → 取购物车项 → 用 cartId 下单，自提）##########")
    call("POST", "/app-api/trade/cart/add", tok, body={"skuId": sku_id, "count": 2})
    lst = call("GET", "/app-api/trade/cart/list", tok, params={"pageSize": 10, "pageNo": 1})
    cart_items = ((lst.get("data") or {}).get("validList") or []) if isinstance(lst.get("data"), dict) else []
    cart_id = cart_items[0]["id"] if cart_items else None
    if cart_id is None:
        print("RESULT: CART_LOOKUP_FAILED")
        return 6
    # 只有带 cartId 下单，TradeOrderUpdateServiceImpl#afterCreateTradeOrder 才会清购物车；
    # 上一版断言"购物车已清"用的是 sku 直购，那条路径本就不动购物车 —— 断言写错了，不是应用错了。
    pickup = call("POST", "/app-api/trade/order/create", tok,
                  body={"items": [{"skuId": sku_id, "count": 2, "cartId": cart_id}], "pointStatus": False,
                        "deliveryType": 2, "pickUpStoreId": store_id, "remark": "clean install pickup"})
    if pickup.get("code") != 0:
        print("RESULT: ORDER_CREATE_FAILED code=%s msg=%s" % (pickup.get("code"), pickup.get("msg")))
        return 4
    order_id = pickup["data"]["id"]
    pay_order_id = pickup["data"]["payOrderId"]

    print("########## 3. 负向对照：mock 渠道仍是种子默认的停用态，支付提交必须被拒 ##########")
    neg = call("POST", "/app-api/pay/order/submit", tok,
               body={"id": pay_order_id, "channelCode": "mock", "channelExtras": {},
                     "displayMode": None, "redirectUrl": "http://localhost/return"})
    neg_blocked = neg.get("code") not in (0, None)
    print("NEGATIVE_CONTROL blocked_code=%s msg=%s ok=%s" % (neg.get("code"), neg.get("msg"), neg_blocked))

    print("########## 4. 运营动作：启用 mock 渠道 ##########")
    mysql("UPDATE pay_channel SET status=0, updater='operator-enable' "
          "WHERE tenant_id=1 AND code='mock' AND deleted=b'0';")

    print("########## 5. 提交模拟支付 + 等异步回调 ##########")
    pos = call("POST", "/app-api/pay/order/submit", tok,
               body={"id": pay_order_id, "channelCode": "mock", "channelExtras": {},
                     "displayMode": None, "redirectUrl": "http://localhost/return"})
    for _ in range(20):
        time.sleep(1.5)
        chk = mysql(("SELECT status FROM pay_order WHERE id=%d" % pay_order_id))
        r = rows(chk, 1)
        if r and r[0][0] == "10":
            break

    print("########## 6. 行级断言 ##########")
    to = rows(mysql("SELECT id, no, status, pay_status, pay_order_id, pay_time, pay_channel_code "
                    "FROM trade_order WHERE id=%d" % order_id), 7)
    po = rows(mysql("SELECT id, app_id, channel_id, status, success_time, merchant_order_id "
                    "FROM pay_order WHERE id=%d" % pay_order_id), 6)
    nt = rows(mysql("SELECT id, app_id, type, data_id, status, notify_times FROM pay_notify_task "
                    "WHERE data_id=%d" % order_id), 6)
    nl = rows(mysql("SELECT id, task_id, status, response FROM pay_notify_log "
                    "WHERE task_id IN (SELECT id FROM pay_notify_task WHERE data_id=%d)" % order_id), 4)
    # rows() 会把表头 | COUNT(*) | 当成数据行，计数查询改走"只认纯数字行"
    cart_raw = mysql("SELECT COUNT(*) FROM trade_cart WHERE user_id=%d AND sku_id=%d AND deleted=b'0'"
                     % (member_id, sku_id))
    cart_left = [[d] for d in re.findall(r"^\|\s*(\d+)\s*\|$", cart_raw, re.M)]

    verdict = []
    verdict.append(("trade_order.status==10", bool(to) and to[0][2] == "10"))
    verdict.append(("trade_order.pay_time 落库", bool(to) and to[0][5] not in ("NULL", "")))
    verdict.append(("pay_order.status==10", bool(po) and po[0][3] == "10"))
    verdict.append(("pay_order.app_id 来自迁移行", bool(po) and po[0][1] == seed_apps[0][0]))
    verdict.append(("notify_task 已执行成功", bool(nt) and nt[0][4] == "10"))
    verdict.append(("notify_log 回执 code=0", bool(nl) and '"code":0' in nl[0][3].replace(" ", "")))
    verdict.append(("购物车已清", bool(cart_left) and cart_left[0][0] == "0"))
    verdict.append(("负向对照成立（停用渠道被拒）", neg_blocked))

    for name, ok in verdict:
        print("%-34s %s" % (name, "PASS" if ok else "FAIL"))
    print("ROWS trade_order=%s pay_order=%s notify_task=%s notify_log=%s"
          % (to, po, nt, nl))
    all_ok = all(ok for _, ok in verdict)
    print("CLEAN_INSTALL_MALL verdict=%s order_id=%s pay_order_id=%s app_id=%s channel_id=%s"
          % ("PASS" if all_ok else "FAIL", order_id, pay_order_id,
             po[0][1] if po else None, po[0][2] if po else None))
    return 0 if all_ok else 10


if __name__ == "__main__":
    sys.exit(main())
