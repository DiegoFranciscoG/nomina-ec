-- nomina-ec: esquema base (ver docs/modelo-datos.md)
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------------------------------------------------------------- seguridad
CREATE TABLE app_users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(60)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'PAYROLL', 'VIEWER')),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------- personal
CREATE TABLE positions (
    id     BIGSERIAL PRIMARY KEY,
    code   VARCHAR(20)  NOT NULL UNIQUE,
    name   VARCHAR(120) NOT NULL,
    active BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE employees (
    id                     BIGSERIAL PRIMARY KEY,
    id_number              VARCHAR(10)  NOT NULL UNIQUE,
    first_names            VARCHAR(80)  NOT NULL,
    last_names             VARCHAR(80)  NOT NULL,
    email                  VARCHAR(120),
    family_dependents      SMALLINT     NOT NULL DEFAULT 0 CHECK (family_dependents BETWEEN 0 AND 20),
    catastrophic_condition BOOLEAN      NOT NULL DEFAULT FALSE,
    active                 BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE contracts (
    id                BIGSERIAL PRIMARY KEY,
    employee_id       BIGINT        NOT NULL REFERENCES employees (id),
    position_id       BIGINT        NOT NULL REFERENCES positions (id),
    contract_type     VARCHAR(20)   NOT NULL CHECK (contract_type IN ('INDEFINITE', 'EVENTUAL', 'OCCASIONAL', 'SEASONAL', 'PER_PROJECT')),
    weekly_hours      SMALLINT      NOT NULL DEFAULT 40 CHECK (weekly_hours BETWEEN 1 AND 40),
    monthly_salary    NUMERIC(12,2) NOT NULL CHECK (monthly_salary > 0),
    start_date        DATE          NOT NULL,
    end_date          DATE,
    region            VARCHAR(20)   NOT NULL CHECK (region IN ('COSTA_GALAPAGOS', 'SIERRA_AMAZONIA')),
    thirteenth_mode   VARCHAR(12)   NOT NULL DEFAULT 'MONTHLY' CHECK (thirteenth_mode IN ('MONTHLY', 'ACCUMULATED')),
    fourteenth_mode   VARCHAR(12)   NOT NULL DEFAULT 'MONTHLY' CHECK (fourteenth_mode IN ('MONTHLY', 'ACCUMULATED')),
    reserve_fund_mode VARCHAR(12)   NOT NULL DEFAULT 'MONTHLY' CHECK (reserve_fund_mode IN ('MONTHLY', 'ACCUMULATED')),
    status            VARCHAR(12)   NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'TERMINATED')),
    CHECK (end_date IS NULL OR end_date >= start_date)
);
CREATE UNIQUE INDEX ux_contracts_one_active ON contracts (employee_id) WHERE status = 'ACTIVE';

-- ---------------------------------------------------------------- configuración legal
CREATE TABLE concepts (
    id                 BIGSERIAL PRIMARY KEY,
    code               VARCHAR(40)  NOT NULL UNIQUE,
    name               VARCHAR(120) NOT NULL,
    concept_type       VARCHAR(25)  NOT NULL CHECK (concept_type IN ('INCOME', 'DEDUCTION', 'PROVISION', 'EMPLOYER_CONTRIBUTION')),
    iess_taxable       BOOLEAN      NOT NULL,
    income_tax_taxable BOOLEAN      NOT NULL,
    sort_order         SMALLINT     NOT NULL DEFAULT 0
);

CREATE TABLE legal_parameters (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(60)    NOT NULL,
    value       NUMERIC(14,6)  NOT NULL,
    valid_from  DATE           NOT NULL,
    valid_to    DATE,
    description VARCHAR(255)   NOT NULL,
    legal_basis VARCHAR(255)   NOT NULL,
    source_url  VARCHAR(500)   NOT NULL,
    created_by  VARCHAR(60)    NOT NULL DEFAULT 'system',
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CHECK (valid_to IS NULL OR valid_to >= valid_from),
    EXCLUDE USING gist (code WITH =, daterange(valid_from, valid_to, '[]') WITH &&)
);
CREATE INDEX ix_legal_parameters_code ON legal_parameters (code, valid_from);

