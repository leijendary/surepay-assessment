--liquibase formatted sql
--changeset leijendekker:create-batch-table
create table batch (
    id uuid primary key default gen_random_uuid(),
    status character varying(10) not null,
    created_at timestamp without time zone not null default now()
);

--changeset leijendekker:create-transaction-table
create table "transaction" (
    id bigint generated always as identity primary key,
    batch_id uuid references batch(id) on delete cascade,
    reference bigint not null,
    account_number character(18) not null,
    description text not null,
    start_balance numeric(12,2) not null,
    mutation numeric(12,2) not null,
    end_balance numeric(12,2) not null
);

--changeset leijendekker:create-transaction-batch-id-reference-unique-index
create unique index transaction_batch_id_reference_key on "transaction"(batch_id, reference);
