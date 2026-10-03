-- ======================== 商城与支付建表脚本（pay / product / promotion / trade / statistics） ========================
-- 作者：zhicloud
-- 说明：由各模块 DO 类字段生成（DO 是事实来源，模块内 H2 测试建表脚本不作为依据）。
--   1) 每张表都带 tenant_id：zhicloud.tenant.ignore-tables 为空，租户拦截器会给所有表追加 tenant_id 条件。
--   2) 主键 AUTO_INCREMENT：id-type: NONE 对 MySQL 解析为 AUTO，插入时不传 id。
--      pay_order / pay_channel / pay_notify_log / pay_order_extension / promotion_banner /
--      trade_after_sale / trade_cart / trade_order / trade_order_item 这 9 张表的 DO 根本没写 @TableId，
--      靠 MyBatis-Plus「字段名 id 即主键」的默认约定；只认 @TableId 的第一版生成脚本把它们建成了
--      没有主键也没有自增的表，是 V123_verify 的 auto_increment 覆盖数（FAIL 54/63）把它们抓出来的。
-- 已在隔离 MySQL 8.4.11 容器实跑。

-- ======================== pay DDL ========================
-- 作者：zhicloud
-- 说明：由 DO 类字段生成，主键自增（MySQL id-type=AUTO），基础字段对齐 BaseDO/TenantBaseDO

