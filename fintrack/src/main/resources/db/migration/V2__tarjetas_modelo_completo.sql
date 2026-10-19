-- Fase B: modelo completo de tarjetas.
--
-- 1) card pasa de (due_day date, amount) a due_day_of_month (1-31). El saldo ya
--    no se guarda en la tarjeta: se calcula como compras - pagos.
-- 2) transaction puede asociarse a una tarjeta (card_id) y deja de exigir
--    account_id: una compra con tarjeta no toca una cuenta hasta que se paga.
-- 2) card_payment: pagos del resumen, que mueven dinero de una cuenta real.

ALTER TABLE card ADD COLUMN due_day_of_month integer;

UPDATE card
SET due_day_of_month = EXTRACT(DAY FROM due_day)::int
WHERE due_day IS NOT NULL;

ALTER TABLE card ALTER COLUMN due_day_of_month SET NOT NULL;

ALTER TABLE card DROP COLUMN due_day;
ALTER TABLE card DROP COLUMN amount;

ALTER TABLE "transaction" ALTER COLUMN account_id DROP NOT NULL;
ALTER TABLE "transaction" ADD COLUMN card_id uuid;
ALTER TABLE "transaction"
    ADD CONSTRAINT fk_transaction_card FOREIGN KEY (card_id) REFERENCES card (id);

CREATE TABLE card_payment (
    id uuid NOT NULL,
    amount numeric(38,2) NOT NULL,
    date date NOT NULL,
    account_id uuid NOT NULL,
    card_id uuid NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT card_payment_pkey PRIMARY KEY (id),
    CONSTRAINT fk_card_payment_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT fk_card_payment_card FOREIGN KEY (card_id) REFERENCES card (id),
    CONSTRAINT fk_card_payment_user FOREIGN KEY (user_id) REFERENCES users (id)
);
