CREATE TABLE budgets
(
    id           UUID           NOT NULL,
    trip_id      UUID           NOT NULL,
    total_amount NUMERIC(14, 2) NOT NULL,
    currency     VARCHAR(3)     NOT NULL,

    locked       BOOLEAN        NOT NULL DEFAULT FALSE,

    created_by   UUID           NOT NULL,
    created_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_budgets
        PRIMARY KEY (id),

    CONSTRAINT uk_budgets_trip_id
        UNIQUE (trip_id),

    CONSTRAINT fk_budgets_trip
        FOREIGN KEY (trip_id)
            REFERENCES trips (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_budgets_created_by
        FOREIGN KEY (created_by)
            REFERENCES users (id),

    CONSTRAINT chk_budgets_total_amount
        CHECK (total_amount >= 0),

    CONSTRAINT chk_budgets_currency
        CHECK (char_length(currency) = 3)
);


CREATE TABLE expenses
(
    id               UUID           NOT NULL,
    trip_id          UUID           NOT NULL,
    budget_id        UUID           NOT NULL,

    booking_id       UUID,

    category         VARCHAR(30)    NOT NULL,

    title            VARCHAR(200)   NOT NULL,
    description      VARCHAR(500),

    amount           NUMERIC(14, 2) NOT NULL,
    currency         VARCHAR(3)     NOT NULL,

    exchange_rate    NUMERIC(18, 8),
    converted_amount NUMERIC(14, 2),

    expense_date     TIMESTAMP      NOT NULL,

    payment_method   VARCHAR(30),

    paid_by          UUID           NOT NULL,
    created_by       UUID           NOT NULL,

    recurring        BOOLEAN        NOT NULL DEFAULT FALSE,
    recurrence_rule  VARCHAR(100),

    archived         BOOLEAN        NOT NULL DEFAULT FALSE,

    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_expenses
        PRIMARY KEY (id),

    CONSTRAINT fk_expenses_trip
        FOREIGN KEY (trip_id)
            REFERENCES trips (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_expenses_budget
        FOREIGN KEY (budget_id)
            REFERENCES budgets (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_expenses_booking
        FOREIGN KEY (booking_id)
            REFERENCES bookings (id)
            ON DELETE SET NULL,

    CONSTRAINT fk_expenses_paid_by
        FOREIGN KEY (paid_by)
            REFERENCES users (id),

    CONSTRAINT fk_expenses_created_by
        FOREIGN KEY (created_by)
            REFERENCES users (id),

    CONSTRAINT chk_expenses_amount
        CHECK (amount >= 0),

    CONSTRAINT chk_expenses_currency
        CHECK (char_length(currency) = 3),

    CONSTRAINT chk_expenses_exchange_rate
        CHECK (
            exchange_rate IS NULL
                OR exchange_rate > 0
            ),

    CONSTRAINT chk_expenses_converted_amount
        CHECK (
            converted_amount IS NULL
                OR converted_amount >= 0
            )
);


CREATE INDEX idx_budgets_trip_id
    ON budgets(trip_id);


CREATE INDEX idx_budgets_created_by
    ON budgets(created_by);


CREATE INDEX idx_expenses_trip_id
    ON expenses(trip_id);


CREATE INDEX idx_expenses_budget_id
    ON expenses(budget_id);


CREATE INDEX idx_expenses_booking_id
    ON expenses(booking_id);


CREATE INDEX idx_expenses_category
    ON expenses(category);


CREATE INDEX idx_expenses_paid_by
    ON expenses(paid_by);


CREATE INDEX idx_expenses_expense_date
    ON expenses(expense_date);


CREATE INDEX idx_expenses_trip_category
    ON expenses(trip_id, category);


CREATE INDEX idx_expenses_trip_date
    ON expenses(trip_id, expense_date);


CREATE INDEX idx_expenses_trip_archived
    ON expenses(trip_id, archived);