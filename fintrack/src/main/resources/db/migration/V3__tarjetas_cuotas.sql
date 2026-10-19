-- Fase 4: tarjetas con cuotas.
--
-- Un detalle es una compra de la tarjeta que se paga en N cuotas. Cada cuota
-- se materializa en card_installment con su importe y su vencimiento, y el
-- pago de una cuota deja su rastro en card_payment (que mueve la plata de la
-- cuenta). El saldo de la tarjeta pasa a ser la suma de las cuotas impagas.

ALTER TABLE "transaction" ADD COLUMN installments integer NOT NULL DEFAULT 1;

CREATE TABLE card_installment (
    id uuid NOT NULL,
    number integer NOT NULL,
    amount numeric(38,2) NOT NULL,
    due_date date NOT NULL,
    paid boolean NOT NULL DEFAULT false,
    paid_date date,
    account_id uuid,
    transaction_id uuid NOT NULL,
    card_id uuid NOT NULL,
    CONSTRAINT card_installment_pkey PRIMARY KEY (id),
    CONSTRAINT uq_card_installment_number UNIQUE (transaction_id, number),
    CONSTRAINT fk_card_installment_transaction FOREIGN KEY (transaction_id) REFERENCES "transaction" (id),
    CONSTRAINT fk_card_installment_card FOREIGN KEY (card_id) REFERENCES card (id),
    CONSTRAINT fk_card_installment_account FOREIGN KEY (account_id) REFERENCES account (id)
);

-- Cada compra de tarjeta ya cargada pasa a ser un detalle de una sola cuota,
-- venciendo el proximo dia de cierre de la tarjeta.
INSERT INTO card_installment (id, number, amount, due_date, transaction_id, card_id)
SELECT gen_random_uuid(),
       1,
       t.amount,
       (
           DATE_TRUNC('month', t.date) + INTERVAL '1 month'
           + (LEAST(
                  c.due_day_of_month,
                  EXTRACT(DAY FROM (DATE_TRUNC('month', t.date) + INTERVAL '2 month - 1 day'))::int
              ) - 1) * INTERVAL '1 day'
       )::date,
       t.id,
       t.card_id
FROM "transaction" t
JOIN card c ON c.id = t.card_id
WHERE t.card_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM card_installment ci WHERE ci.transaction_id = t.id);

-- Todo pago queda ligado a la cuota que salda. Los pagos libres de monto se
-- eliminan: en la base real no hay ninguno.
ALTER TABLE card_payment ADD COLUMN card_installment_id uuid;
DELETE FROM card_payment WHERE card_installment_id IS NULL;
ALTER TABLE card_payment
    ADD CONSTRAINT uq_card_payment_installment UNIQUE (card_installment_id);
ALTER TABLE card_payment
    ADD CONSTRAINT fk_card_payment_installment
    FOREIGN KEY (card_installment_id) REFERENCES card_installment (id);
ALTER TABLE card_payment ALTER COLUMN card_installment_id SET NOT NULL;
