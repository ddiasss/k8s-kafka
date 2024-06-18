drop table if exists order_product;
drop table if exists stock;

create table order_product (id bigserial primary key, product_id bigint not null, status text not null);
create table stock (id bigserial primary key, amount bigint not null, product_id bigint not null, unique (product_id));
create index stock_product_id on stock (product_id);

insert into stock values (1, 10, 100), (2, 75, 200), (3, 100, 300);