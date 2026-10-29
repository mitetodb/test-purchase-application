-- V1 baseline: schema generated from JPA entities (Hibernate MySQLDialect) at Flyway introduction.
-- Existing databases are baselined at version 1 (spring.flyway.baseline-on-migrate) and skip this script.

create table account_manager_customers (
    account_manager_id binary(16) not null,
    customer_id binary(16) not null,
    primary key (account_manager_id, customer_id)
) engine=InnoDB;

create table attachments (
    created_on datetime(6),
    file_size bigint,
    updated_on datetime(6),
    id binary(16) not null,
    test_purchase_id binary(16),
    file_path varchar(255),
    name varchar(255),
    updated_by_user varchar(255),
    category enum ('CANCELLATION','CURRENT_OFFER_PDF','INVOICE','ORDER_CONFIRMATION_PDF','PRODUCT_PICTURE','RETURN_LABEL','SHIPPING_LABEL'),
    file_type enum ('DOC','JPG','OTHER','PDF','PNG','XLS'),
    primary key (id)
) engine=InnoDB;

create table customers (
    base_service_fee float(53),
    created_on datetime(6),
    updated_on datetime(6),
    number varchar(10) not null,
    id binary(16) not null,
    category varchar(255),
    email varchar(255),
    name varchar(255) not null,
    updated_by_user varchar(255),
    country enum ('AUSTRIA','BELGIUM','BULGARIA','CROATIA','CYPRUS','CZECHIA','DENMARK','ESTONIA','FINLAND','FRANCE','GERMANY','GREECE','HUNGARY','IRELAND','ITALY','LATVIA','LITHUANIA','LUXEMBOURG','MALTA','NETHERLANDS','POLAND','PORTUGAL','ROMANIA','SLOVAKIA','SLOVENIA','SPAIN','SWEDEN','UNITED_KINGDOM','UNITED_STATES'),
    primary key (id)
) engine=InnoDB;

create table items (
    quantity integer,
    unit_price float(53),
    id binary(16) not null,
    purchase_id binary(16),
    product_name varchar(255),
    product_url varchar(255),
    primary key (id)
) engine=InnoDB;