CREATE TABLE legal_parameter_audit (
    id           BIGSERIAL PRIMARY KEY,
    parameter_id BIGINT       NOT NULL REFERENCES legal_parameters (id),
    code         VARCHAR(60)  NOT NULL,
    action       VARCHAR(20)  NOT NULL CHECK (action IN ('CREATE', 'CLOSE_VALIDITY')),
    old_value    JSONB,
    new_value    JSONB        NOT NULL,
    reason       VARCHAR(500) NOT NULL,
    changed_by   VARCHAR(60)  NOT NULL,
    changed_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_legal_parameter_audit_code ON legal_parameter_audit (code, changed_at DESC);

CREATE TABLE income_tax_brackets (
    id            BIGSERIAL PRIMARY KEY,
    fiscal_year   SMALLINT      NOT NULL,
    lower_bound   NUMERIC(12,2) NOT NULL,
    upper_bound   NUMERIC(12,2),
    base_tax      NUMERIC(12,2) NOT NULL,
    marginal_rate NUMERIC(6,4)  NOT NULL CHECK (marginal_rate BETWEEN 0 AND 1),
    source_url    VARCHAR(500)  NOT NULL,
    UNIQUE (fiscal_year, lower_bound),
    CHECK (upper_bound IS NULL OR upper_bound > lower_bound)
);

CREATE TABLE personal_expense_caps (
    id                BIGSERIAL PRIMARY KEY,
    fiscal_year       SMALLINT     NOT NULL,
    family_dependents SMALLINT     NOT NULL CHECK (family_dependents BETWEEN 0 AND 5),
    basket_multiplier NUMERIC(6,2) NOT NULL CHECK (basket_multiplier > 0),
    source_url        VARCHAR(500) NOT NULL,
    UNIQUE (fiscal_year, family_dependents)
);

CREATE TABLE personal_expense_projections (
    id               BIGSERIAL PRIMARY KEY,
    employee_id      BIGINT        NOT NULL REFERENCES employees (id),
    fiscal_year      SMALLINT      NOT NULL,
    projected_amount NUMERIC(12,2) NOT NULL CHECK (projected_amount >= 0),
    UNIQUE (employee_id, fiscal_year)
);

-- ---------------------------------------------------------------- operación
CREATE TABLE payroll_periods (
    id            BIGSERIAL PRIMARY KEY,
    year          SMALLINT    NOT NULL CHECK (year BETWEEN 2000 AND 2100),
    month         SMALLINT    NOT NULL CHECK (month BETWEEN 1 AND 12),
    status        VARCHAR(12) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CALCULATED', 'CLOSED')),
    calculated_at TIMESTAMPTZ,
    closed_at     TIMESTAMPTZ,
    closed_by     VARCHAR(60),
    UNIQUE (year, month)
);

CREATE TABLE novelties (
    id           BIGSERIAL PRIMARY KEY,
    period_id    BIGINT        NOT NULL REFERENCES payroll_periods (id),
    employee_id  BIGINT        NOT NULL REFERENCES employees (id),
    novelty_type VARCHAR(30)   NOT NULL CHECK (novelty_type IN ('OVERTIME_SUPPLEMENTARY', 'OVERTIME_EXTRAORDINARY', 'ABSENCE_DAYS', 'ADVANCE', 'BONUS', 'OTHER_DEDUCTION')),
    quantity     NUMERIC(8,2)  NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    amount       NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (amount >= 0),
    description  VARCHAR(255),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX ix_novelties_period_employee ON novelties (period_id, employee_id);

CREATE TABLE payslips (
    id                  BIGSERIAL PRIMARY KEY,
    period_id           BIGINT        NOT NULL REFERENCES payroll_periods (id),
    employee_id         BIGINT        NOT NULL REFERENCES employees (id),
    contract_id         BIGINT        NOT NULL REFERENCES contracts (id),
    worked_days         NUMERIC(5,2)  NOT NULL,
    base_salary         NUMERIC(12,2) NOT NULL,
    iess_base           NUMERIC(12,2) NOT NULL,
    income_tax_base     NUMERIC(12,2) NOT NULL,
    projected_annual_tax_base NUMERIC(12,2) NOT NULL,
    total_income        NUMERIC(12,2) NOT NULL,
    total_deductions    NUMERIC(12,2) NOT NULL,
    net_pay             NUMERIC(12,2) NOT NULL,
    employer_cost       NUMERIC(12,2) NOT NULL,
    parameters_snapshot JSONB         NOT NULL,
    calculated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (period_id, employee_id)
);

CREATE TABLE payslip_lines (
    id           BIGSERIAL PRIMARY KEY,
    payslip_id   BIGINT        NOT NULL REFERENCES payslips (id) ON DELETE CASCADE,
    concept_code VARCHAR(40)   NOT NULL REFERENCES concepts (code),
    quantity     NUMERIC(10,2),
    rate         NUMERIC(14,6),
    amount       NUMERIC(12,2) NOT NULL,
    sort_order   SMALLINT      NOT NULL DEFAULT 0
);
CREATE INDEX ix_payslip_lines_payslip ON payslip_lines (payslip_id);

CREATE TABLE provisions (
    id             BIGSERIAL PRIMARY KEY,
    payslip_id     BIGINT        NOT NULL REFERENCES payslips (id) ON DELETE CASCADE,
    employee_id    BIGINT        NOT NULL REFERENCES employees (id),
    period_id      BIGINT        NOT NULL REFERENCES payroll_periods (id),
    provision_type VARCHAR(20)   NOT NULL CHECK (provision_type IN ('THIRTEENTH', 'FOURTEENTH', 'RESERVE_FUND', 'VACATION')),
    amount         NUMERIC(12,2) NOT NULL,
    paid_monthly   BOOLEAN       NOT NULL
);
CREATE INDEX ix_provisions_employee ON provisions (employee_id, provision_type);

CREATE TABLE settlements (
    id               BIGSERIAL PRIMARY KEY,
    employee_id      BIGINT        NOT NULL REFERENCES employees (id),
    contract_id      BIGINT        NOT NULL UNIQUE REFERENCES contracts (id),
    termination_date DATE          NOT NULL,
    reason           VARCHAR(30)   NOT NULL CHECK (reason IN ('RESIGNATION', 'MUTUAL_AGREEMENT', 'UNJUSTIFIED_DISMISSAL', 'JUSTIFIED_DISMISSAL')),
    total            NUMERIC(12,2) NOT NULL,
    detail           JSONB         NOT NULL,
    created_by       VARCHAR(60)   NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);
