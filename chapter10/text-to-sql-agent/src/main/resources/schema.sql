-- =============================================================
-- text-to-sql-agent 教学库表结构（H2 内嵌库）
--
-- 关键点：每张表、每个字段都用 COMMENT ON 写上中文业务语义。
-- 这些注释不是摆设——SchemaInspector 会通过 JDBC DatabaseMetaData
-- 的 REMARKS 把它们读出来，拼成给大模型看的 schema 上下文，
-- 也作为 Schema Linking（找相关表）打分的依据。
-- 语义跟着 schema 走，这就是这个项目里最朴素的「语义层」。
-- =============================================================

DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS customers;

-- 客户表：谁在下单
CREATE TABLE customers (
    id            BIGINT PRIMARY KEY,
    name          VARCHAR(64)  NOT NULL,
    level         VARCHAR(16)  NOT NULL,
    city          VARCHAR(32)  NOT NULL,
    registered_at TIMESTAMP    NOT NULL
);
COMMENT ON TABLE  customers IS '客户表，记录客户姓名、客户等级、城市和注册时间';
COMMENT ON COLUMN customers.id IS '客户ID';
COMMENT ON COLUMN customers.name IS '客户姓名';
COMMENT ON COLUMN customers.level IS '客户等级，取值：普通/银卡/金卡';
COMMENT ON COLUMN customers.city IS '所在城市';
COMMENT ON COLUMN customers.registered_at IS '注册时间';

-- 商品表：卖的是什么
CREATE TABLE products (
    id         BIGINT PRIMARY KEY,
    name       VARCHAR(128) NOT NULL,
    category   VARCHAR(32)  NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL
);
COMMENT ON TABLE  products IS '商品表，记录商品名称、商品分类和单价';
COMMENT ON COLUMN products.id IS '商品ID';
COMMENT ON COLUMN products.name IS '商品名称';
COMMENT ON COLUMN products.category IS '商品分类，取值：数码/家电/服饰/食品';
COMMENT ON COLUMN products.unit_price IS '商品单价（元）';

-- 订单表：一次下单的主记录
CREATE TABLE orders (
    id           BIGINT PRIMARY KEY,
    customer_id  BIGINT       NOT NULL,
    status       VARCHAR(16)  NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    created_at   TIMESTAMP    NOT NULL
);
COMMENT ON TABLE  orders IS '订单表，记录客户下单的订单状态和订单总额';
COMMENT ON COLUMN orders.id IS '订单ID';
COMMENT ON COLUMN orders.customer_id IS '下单客户ID，关联客户表';
COMMENT ON COLUMN orders.status IS '订单状态，取值：PAID已支付/SHIPPED已发货/REFUNDED已退款';
COMMENT ON COLUMN orders.total_amount IS '订单总额（元）';
COMMENT ON COLUMN orders.created_at IS '下单时间';

-- 订单明细表：一张订单里具体买了哪些商品
CREATE TABLE order_items (
    id         BIGINT PRIMARY KEY,
    order_id   BIGINT       NOT NULL,
    product_id BIGINT       NOT NULL,
    quantity   INT          NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL
);
COMMENT ON TABLE  order_items IS '订单明细表，记录每笔订单包含的商品、成交数量和成交单价';
COMMENT ON COLUMN order_items.id IS '订单明细ID';
COMMENT ON COLUMN order_items.order_id IS '所属订单ID，关联订单表';
COMMENT ON COLUMN order_items.product_id IS '商品ID，关联商品表';
COMMENT ON COLUMN order_items.quantity IS '成交数量';
COMMENT ON COLUMN order_items.unit_price IS '成交单价（元）';

-- 支付表：订单通过什么渠道付了多少钱
CREATE TABLE payments (
    id       BIGINT PRIMARY KEY,
    order_id BIGINT       NOT NULL,
    channel  VARCHAR(16)  NOT NULL,
    amount   DECIMAL(12,2) NOT NULL,
    paid_at  TIMESTAMP    NOT NULL
);
COMMENT ON TABLE  payments IS '支付表，记录订单的支付渠道和支付金额';
COMMENT ON COLUMN payments.id IS '支付记录ID';
COMMENT ON COLUMN payments.order_id IS '支付对应的订单ID，关联订单表';
COMMENT ON COLUMN payments.channel IS '支付渠道，取值：ALIPAY支付宝/WECHAT微信/BANK_CARD银行卡';
COMMENT ON COLUMN payments.amount IS '支付金额（元）';
COMMENT ON COLUMN payments.paid_at IS '支付时间';