create table password_reset_tokens (
    used bit not null,
    expires_at datetime(6) not null,
    id binary(16) not null,
    user_id binary(16) not null,
    token varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create table shops (
    created_on datetime(6),
    updated_on datetime(6),
    number varchar(10) not null,
    id binary(16) not null,
    description TEXT,
    name varchar(255) not null,
    notes TEXT,
    updated_by_user varchar(255),
    country enum ('AUSTRIA','BELGIUM','BULGARIA','CROATIA','CYPRUS','CZECHIA','DENMARK','ESTONIA','FINLAND','FRANCE','GERMANY','GREECE','HUNGARY','IRELAND','ITALY','LATVIA','LITHUANIA','LUXEMBOURG','MALTA','NETHERLANDS','POLAND','PORTUGAL','ROMANIA','SLOVAKIA','SLOVENIA','SPAIN','SWEDEN','UNITED_KINGDOM','UNITED_STATES'),
    primary key (id)
) engine=InnoDB;

create table test_purchases (
    postage_fee float(53),
    product_price float(53),
    service_fee float(53),
    created_on datetime(6),
    updated_on datetime(6),
    customer_id binary(16),
    id binary(16) not null,
    mystery_shopper_id binary(16),
    shop_id binary(16),
    number varchar(20) not null,
    updated_by_user varchar(255),
    category enum ('L','M','S','XL'),
    country enum ('AUSTRIA','BELGIUM','BULGARIA','CROATIA','CYPRUS','CZECHIA','DENMARK','ESTONIA','FINLAND','FRANCE','GERMANY','GREECE','HUNGARY','IRELAND','ITALY','LATVIA','LITHUANIA','LUXEMBOURG','MALTA','NETHERLANDS','POLAND','PORTUGAL','ROMANIA','SLOVAKIA','SLOVENIA','SPAIN','SWEDEN','UNITED_KINGDOM','UNITED_STATES'),
    status enum ('CANCELLED','CLOSED','INITIALISED','IN_PROGRESS','PRODUCT_FORWARDED_TO_CLIENT','PRODUCT_ORDERED','PRODUCT_RECEIVED','PRODUCT_SHIPPED_BACK_TO_SELLER'),
    type enum ('FORWARDING_TO_CLIENT','RETURN_BACK_TO_SELLER'),
    primary key (id)
) engine=InnoDB;

create table test_purchases_attachments (
    attachments_id binary(16) not null,
    test_purchase_id binary(16) not null
) engine=InnoDB;

create table testpurchase_status_history (
    changed_at datetime(6) not null,
    id binary(16) not null,
    testpurchase_id binary(16) not null,
    changed_by varchar(255) not null,
    comment varchar(255),
    new_status enum ('CANCELLED','CLOSED','INITIALISED','IN_PROGRESS','PRODUCT_FORWARDED_TO_CLIENT','PRODUCT_ORDERED','PRODUCT_RECEIVED','PRODUCT_SHIPPED_BACK_TO_SELLER') not null,
    old_status enum ('CANCELLED','CLOSED','INITIALISED','IN_PROGRESS','PRODUCT_FORWARDED_TO_CLIENT','PRODUCT_ORDERED','PRODUCT_RECEIVED','PRODUCT_SHIPPED_BACK_TO_SELLER') not null,
    primary key (id)
) engine=InnoDB;

create table users (
    active bit not null,
    created_on datetime(6),
    updated_on datetime(6),
    id binary(16) not null,
    email varchar(255) not null,
    image_url varchar(255),
    language varchar(255),
    password varchar(255) not null,
    status varchar(255),
    updated_by_user varchar(255),
    username varchar(255) not null,
    country enum ('AUSTRIA','BELGIUM','BULGARIA','CROATIA','CYPRUS','CZECHIA','DENMARK','ESTONIA','FINLAND','FRANCE','GERMANY','GREECE','HUNGARY','IRELAND','ITALY','LATVIA','LITHUANIA','LUXEMBOURG','MALTA','NETHERLANDS','POLAND','PORTUGAL','ROMANIA','SLOVAKIA','SLOVENIA','SPAIN','SWEDEN','UNITED_KINGDOM','UNITED_STATES'),
    role enum ('ACCOUNT_MANAGER','ADMIN','CUSTOMER','MYSTERY_SHOPPER','SALES_MANAGER') not null,
    primary key (id)
) engine=InnoDB;

alter table customers 
   add constraint UKnuc5kd2p40ql2xsn6ca5lg7q4 unique (number);

alter table password_reset_tokens 
   add constraint UK71lqwbwtklmljk3qlsugr1mig unique (token);

alter table shops 
   add constraint UKss62uhsc7p09xc3kf1hvfgwug unique (number);

alter table test_purchases 
   add constraint UKcgdbo3aolyh9fhmk7km99f5pd unique (number);

alter table test_purchases_attachments 
   add constraint UKn1wbct97mj9pnxwh19dnf2fil unique (attachments_id);

alter table users 
   add constraint UK6dotkott2kjsp8vw4d0m25fb7 unique (email);

alter table users 
   add constraint UKr43af9ap4edm43mmtq01oddj6 unique (username);

alter table account_manager_customers 
   add constraint FK7119d7qvfdq6tdbj3jyjdhhs4 
   foreign key (customer_id) 
   references customers (id);

alter table account_manager_customers 
   add constraint FKkql0t6njm8nv7yprm7biv9p5r 
   foreign key (account_manager_id) 
   references users (id);

alter table attachments 
   add constraint FK5l5g1k6qdycks1impf5y01tnm 
   foreign key (test_purchase_id) 
   references test_purchases (id);

alter table items 
   add constraint FKmrrx05ccqjp1wqexdc0jt3hh0 
   foreign key (purchase_id) 
   references test_purchases (id);

alter table password_reset_tokens 
   add constraint FKk3ndxg5xp6v7wd4gjyusp15gq 
   foreign key (user_id) 
   references users (id);

alter table test_purchases 
   add constraint FKioc4dln5d5tkd3nk6wlli9rq9 
   foreign key (customer_id) 
   references customers (id);

alter table test_purchases 
   add constraint FKoqf7ypfqtqt8de2jrhqh5fi2j 
   foreign key (mystery_shopper_id) 
   references users (id);

alter table test_purchases 
   add constraint FK71tnuriemqosvk67lwd1c7s4e 
   foreign key (shop_id) 
   references shops (id);

alter table test_purchases_attachments 
   add constraint FK35x39wercc4lxo5frehusw0se 
   foreign key (attachments_id) 
   references attachments (id);

alter table test_purchases_attachments 
   add constraint FKgom73bt43f081ondspomlq7t9 
   foreign key (test_purchase_id) 
   references test_purchases (id);

alter table testpurchase_status_history 
   add constraint FKd5q3ppse7bedfl0fd56rx1vgu 
   foreign key (testpurchase_id) 
   references test_purchases (id);
