SET NAMES utf8mb4;
-- 全新安装端到端用的参照/配置数据（业务单据一律走 API 生成）。
-- 与 zc-mall-seed.sql 的两处差别，都是上一轮踩过之后才发现原文件是错的：
--   1) slider_pic_urls 是 JacksonTypeHandler 的 JSON 列，必须写 JSON 数组；
--      写裸 URL 时库里存得进去，但下单读取 SPU 时抛 JsonParseException → 接口 500「系统异常」。
--   2) 不再插 pay_app / pay_channel —— 那是 V135 的职责；再插一份就会出现两个 mall 应用，
--      而且原文件里那条 order_notify_url 指向 /admin-api/pay/notify/order（pay 自己的回调入口），
--      不是 trade 的 /app-api/trade/order/update-paid，V135_verify 的第 2 条检查防的正是这种写法。

INSERT INTO product_category (parent_id, name, pic_url, sort, status, creator, create_time, updater, update_time, deleted, tenant_id)
  VALUES (0, 'LiveCat', 'http://localhost/cat.png', 0, 0, '225', NOW(), '225', NOW(), b'0', 1);

INSERT INTO product_spu (name, keyword, introduction, description, category_id, pic_url, slider_pic_urls, sort,
                         status, spec_type, price, market_price, cost_price, stock, delivery_types,
                         give_integral, sub_commission_type, sales_count, virtual_sales_count, browse_count,
                         creator, create_time, updater, update_time, deleted, tenant_id)
  VALUES ('LiveSpu', 'live', 'live introduction', 'live description',
          (SELECT id FROM product_category WHERE name='LiveCat' AND tenant_id=1 LIMIT 1),
          'http://localhost/spu.png', '["http://localhost/spu.png"]', 0, 1, b'0', 19900, 29900, 10000, 100, '1,2',
          10, b'0', 0, 0, 0, '225', NOW(), '225', NOW(), b'0', 1);

INSERT INTO product_sku (spu_id, properties, price, market_price, cost_price, stock, weight, volume,
                         sales_count, creator, create_time, updater, update_time, deleted, tenant_id)
  VALUES ((SELECT id FROM product_spu WHERE name='LiveSpu' AND tenant_id=1 LIMIT 1),
          '[]', 19900, 29900, 10000, 100, 1.0, 0.01, 0, '225', NOW(), '225', NOW(), b'0', 1);

INSERT INTO trade_config (delivery_express_free_enabled, delivery_express_free_price, delivery_pick_up_enabled,
                          brokerage_enabled, brokerage_first_percent, brokerage_second_percent,
                          brokerage_withdraw_min_price, brokerage_withdraw_fee_percent, brokerage_frozen_days,
                          creator, create_time, updater, update_time, deleted, tenant_id)
  VALUES (b'0', 0, b'1', b'0', 0, 0, 10000, 10, 7, '225', NOW(), '225', NOW(), b'0', 1);

INSERT INTO trade_delivery_pick_up_store (name, introduction, phone, area_id, detail_address, logo,
                                          opening_time, closing_time, latitude, longitude, verify_user_ids, status,
                                          creator, create_time, updater, update_time, deleted, tenant_id)
  VALUES ('LiveStore', 'live pick up store', '13900002222', 1, 'Live Road 1', 'http://localhost/store.png',
          '09:00:00', '18:00:00', 30.0, 120.0, '225'  -- LongListTypeHandler 用逗号分隔，不是 JSON 数组, 0, '225', NOW(), '225', NOW(), b'0', 1);

INSERT INTO member_user (mobile, email, password, status, register_ip, register_terminal, nickname, name, sex,
                         point, experience, creator, create_time, updater, update_time, deleted, tenant_id)
  VALUES ('13900001111', 'live@example.com', 'live-not-used-hash', 0, '127.0.0.1', 20, 'LiveMember', 'Live', 1,
          0, 0, '225', NOW(), '225', NOW(), b'0', 1);

INSERT INTO member_address (user_id, name, mobile, area_id, detail_address, default_status,
                            creator, create_time, updater, update_time, deleted, tenant_id)
  VALUES ((SELECT id FROM member_user WHERE mobile='13900001111' AND tenant_id=1 LIMIT 1),
          'LiveReceiver', '13900001111', 1, 'Live Road 100', b'1', '225', NOW(), '225', NOW(), b'0', 1);