-- ----------------------------
-- 支付应用 DO (PayAppDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS pay_app (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '应用编号，数据库自增',
    PRIMARY KEY (id),
    app_key VARCHAR(64) COMMENT '应用标识',
    name VARCHAR(128) COMMENT '应用名',
    status INT COMMENT '状态',
    remark VARCHAR(512) COMMENT '备注',
    order_notify_url VARCHAR(512) COMMENT '支付结果的回调地址',
    refund_notify_url VARCHAR(512) COMMENT '退款结果的回调地址',
    transfer_notify_url VARCHAR(512) COMMENT '转账结果的回调地址',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='支付应用 DO';

-- ----------------------------
-- 支付渠道 DO (PayChannelDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS pay_channel (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '渠道编号，数据库自增',
    PRIMARY KEY (id),
    code VARCHAR(64) COMMENT '渠道编码',
    status INT COMMENT '状态',
    fee_rate DOUBLE COMMENT '渠道费率，单位：百分比',
    remark VARCHAR(512) COMMENT '备注',
    app_id BIGINT COMMENT '应用编号',
    config TEXT COMMENT '支付渠道配置',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='支付渠道 DO';

-- ----------------------------
-- 示例订单 (PayDemoOrderDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS pay_demo_order (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单编号，自增',
    PRIMARY KEY (id),
    user_id BIGINT COMMENT '用户编号',
    spu_id BIGINT COMMENT '商品编号',
    spu_name VARCHAR(255) COMMENT '商品名称',
    price INT COMMENT '价格，单位：分',
    pay_status BIT(1) COMMENT '是否支付',
    pay_order_id BIGINT COMMENT '支付订单编号',
    pay_time DATETIME COMMENT '付款时间',
    pay_channel_code VARCHAR(64) COMMENT '支付渠道',
    pay_refund_id BIGINT COMMENT '支付退款单号',
    refund_price INT COMMENT '退款金额，单位：分',
    refund_time DATETIME COMMENT '退款完成时间',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='示例订单';

-- ----------------------------
-- 示例提现订单 (PayDemoWithdrawDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS pay_demo_withdraw (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '提现单编号，自增',
    PRIMARY KEY (id),
    subject VARCHAR(128) COMMENT '提现标题',
    price INT COMMENT '提现金额，单位：分',
    user_account VARCHAR(64) COMMENT '收款人账号',
    user_name VARCHAR(255) COMMENT '收款人姓名',
    type INT COMMENT '提现方式',
    status INT COMMENT '提现状态',
    pay_transfer_id BIGINT COMMENT '转账单编号',
    transfer_channel_code VARCHAR(64) COMMENT '转账渠道',
    transfer_time DATETIME COMMENT '转账成功时间',
    transfer_error_msg VARCHAR(512) COMMENT '转账错误提示',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='示例提现订单';

-- ----------------------------
-- 商户支付、退款等的通知 Log (PayNotifyLogDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS pay_notify_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志编号，自增',
    PRIMARY KEY (id),
    task_id BIGINT COMMENT '通知任务编号',
    notify_times INT COMMENT '第几次被通知',
    response VARCHAR(255) COMMENT 'HTTP 响应结果',
    status INT COMMENT '支付通知状态',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='商户支付、退款等的通知 Log';

-- ----------------------------
-- 支付通知 (PayNotifyTaskDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS pay_notify_task (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号，自增',
    PRIMARY KEY (id),
    app_id BIGINT COMMENT '应用编号',
    type INT COMMENT '通知类型',
    data_id BIGINT COMMENT '数据编号，根据不同 type 进行关联：',
    merchant_order_id VARCHAR(255) COMMENT '商户订单编号',
    merchant_refund_id VARCHAR(255) COMMENT '商户退款编号',
    merchant_transfer_id VARCHAR(255) COMMENT '商户转账编号',
    status INT COMMENT '通知状态',
    next_notify_time DATETIME COMMENT '下一次通知时间',
    last_execute_time DATETIME COMMENT '最后一次执行时间',
    notify_times INT COMMENT '当前通知次数',
    max_notify_times INT COMMENT '最大可通知次数',
    notify_url VARCHAR(512) COMMENT '通知地址',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='支付通知';

-- ----------------------------
-- 支付订单 DO (PayOrderDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS pay_order (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单编号，数据库自增',
    PRIMARY KEY (id),
    app_id BIGINT COMMENT '应用编号',
    channel_id BIGINT COMMENT '渠道编号',
    channel_code VARCHAR(64) COMMENT '渠道编码',
    user_id BIGINT COMMENT '用户编号',
    user_type INT COMMENT '用户类型',
    merchant_order_id VARCHAR(255) COMMENT '商户订单编号',
    subject VARCHAR(128) COMMENT '商品标题',
    body VARCHAR(255) COMMENT '商品描述信息',
    notify_url VARCHAR(512) COMMENT '异步通知地址',
    price INT COMMENT '支付金额，单位：分',
    channel_fee_rate DOUBLE COMMENT '渠道手续费，单位：百分比',
    channel_fee_price INT COMMENT '渠道手续金额，单位：分',
    status INT COMMENT '支付状态',
    user_ip VARCHAR(255) COMMENT '用户 IP',
    expire_time DATETIME COMMENT '订单失效时间',
    success_time DATETIME COMMENT '订单支付成功时间',
    extension_id BIGINT COMMENT '支付成功的订单拓展单编号',
    no VARCHAR(64) COMMENT '支付成功的外部订单号',
    refund_price INT COMMENT '退款总金额，单位：分',
    channel_user_id VARCHAR(255) COMMENT '渠道用户编号',
    channel_order_no VARCHAR(64) COMMENT '渠道订单号',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='支付订单 DO';

-- ----------------------------
-- 支付订单拓展 DO (PayOrderExtensionDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS pay_order_extension (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单拓展编号，数据库自增',
    PRIMARY KEY (id),
    no VARCHAR(64) COMMENT '外部订单号，根据规则生成',
    order_id BIGINT COMMENT '订单号',
    channel_id BIGINT COMMENT '渠道编号',
    channel_code VARCHAR(64) COMMENT '渠道编码',
    user_ip VARCHAR(255) COMMENT '用户 IP',
    status INT COMMENT '支付状态',
    channel_extras TEXT COMMENT '支付渠道的额外参数',
    channel_error_code VARCHAR(64) COMMENT '调用渠道的错误码',
    channel_error_msg VARCHAR(512) COMMENT '调用渠道报错时，错误信息',
    channel_notify_data TEXT COMMENT '支付渠道的同步/异步通知的内容',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='支付订单拓展 DO';

-- ----------------------------
-- 支付退款单 DO (PayRefundDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS pay_refund (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '退款单编号，数据库自增',
    PRIMARY KEY (id),
    no VARCHAR(64) COMMENT '外部退款号，根据规则生成',
    app_id BIGINT COMMENT '应用编号',
    channel_id BIGINT COMMENT '渠道编号',
    channel_code VARCHAR(64) COMMENT '渠道编码',
    order_id BIGINT COMMENT '订单编号',
    order_no VARCHAR(64) COMMENT '支付订单编号',
    user_id BIGINT COMMENT '用户编号',
    user_type INT COMMENT '用户类型',
    merchant_order_id VARCHAR(255) COMMENT '商户订单编号',
    merchant_refund_id VARCHAR(255) COMMENT '商户退款订单号',
    notify_url VARCHAR(512) COMMENT '异步通知地址',
    status INT COMMENT '退款状态',
    pay_price INT COMMENT '支付金额，单位：分',
    refund_price INT COMMENT '退款金额，单位：分',
    reason VARCHAR(512) COMMENT '退款原因',
    user_ip VARCHAR(255) COMMENT '用户 IP',
    channel_order_no VARCHAR(64) COMMENT '渠道订单号',
    channel_refund_no VARCHAR(64) COMMENT '渠道退款单号',
    success_time DATETIME COMMENT '退款成功时间',
    channel_error_code VARCHAR(64) COMMENT '调用渠道的错误码',
    channel_error_msg VARCHAR(512) COMMENT '调用渠道的错误提示',
    channel_notify_data TEXT COMMENT '支付渠道的同步/异步通知的内容',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='支付退款单 DO';

-- ----------------------------
-- 转账单 DO (PayTransferDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS pay_transfer (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    no VARCHAR(64) COMMENT '转账单号',
    app_id BIGINT COMMENT '应用编号',
    channel_id BIGINT COMMENT '转账渠道编号',
    channel_code VARCHAR(64) COMMENT '转账渠道编码',
    user_id BIGINT COMMENT '用户编号',
    user_type INT COMMENT '用户类型',
    merchant_transfer_id VARCHAR(255) COMMENT '商户转账单编号',
    subject VARCHAR(128) COMMENT '转账标题',
    price INT COMMENT '转账金额，单位：分',
    user_account VARCHAR(64) COMMENT '收款人账号',
    user_name VARCHAR(255) COMMENT '收款人姓名',
    status INT COMMENT '转账状态',
    success_time DATETIME COMMENT '订单转账成功时间',
    notify_url VARCHAR(512) COMMENT '异步通知地址',
    user_ip VARCHAR(255) COMMENT '用户 IP',
    channel_extras TEXT COMMENT '渠道的额外参数',
    channel_transfer_no VARCHAR(64) COMMENT '渠道转账单号',
    channel_error_code VARCHAR(64) COMMENT '调用渠道的错误码',
    channel_error_msg VARCHAR(512) COMMENT '调用渠道的错误提示',
    channel_notify_data TEXT COMMENT '渠道的同步/异步通知的内容',
    channel_package_info VARCHAR(255) COMMENT '渠道 package 信息',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='转账单 DO';

-- ----------------------------
-- 会员钱包 DO (PayWalletDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS pay_wallet (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    user_id BIGINT COMMENT '用户 id',
    user_type INT COMMENT '用户类型, 预留 多商户转帐可能需要用到',
    balance INT COMMENT '余额，单位分',
    freeze_price INT COMMENT '冻结金额，单位分',
    total_expense INT COMMENT '累计支出，单位分',
    total_recharge INT COMMENT '累计充值，单位分',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='会员钱包 DO';

-- ----------------------------
-- 会员钱包充值 (PayWalletRechargeDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS pay_wallet_recharge (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    wallet_id BIGINT COMMENT '钱包编号',
    total_price INT COMMENT '用户实际到账余额',
    pay_price INT COMMENT '实际支付金额',
    bonus_price INT COMMENT '钱包赠送金额',
    package_id BIGINT COMMENT '充值套餐编号',
    pay_status BIT(1) COMMENT '是否已支付',
    pay_order_id BIGINT COMMENT '支付订单编号',
    pay_channel_code VARCHAR(64) COMMENT '支付成功的支付渠道',
    pay_time DATETIME COMMENT '订单支付时间',
    pay_refund_id BIGINT COMMENT '支付退款单编号',
    refund_total_price INT COMMENT '退款金额，包含赠送金额',
    refund_pay_price INT COMMENT '退款支付金额',
    refund_bonus_price INT COMMENT '退款钱包赠送金额',
    refund_time DATETIME COMMENT '退款时间',
    refund_status INT COMMENT '退款状态',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='会员钱包充值';

-- ----------------------------
-- 会员钱包充值套餐 DO (PayWalletRechargePackageDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS pay_wallet_recharge_package (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '套餐名',
    pay_price INT COMMENT '支付金额',
    bonus_price INT COMMENT '赠送金额',
    status INT COMMENT '状态',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='会员钱包充值套餐 DO';

-- ----------------------------
-- 会员钱包流水 DO (PayWalletTransactionDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS pay_wallet_transaction (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    no VARCHAR(64) COMMENT '流水号',
    wallet_id BIGINT COMMENT '钱包编号',
    biz_type INT COMMENT '关联业务分类',
    biz_id VARCHAR(255) COMMENT '关联业务编号',
    title VARCHAR(128) COMMENT '流水说明',
    price INT COMMENT '交易金额，单位分',
    balance INT COMMENT '交易后余额，单位分',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='会员钱包流水 DO';

-- ======================== product DDL ========================
-- 作者：zhicloud
-- 说明：由 DO 类字段生成，主键自增（MySQL id-type=AUTO），基础字段对齐 BaseDO/TenantBaseDO

-- ----------------------------
-- 商品品牌 DO (ProductBrandDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS product_brand (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '品牌编号',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '品牌名称',
    pic_url VARCHAR(512) COMMENT '品牌图片',
    sort INT COMMENT '品牌排序',
    description VARCHAR(512) COMMENT '品牌描述',
    status INT COMMENT '状态',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='商品品牌 DO';

-- ----------------------------
-- 商品分类 DO (ProductCategoryDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS product_category (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类编号',
    PRIMARY KEY (id),
    parent_id BIGINT COMMENT '父分类编号',
    name VARCHAR(128) COMMENT '分类名称',
    pic_url VARCHAR(512) COMMENT '移动端分类图',
    sort INT COMMENT '分类排序',
    status INT COMMENT '开启状态',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='商品分类 DO';

-- ----------------------------
-- 商品评论 DO (ProductCommentDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS product_comment (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论编号，主键自增',
    PRIMARY KEY (id),
    user_id BIGINT COMMENT '评价人的用户编号',
    user_nickname VARCHAR(255) COMMENT '评价人名称',
    user_avatar VARCHAR(512) COMMENT '评价人头像',
    anonymous BIT(1) COMMENT '是否匿名',
    order_id BIGINT COMMENT '交易订单编号',
    order_item_id BIGINT COMMENT '交易订单项编号',
    spu_id BIGINT COMMENT '商品 SPU 编号',
    spu_name VARCHAR(255) COMMENT '商品 SPU 名称',
    sku_id BIGINT COMMENT '商品 SKU 编号',
    sku_pic_url VARCHAR(512) COMMENT '商品 SKU 图片地址',
    sku_properties TEXT COMMENT '属性数组，JSON 格式',
    visible BIT(1) COMMENT '是否可见',
    scores INT COMMENT '评分星级',
    description_scores INT COMMENT '描述星级',
    benefit_scores INT COMMENT '服务星级',
    content TEXT COMMENT '评论内容',
    pic_urls TEXT COMMENT '评论图片地址数组',
    reply_status BIT(1) COMMENT '商家是否回复',
    reply_user_id BIGINT COMMENT '回复管理员编号',
    reply_content TEXT COMMENT '商家回复内容',
    reply_time DATETIME COMMENT '商家回复时间',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='商品评论 DO';

-- ----------------------------
-- 商品收藏 DO (ProductFavoriteDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS product_favorite (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号，主键自增',
    PRIMARY KEY (id),
    user_id BIGINT COMMENT '用户编号',
    spu_id BIGINT COMMENT '商品 SPU 编号',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='商品收藏 DO';

-- ----------------------------
-- 商品浏览记录 DO (ProductBrowseHistoryDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS product_browse_history (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录编号',
    PRIMARY KEY (id),
    spu_id BIGINT COMMENT '商品 SPU 编号',
    user_id BIGINT COMMENT '用户编号',
    user_deleted BIT(1) COMMENT '用户是否删除',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='商品浏览记录 DO';

-- ----------------------------
-- 商品属性项 DO (ProductPropertyDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS product_property (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '名称',
    remark VARCHAR(512) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='商品属性项 DO';

-- ----------------------------
-- 商品属性值 DO (ProductPropertyValueDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS product_property_value (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    PRIMARY KEY (id),
    property_id BIGINT COMMENT '属性项的编号',
    name VARCHAR(128) COMMENT '名称',
    remark VARCHAR(512) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='商品属性值 DO';

-- ----------------------------
-- 商品 SKU DO (ProductSkuDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS product_sku (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品 SKU 编号，自增',
    PRIMARY KEY (id),
    spu_id BIGINT COMMENT 'SPU 编号',
    properties TEXT COMMENT '属性数组，JSON 格式',
    price INT COMMENT '商品价格，单位：分',
    market_price INT COMMENT '市场价，单位：分',
    cost_price INT COMMENT '成本价，单位：分',
    bar_code VARCHAR(64) COMMENT '商品条码',
    pic_url VARCHAR(512) COMMENT '图片地址',
    stock INT COMMENT '库存',
    weight DOUBLE COMMENT '商品重量，单位：kg 千克',
    volume DOUBLE COMMENT '商品体积，单位：m^3 平米',
    first_brokerage_price INT COMMENT '一级分销的佣金，单位：分',
    second_brokerage_price INT COMMENT '二级分销的佣金，单位：分',
    sales_count INT COMMENT '商品销量',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='商品 SKU DO';

-- ----------------------------
-- 商品 SPU DO (ProductSpuDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS product_spu (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品 SPU 编号，自增',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '商品名称',
    keyword VARCHAR(255) COMMENT '关键字',
    introduction VARCHAR(255) COMMENT '商品简介',
    description VARCHAR(512) COMMENT '商品详情',
    category_id BIGINT COMMENT '商品分类编号',
    brand_id BIGINT COMMENT '商品品牌编号',
    pic_url VARCHAR(512) COMMENT '商品封面图',
    slider_pic_urls TEXT COMMENT '商品轮播图',
    sort INT COMMENT '排序字段',
    status INT COMMENT '商品状态',
    spec_type BIT(1) COMMENT '规格类型',
    price INT COMMENT '商品价格，单位使用：分',
    market_price INT COMMENT '市场价，单位使用：分',
    cost_price INT COMMENT '成本价，单位使用：分',
    stock INT COMMENT '库存',
    delivery_types TEXT COMMENT '配送方式数组',
    delivery_template_id BIGINT COMMENT '物流配置模板编号',
    give_integral INT COMMENT '赠送积分',
    sub_commission_type BIT(1) COMMENT '分销类型',
    sales_count INT COMMENT '商品销量',
    virtual_sales_count INT COMMENT '虚拟销量',
    browse_count INT COMMENT '浏览量',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='商品 SPU DO';

-- ======================== promotion DDL ========================
-- 作者：zhicloud
-- 说明：由 DO 类字段生成，主键自增（MySQL id-type=AUTO），基础字段对齐 BaseDO/TenantBaseDO

-- ----------------------------
-- 文章分类 DO (ArticleCategoryDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS promotion_article_category (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '文章分类编号',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '文章分类名称',
    pic_url VARCHAR(512) COMMENT '图标地址',
    status INT COMMENT '状态',
    sort INT COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='文章分类 DO';

-- ----------------------------
-- 文章管理 DO (ArticleDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS promotion_article (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '文章管理编号',
    PRIMARY KEY (id),
    category_id BIGINT COMMENT '分类编号 ArticleCategoryDO#id',
    spu_id BIGINT COMMENT '关联商品编号 ProductSpuDO#id',
    title VARCHAR(128) COMMENT '文章标题',
    author VARCHAR(255) COMMENT '文章作者',
    pic_url VARCHAR(512) COMMENT '文章封面图片地址',
    introduction VARCHAR(255) COMMENT '文章简介',
    browse_count INT COMMENT '浏览次数',
    sort INT COMMENT '排序',
    status INT COMMENT '状态',
    recommend_hot BIT(1) COMMENT '是否热门(小程序)',
    recommend_banner BIT(1) COMMENT '是否轮播图(小程序)',
    content TEXT COMMENT '文章内容',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='文章管理 DO';

-- ----------------------------
-- banner DO (BannerDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS promotion_banner (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    title VARCHAR(128) COMMENT '标题',
    url VARCHAR(512) COMMENT '跳转链接',
    pic_url VARCHAR(512) COMMENT '图片链接',
    sort INT COMMENT '排序',
    status INT COMMENT '状态 {@link CommonStatusEnum}',
    position INT COMMENT '定位 {@link BannerPositionEnum}',
    memo VARCHAR(255) COMMENT '备注',
    browse_count INT COMMENT '点击次数',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='banner DO';

-- ----------------------------
-- 砍价活动 DO (BargainActivityDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS promotion_bargain_activity (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '砍价活动编号',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '砍价活动名称',
    start_time DATETIME COMMENT '活动开始时间',
    end_time DATETIME COMMENT '活动结束时间',
    status INT COMMENT '活动状态',
    spu_id BIGINT COMMENT '商品 SPU 编号',
    sku_id BIGINT COMMENT '商品 SKU 编号',
    bargain_first_price INT COMMENT '砍价起始价格，单位：分',
    bargain_min_price INT COMMENT '砍价底价，单位：分',
    stock INT COMMENT '砍价库存(剩余库存砍价时扣减)',
    total_stock INT COMMENT '砍价总库存',
    help_max_count INT COMMENT '砍价人数',
    bargain_count INT COMMENT '帮砍次数',
    total_limit_count INT COMMENT '总限购数量',
    random_min_price INT COMMENT '用户每次砍价的最小金额，单位：分',
    random_max_price INT COMMENT '用户每次砍价的最大金额，单位：分',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='砍价活动 DO';

-- ----------------------------
-- 砍价助力 DO (BargainHelpDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS promotion_bargain_help (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    activity_id BIGINT COMMENT '砍价活动编号',
    record_id BIGINT COMMENT '砍价记录编号',
    user_id BIGINT COMMENT '用户编号',
    reduce_price INT COMMENT '减少价格，单位：分',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='砍价助力 DO';

-- ----------------------------
-- 砍价记录 DO TODO (BargainRecordDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS promotion_bargain_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    user_id BIGINT COMMENT '用户编号',
    activity_id BIGINT COMMENT '砍价活动编号',
    spu_id BIGINT COMMENT '商品 SPU 编号',
    sku_id BIGINT COMMENT '商品 SKU 编号',
    bargain_first_price INT COMMENT '砍价起始价格，单位：分',
    bargain_price INT COMMENT '当前砍价，单位：分',
    status INT COMMENT '砍价状态',
    end_time DATETIME COMMENT '结束时间',
    order_id BIGINT COMMENT '订单编号',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='砍价记录 DO TODO';

-- ----------------------------
-- 拼团活动 DO (CombinationActivityDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS promotion_combination_activity (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '活动编号',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '拼团名称',
    spu_id BIGINT COMMENT '商品 SPU 编号',
    total_limit_count INT COMMENT '总限购数量',
    single_limit_count INT COMMENT '单次限购数量',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    user_size INT COMMENT '几人团',
    virtual_group BIT(1) COMMENT '虚拟成团',
    status INT COMMENT '活动状态',
    limit_duration INT COMMENT '限制时长（小时）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='拼团活动 DO';

-- ----------------------------
-- 拼团商品 DO (CombinationProductDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS promotion_combination_product (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    activity_id BIGINT COMMENT '拼团活动编号',
    spu_id BIGINT COMMENT '商品 SPU 编号',
    sku_id BIGINT COMMENT '商品 SKU 编号',
    combination_price INT COMMENT '拼团价格，单位分',
    activity_status INT COMMENT '拼团商品状态',
    activity_start_time DATETIME COMMENT '活动开始时间点',
    activity_end_time DATETIME COMMENT '活动结束时间点',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='拼团商品 DO';

-- ----------------------------
-- 拼团记录 DO (CombinationRecordDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS promotion_combination_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号，主键自增',
    PRIMARY KEY (id),
    activity_id BIGINT COMMENT '拼团活动编号',
    combination_price INT COMMENT '拼团商品单价',
    spu_id BIGINT COMMENT 'SPU 编号',
    spu_name VARCHAR(255) COMMENT '商品名字',
    pic_url VARCHAR(512) COMMENT '商品图片',
    sku_id BIGINT COMMENT 'SKU 编号',
    count INT COMMENT '购买的商品数量',
    user_id BIGINT COMMENT '用户编号',
    nickname VARCHAR(128) COMMENT '用户昵称',
    avatar VARCHAR(512) COMMENT '用户头像',
    head_id BIGINT COMMENT '团长编号',
    status INT COMMENT '开团状态',
    order_id BIGINT COMMENT '订单编号',
    user_size INT COMMENT '开团需要人数',
    user_count INT COMMENT '已加入拼团人数',
    virtual_group BIT(1) COMMENT '是否虚拟成团',
    expire_time DATETIME COMMENT '过期时间',
    start_time DATETIME COMMENT '开始时间 (订单付款后开始的时间)',
    end_time DATETIME COMMENT '结束时间（成团时间/失败时间）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='拼团记录 DO';

-- ----------------------------
-- 优惠劵 DO (CouponDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS promotion_coupon (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '优惠劵编号',
    PRIMARY KEY (id),
    template_id BIGINT COMMENT '优惠劵模板编号',
    name VARCHAR(128) COMMENT '优惠劵名',
    status INT COMMENT '优惠码状态',
    user_id BIGINT COMMENT '用户编号',
    take_type INT COMMENT '领取类型',
    use_price INT COMMENT '是否设置满多少金额可用，单位：分',
    valid_start_time DATETIME COMMENT '生效开始时间',
    valid_end_time DATETIME COMMENT '生效结束时间',
    product_scope INT COMMENT '商品范围',
    product_scope_values TEXT COMMENT '商品范围编号的数组',
    discount_type INT COMMENT '折扣类型',
    discount_percent INT COMMENT '折扣百分比',
    discount_price INT COMMENT '优惠金额，单位：分',
    discount_limit_price INT COMMENT '折扣上限，仅在 {@link #discountType} 等于 {@link PromotionDiscountTypeEnum#PERCENT} 时生效',
    use_order_id BIGINT COMMENT '使用订单号',
    use_time DATETIME COMMENT '使用时间',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='优惠劵 DO';

-- ----------------------------
-- 优惠劵模板 DO (CouponTemplateDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS promotion_coupon_template (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '模板编号，自增唯一',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '优惠劵名',
    description VARCHAR(512) COMMENT '优惠券说明',
    status INT COMMENT '',
    total_count INT COMMENT '发放数量',
    take_limit_count INT COMMENT '每人限领个数',
    take_type INT COMMENT '领取方式',
    use_price INT COMMENT '是否设置满多少金额可用，单位：分',
    product_scope INT COMMENT '商品范围',
    product_scope_values TEXT COMMENT '商品范围编号的数组',
    validity_type INT COMMENT '生效日期类型',
    valid_start_time DATETIME COMMENT '固定日期 - 生效开始时间',
    valid_end_time DATETIME COMMENT '固定日期 - 生效结束时间',
    fixed_start_term INT COMMENT '领取日期 - 开始天数',
    fixed_end_term INT COMMENT '领取日期 - 结束天数',
    discount_type INT COMMENT '折扣类型',
    discount_percent INT COMMENT '折扣百分比',
    discount_price INT COMMENT '优惠金额，单位：分',
    discount_limit_price INT COMMENT '折扣上限，仅在 {@link #discountType} 等于 {@link PromotionDiscountTypeEnum#PERCENT} 时生效',
    take_count INT COMMENT '领取优惠券的数量',
    use_count INT COMMENT '使用优惠券的次数',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='优惠劵模板 DO';

-- ----------------------------
-- 限时折扣活动 DO (DiscountActivityDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS promotion_discount_activity (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '活动编号，主键自增',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '活动标题',
    status INT COMMENT '状态',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    remark VARCHAR(512) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='限时折扣活动 DO';

-- ----------------------------
-- 限时折扣商品 DO (DiscountProductDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS promotion_discount_product (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号，主键自增',
    PRIMARY KEY (id),
    activity_id BIGINT COMMENT '限时折扣活动的编号',
    spu_id BIGINT COMMENT '商品 SPU 编号',
    sku_id BIGINT COMMENT '商品 SKU 编号',
    discount_type INT COMMENT '折扣类型',
    discount_percent INT COMMENT '折扣百分比',
    discount_price INT COMMENT '优惠金额，单位：分',
    activity_name VARCHAR(255) COMMENT '活动标题',
    activity_status INT COMMENT '活动状态',
    activity_start_time DATETIME COMMENT '活动开始时间点',
    activity_end_time DATETIME COMMENT '活动结束时间点',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='限时折扣商品 DO';

-- ----------------------------
-- 装修页面 DO (DiyPageDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS promotion_diy_page (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '装修页面编号',
    PRIMARY KEY (id),
    template_id BIGINT COMMENT '装修模板编号',
    name VARCHAR(128) COMMENT '页面名称',
    remark VARCHAR(512) COMMENT '备注',
    preview_pic_urls TEXT COMMENT '预览图，多个逗号分隔',
    property VARCHAR(255) COMMENT '页面属性，JSON 格式',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='装修页面 DO';

-- ----------------------------
-- 装修模板 DO (DiyTemplateDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS promotion_diy_template (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '装修模板编号',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '模板名称',
    used BIT(1) COMMENT '是否使用',
    used_time DATETIME COMMENT '使用时间',
    remark VARCHAR(512) COMMENT '备注',
    preview_pic_urls TEXT COMMENT '预览图',
    property VARCHAR(255) COMMENT 'uni-app 底部导航属性，JSON 格式',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='装修模板 DO';

-- ----------------------------
-- 客服会话 DO (KeFuConversationDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS promotion_kefu_conversation (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    user_id BIGINT COMMENT '会话所属用户',
    last_message_time DATETIME COMMENT '最后聊天时间',
    last_message_content TEXT COMMENT '最后聊天内容',
    last_message_content_type INT COMMENT '最后发送的消息类型',
    admin_pinned BIT(1) COMMENT '管理端置顶',
    user_deleted BIT(1) COMMENT '用户是否可见',
    admin_deleted BIT(1) COMMENT '管理员是否可见',
    admin_unread_message_count INT COMMENT '管理员未读消息数',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='客服会话 DO';

-- ----------------------------
-- 客服消息 DO (KeFuMessageDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS promotion_kefu_message (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    conversation_id BIGINT COMMENT '会话编号',
    sender_id BIGINT COMMENT '发送人编号',
    sender_type INT COMMENT '发送人类型',
    receiver_id BIGINT COMMENT '接收人编号',
    receiver_type INT COMMENT '接收人类型',
    content_type INT COMMENT '消息类型',
    content TEXT COMMENT '消息',
    read_status BIT(1) COMMENT '是/否已读',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='客服消息 DO';

-- ----------------------------
-- 积分商城活动 DO (PointActivityDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS promotion_point_activity (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '积分商城活动编号',
    PRIMARY KEY (id),
    spu_id BIGINT COMMENT '积分商城活动商品',
    status INT COMMENT '活动状态',
    remark VARCHAR(512) COMMENT '备注',
    sort INT COMMENT '排序',
    stock INT COMMENT '积分商城活动库存(剩余库存积分兑换时扣减)',
    total_stock INT COMMENT '积分商城活动总库存',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='积分商城活动 DO';

-- ----------------------------
-- 积分商城商品 DO (PointProductDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS promotion_point_product (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '积分商城商品编号',
    PRIMARY KEY (id),
    activity_id BIGINT COMMENT '积分商城活动 id',
    spu_id BIGINT COMMENT '商品 SPU 编号',
    sku_id BIGINT COMMENT '商品 SKU 编号',
    count INT COMMENT '可兑换次数',
    point INT COMMENT '所需兑换积分',
    price INT COMMENT '所需兑换金额，单位：分',
    stock INT COMMENT '积分商城商品库存',
    activity_status INT COMMENT '积分商城商品状态',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='积分商城商品 DO';

-- ----------------------------
-- 满减送活动 DO (RewardActivityDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS promotion_reward_activity (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '活动编号，主键自增',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '活动标题',
    status INT COMMENT '状态',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    remark VARCHAR(512) COMMENT '备注',
    condition_type INT COMMENT '条件类型',
    product_scope INT COMMENT '商品范围',
    product_scope_values TEXT COMMENT '商品 SPU 编号的数组',
    rules TEXT COMMENT '优惠规则的数组',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='满减送活动 DO';

-- ----------------------------
-- 秒杀活动 DO (SeckillActivityDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS promotion_seckill_activity (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '秒杀活动编号',
    PRIMARY KEY (id),
    spu_id BIGINT COMMENT '秒杀活动商品',
    name VARCHAR(128) COMMENT '秒杀活动名称',
    status INT COMMENT '活动状态',
    remark VARCHAR(512) COMMENT '备注',
    start_time DATETIME COMMENT '活动开始时间',
    end_time DATETIME COMMENT '活动结束时间',
    sort INT COMMENT '排序',
    config_ids TEXT COMMENT '秒杀时段 id',
    total_limit_count INT COMMENT '总限购数量',
    single_limit_count INT COMMENT '单次限够数量',
    stock INT COMMENT '秒杀库存(剩余库存秒杀时扣减)',
    total_stock INT COMMENT '秒杀总库存',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='秒杀活动 DO';

-- ----------------------------
-- 秒杀时段 DO (SeckillConfigDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS promotion_seckill_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '秒杀时段名称',
    start_time VARCHAR(255) COMMENT '开始时间点',
    end_time VARCHAR(255) COMMENT '结束时间点',
    slider_pic_urls TEXT COMMENT '秒杀轮播图',
    status INT COMMENT '状态',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='秒杀时段 DO';

-- ----------------------------
-- 秒杀参与商品 DO (SeckillProductDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS promotion_seckill_product (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '秒杀参与商品编号',
    PRIMARY KEY (id),
    activity_id BIGINT COMMENT '秒杀活动 id',
    config_ids TEXT COMMENT '秒杀时段 id',
    spu_id BIGINT COMMENT '商品 SPU 编号',
    sku_id BIGINT COMMENT '商品 SKU 编号',
    seckill_price INT COMMENT '秒杀金额，单位：分',
    stock INT COMMENT '秒杀库存',
    activity_status INT COMMENT '秒杀商品状态',
    activity_start_time DATETIME COMMENT '活动开始时间点',
    activity_end_time DATETIME COMMENT '活动结束时间点',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='秒杀参与商品 DO';

-- ======================== trade DDL ========================
-- 作者：zhicloud
-- 说明：由 DO 类字段生成，主键自增（MySQL id-type=AUTO），基础字段对齐 BaseDO/TenantBaseDO

-- ----------------------------
-- 售后订单，用于处理 {@link TradeOrderDO} 交易订单的退款退货流程 (AfterSaleDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS trade_after_sale (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '售后编号，主键自增',
    PRIMARY KEY (id),
    no VARCHAR(64) COMMENT '售后单号',
    status INT COMMENT '退款状态',
    way INT COMMENT '售后方式',
    type INT COMMENT '售后类型',
    user_id BIGINT COMMENT '用户编号',
    apply_reason VARCHAR(512) COMMENT '申请原因',
    apply_description VARCHAR(512) COMMENT '补充描述',
    apply_pic_urls TEXT COMMENT '补充凭证图片',
    order_id BIGINT COMMENT '交易订单编号',
    order_no VARCHAR(64) COMMENT '订单流水号',
    order_item_id BIGINT COMMENT '交易订单项编号',
    spu_id BIGINT COMMENT '商品 SPU 编号',
    spu_name VARCHAR(255) COMMENT '商品 SPU 名称',
    sku_id BIGINT COMMENT '商品 SKU 编号',
    properties TEXT COMMENT '属性数组，JSON 格式',
    pic_url VARCHAR(512) COMMENT '商品图片',
    count INT COMMENT '退货商品数量',
    audit_time DATETIME COMMENT '审批时间',
    audit_user_id BIGINT COMMENT '审批人',
    audit_reason VARCHAR(512) COMMENT '审批备注',
    refund_price INT COMMENT '退款金额，单位：分。',
    pay_refund_id BIGINT COMMENT '支付退款编号',
    refund_time DATETIME COMMENT '退款时间',
    logistics_id BIGINT COMMENT '退货物流公司编号',
    logistics_no VARCHAR(64) COMMENT '退货物流单号',
    delivery_time DATETIME COMMENT '退货时间',
    receive_time DATETIME COMMENT '收货时间',
    receive_reason VARCHAR(512) COMMENT '收货备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='售后订单，用于处理 {@link TradeOrderDO} 交易订单的退款退货流程';

-- ----------------------------
-- 交易售后日志 DO (AfterSaleLogDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS trade_after_sale_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    user_id BIGINT COMMENT '用户编号',
    user_type INT COMMENT '用户类型',
    after_sale_id BIGINT COMMENT '售后编号',
    before_status INT COMMENT '操作前状态',
    after_status INT COMMENT '操作后状态',
    operate_type INT COMMENT '操作类型',
    content TEXT COMMENT '操作明细',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='交易售后日志 DO';

-- ----------------------------
-- 佣金记录 DO (BrokerageRecordDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS trade_brokerage_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    user_id BIGINT COMMENT '用户编号',
    biz_id VARCHAR(255) COMMENT '业务编号',
    biz_type INT COMMENT '业务类型',
    title VARCHAR(128) COMMENT '标题',
    description VARCHAR(512) COMMENT '说明',
    price INT COMMENT '金额',
    total_price INT COMMENT '当前总佣金',
    status INT COMMENT '状态',
    frozen_days INT COMMENT '冻结时间（天）',
    unfreeze_time DATETIME COMMENT '解冻时间',
    source_user_level INT COMMENT '来源用户等级',
    source_user_id BIGINT COMMENT '来源用户编号',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='佣金记录 DO';

-- ----------------------------
-- 分销用户 DO (BrokerageUserDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS trade_brokerage_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户编号',
    PRIMARY KEY (id),
    bind_user_id BIGINT COMMENT '推广员编号',
    bind_user_time DATETIME COMMENT '推广员绑定时间',
    brokerage_enabled BIT(1) COMMENT '是否有分销资格',
    brokerage_time DATETIME COMMENT '成为分销员时间',
    brokerage_price INT COMMENT '可用佣金',
    frozen_price INT COMMENT '冻结佣金',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='分销用户 DO';

-- ----------------------------
-- 佣金提现 DO (BrokerageWithdrawDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS trade_brokerage_withdraw (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    user_id BIGINT COMMENT '用户编号',
    price INT COMMENT '提现金额，单位：分',
    fee_price INT COMMENT '提现手续费，单位：分',
    total_price INT COMMENT '当前总佣金，单位：分',
    type INT COMMENT '提现类型',
    user_name VARCHAR(255) COMMENT '提现姓名',
    user_account VARCHAR(64) COMMENT '提现账号',
    qr_code_url VARCHAR(512) COMMENT '收款码',
    bank_name VARCHAR(255) COMMENT '银行名称',
    bank_address VARCHAR(255) COMMENT '开户地址',
    status INT COMMENT '状态',
    audit_reason VARCHAR(512) COMMENT '审核驳回原因',
    audit_time DATETIME COMMENT '审核时间',
    remark VARCHAR(512) COMMENT '备注',
    pay_transfer_id BIGINT COMMENT '转账单编号',
    transfer_channel_code VARCHAR(64) COMMENT '转账渠道',
    transfer_time DATETIME COMMENT '转账成功时间',
    transfer_error_msg VARCHAR(512) COMMENT '转账错误提示',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='佣金提现 DO';

-- ----------------------------
-- 购物车的商品信息 DO (CartDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS trade_cart (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号，唯一自增',
    PRIMARY KEY (id),
    user_id BIGINT COMMENT '用户编号',
    spu_id BIGINT COMMENT '商品 SPU 编号',
    sku_id BIGINT COMMENT '商品 SKU 编号',
    count INT COMMENT '商品购买数量',
    selected BIT(1) COMMENT '是否选中',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='购物车的商品信息 DO';

-- ----------------------------
-- 交易中心配置 DO (TradeConfigDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS trade_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    PRIMARY KEY (id),
    after_sale_refund_reasons TEXT COMMENT '售后的退款理由',
    after_sale_return_reasons TEXT COMMENT '售后的退货理由',
    delivery_express_free_enabled BIT(1) COMMENT '是否启用全场包邮',
    delivery_express_free_price INT COMMENT '全场包邮的最小金额，单位：分',
    delivery_pick_up_enabled BIT(1) COMMENT '是否开启自提',
    brokerage_enabled BIT(1) COMMENT '是否启用分佣',
    brokerage_enabled_condition INT COMMENT '分佣模式',
    brokerage_bind_mode INT COMMENT '分销关系绑定模式',
    brokerage_poster_urls TEXT COMMENT '分销海报图地址数组',
    brokerage_first_percent INT COMMENT '一级返佣比例',
    brokerage_second_percent INT COMMENT '二级返佣比例',
    brokerage_withdraw_min_price INT COMMENT '用户提现最低金额',
    brokerage_withdraw_fee_percent INT COMMENT '用户提现手续费百分比',
    brokerage_frozen_days INT COMMENT '佣金冻结时间(天)',
    brokerage_withdraw_types TEXT COMMENT '提现方式',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='交易中心配置 DO';

-- ----------------------------
-- 快递公司 DO (DeliveryExpressDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS trade_delivery_express (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号，自增',
    PRIMARY KEY (id),
    code VARCHAR(64) COMMENT '快递公司 code',
    name VARCHAR(128) COMMENT '快递公司名称',
    logo VARCHAR(512) COMMENT '快递公司 logo',
    sort INT COMMENT '排序',
    status INT COMMENT '状态',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='快递公司 DO';

-- ----------------------------
-- 快递运费模板计费配置 DO (DeliveryExpressTemplateChargeDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS trade_delivery_express_template_charge (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号，自增',
    PRIMARY KEY (id),
    template_id BIGINT COMMENT '配送模板编号',
    area_ids TEXT COMMENT '配送区域编号列表',
    charge_mode INT COMMENT '配送计费方式',
    start_count DOUBLE COMMENT '首件数量(件数,重量，或体积)',
    start_price INT COMMENT '起步价，单位：分',
    extra_count DOUBLE COMMENT '续件数量(件, 重量，或体积)',
    extra_price INT COMMENT '额外价，单位：分',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='快递运费模板计费配置 DO';

-- ----------------------------
-- 快递运费模板 DO (DeliveryExpressTemplateDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS trade_delivery_express_template (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号，自增',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '模板名称',
    charge_mode INT COMMENT '配送计费方式',
    sort INT COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='快递运费模板 DO';

-- ----------------------------
-- 快递运费模板包邮配置 DO (DeliveryExpressTemplateFreeDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS trade_delivery_express_template_free (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    template_id BIGINT COMMENT '配送模板编号',
    area_ids TEXT COMMENT '配送区域编号列表',
    free_price INT COMMENT '包邮金额，单位：分',
    free_count INT COMMENT '包邮件数',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='快递运费模板包邮配置 DO';

-- ----------------------------
-- 自提门店 DO (DeliveryPickUpStoreDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS trade_delivery_pick_up_store (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '门店名称',
    introduction VARCHAR(255) COMMENT '门店简介',
    phone VARCHAR(255) COMMENT '门店手机',
    area_id INT COMMENT '区域编号',
    detail_address VARCHAR(255) COMMENT '门店详细地址',
    logo VARCHAR(512) COMMENT '门店 logo',
    opening_time TIME COMMENT '营业开始时间',
    closing_time TIME COMMENT '营业结束时间',
    latitude DOUBLE COMMENT '纬度',
    longitude DOUBLE COMMENT '经度',
    verify_user_ids TEXT COMMENT '核销员工用户编号数组',
    status INT COMMENT '门店状态',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='自提门店 DO';

-- ----------------------------
-- 交易订单 DO (TradeOrderDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS trade_order (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单编号，主键自增',
    PRIMARY KEY (id),
    no VARCHAR(64) COMMENT '订单流水号',
    type INT COMMENT '订单类型',
    terminal INT COMMENT '订单来源',
    user_id BIGINT COMMENT '用户编号',
    user_ip VARCHAR(255) COMMENT '用户 IP',
    user_remark VARCHAR(512) COMMENT '用户备注',
    status INT COMMENT '订单状态',
    product_count INT COMMENT '购买的商品数量',
    finish_time DATETIME COMMENT '订单完成时间',
    cancel_time DATETIME COMMENT '订单取消时间',
    cancel_type INT COMMENT '取消类型',
    remark VARCHAR(512) COMMENT '商家备注',
    comment_status BIT(1) COMMENT '是否评价',
    brokerage_user_id BIGINT COMMENT '推广人编号',
    pay_order_id BIGINT COMMENT '支付订单编号',
    pay_status BIT(1) COMMENT '是否已支付',
    pay_time DATETIME COMMENT '付款时间',
    pay_channel_code VARCHAR(64) COMMENT '支付渠道',
    total_price INT COMMENT '商品原价，单位：分',
    discount_price INT COMMENT '优惠金额，单位：分',
    delivery_price INT COMMENT '运费金额，单位：分',
    adjust_price INT COMMENT '订单调价，单位：分',
    pay_price INT COMMENT '应付金额（总），单位：分',
    delivery_type INT COMMENT '配送方式',
    logistics_id BIGINT COMMENT '发货物流公司编号',
    logistics_no VARCHAR(64) COMMENT '发货物流单号',
    delivery_time DATETIME COMMENT '发货时间',
    receive_time DATETIME COMMENT '收货时间',
    receiver_name VARCHAR(255) COMMENT '收件人名称',
    receiver_mobile VARCHAR(255) COMMENT '收件人手机',
    receiver_area_id INT COMMENT '收件人地区编号',
    receiver_detail_address VARCHAR(255) COMMENT '收件人详细地址',
    pick_up_store_id BIGINT COMMENT '自提门店编号',
    pick_up_verify_code VARCHAR(64) COMMENT '自提核销码',
    refund_status INT COMMENT '售后状态',
    refund_price INT COMMENT '退款金额，单位：分',
    coupon_id BIGINT COMMENT '优惠劵编号',
    coupon_price INT COMMENT '优惠劵减免金额，单位：分',
    use_point INT COMMENT '使用的积分',
    point_price INT COMMENT '积分抵扣的金额，单位：分',
    give_point INT COMMENT '赠送的积分',
    refund_point INT COMMENT '退还的使用的积分',
    vip_price INT COMMENT 'VIP 减免金额，单位：分',
    give_coupon_template_counts TEXT COMMENT '赠送的优惠劵',
    give_coupon_ids TEXT COMMENT '赠送的优惠劵编号',
    seckill_activity_id BIGINT COMMENT '秒杀活动编号',
    bargain_activity_id BIGINT COMMENT '砍价活动编号',
    bargain_record_id BIGINT COMMENT '砍价记录编号',
    combination_activity_id BIGINT COMMENT '拼团活动编号',
    combination_head_id BIGINT COMMENT '拼团团长编号',
    combination_record_id BIGINT COMMENT '拼团记录编号',
    point_activity_id BIGINT COMMENT '积分商城活动的编号',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='交易订单 DO';

-- ----------------------------
-- 交易订单项 DO (TradeOrderItemDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS trade_order_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    user_id BIGINT COMMENT '用户编号',
    order_id BIGINT COMMENT '订单编号',
    cart_id BIGINT COMMENT '购物车项编号',
    spu_id BIGINT COMMENT '商品 SPU 编号',
    spu_name VARCHAR(255) COMMENT '商品 SPU 名称',
    sku_id BIGINT COMMENT '商品 SKU 编号',
    properties TEXT COMMENT '属性数组，JSON 格式',
    pic_url VARCHAR(512) COMMENT '商品图片',
    count INT COMMENT '购买数量',
    comment_status BIT(1) COMMENT '是否评价',
    price INT COMMENT '商品原价（单），单位：分',
    discount_price INT COMMENT '优惠金额（总），单位：分',
    delivery_price INT COMMENT '运费金额（总），单位：分',
    adjust_price INT COMMENT '订单调价（总），单位：分',
    pay_price INT COMMENT '应付金额（总），单位：分',
    coupon_price INT COMMENT '优惠劵减免金额，单位：分',
    point_price INT COMMENT '积分抵扣的金额，单位：分',
    use_point INT COMMENT '使用的积分',
    give_point INT COMMENT '赠送的积分',
    vip_price INT COMMENT 'VIP 减免金额，单位：分',
    after_sale_id BIGINT COMMENT '售后单编号',
    after_sale_status INT COMMENT '售后状态',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='交易订单项 DO';

-- ----------------------------
-- 订单日志 DO (TradeOrderLogDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS trade_order_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    user_id BIGINT COMMENT '用户编号',
    user_type INT COMMENT '用户类型',
    order_id BIGINT COMMENT '订单号',
    before_status INT COMMENT '操作前状态',
    after_status INT COMMENT '操作后状态',
    operate_type INT COMMENT '操作类型',
    content TEXT COMMENT '订单日志信息',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='订单日志 DO';

-- ======================== statistics DDL ========================
-- 作者：zhicloud
-- 说明：由 DO 类字段生成，主键自增（MySQL id-type=AUTO），基础字段对齐 BaseDO/TenantBaseDO

-- ----------------------------
-- 商品统计 DO (ProductStatisticsDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS product_statistics (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号，主键自增',
    PRIMARY KEY (id),
    time DATE COMMENT '统计日期',
    spu_id BIGINT COMMENT '商品 SPU 编号',
    browse_count INT COMMENT '浏览量',
    browse_user_count INT COMMENT '访客量',
    favorite_count INT COMMENT '收藏数量',
    cart_count INT COMMENT '加购数量',
    order_count INT COMMENT '下单件数',
    order_pay_count INT COMMENT '支付件数',
    order_pay_price INT COMMENT '支付金额，单位：分',
    after_sale_count INT COMMENT '退款件数',
    after_sale_refund_price INT COMMENT '退款金额，单位：分',
    browse_convert_percent INT COMMENT '访客支付转化率（百分比）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='商品统计 DO';

-- ----------------------------
-- 交易统计 DO (TradeStatisticsDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS trade_statistics (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号，主键自增',
    PRIMARY KEY (id),
    time DATETIME COMMENT '统计日期',
    order_create_count INT COMMENT '创建订单数',
    order_pay_count INT COMMENT '支付订单商品数',
    order_pay_price INT COMMENT '总支付金额，单位：分',
    after_sale_count INT COMMENT '退款订单数',
    after_sale_refund_price INT COMMENT '总退款金额，单位：分',
    brokerage_settlement_price INT COMMENT '佣金金额（已结算），单位：分',
    wallet_pay_price INT COMMENT '总支付金额（余额），单位：分',
    recharge_pay_count INT COMMENT '充值订单数',
    recharge_pay_price INT COMMENT '充值金额，单位：分',
    recharge_refund_count INT COMMENT '充值退款订单数',
    recharge_refund_price INT COMMENT '充值退款金额，单位：分',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='交易统计 DO';
