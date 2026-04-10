-- Migração V1: Estrutura inicial do banco de dados
-- Sistema de Monitoramento da Qualidade do Ar para Escolas

-- ============================================================
-- Tabela de escolas
-- ============================================================
CREATE TABLE schools (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    cnpj        VARCHAR(18) UNIQUE,
    address     VARCHAR(300),
    city        VARCHAR(100),
    state       CHAR(2),
    zip_code    VARCHAR(9),
    email       VARCHAR(150),
    phone       VARCHAR(20),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Tabela de salas / ambientes monitorados
-- ============================================================
CREATE TABLE rooms (
    id          BIGSERIAL PRIMARY KEY,
    school_id   BIGINT NOT NULL REFERENCES schools(id),
    name        VARCHAR(100) NOT NULL,
    floor       VARCHAR(20),
    capacity    INTEGER,
    area_m2     NUMERIC(6,2),
    room_type   VARCHAR(50) NOT NULL DEFAULT 'CLASSROOM',
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Tabela de sensores
-- ============================================================
CREATE TABLE sensors (
    id              BIGSERIAL PRIMARY KEY,
    room_id         BIGINT NOT NULL REFERENCES rooms(id),
    serial_number   VARCHAR(100) UNIQUE NOT NULL,
    model           VARCHAR(100),
    manufacturer    VARCHAR(100),
    firmware_version VARCHAR(20),
    measures_co2    BOOLEAN DEFAULT TRUE,
    measures_pm25   BOOLEAN DEFAULT TRUE,
    measures_pm10   BOOLEAN DEFAULT TRUE,
    measures_tvoc   BOOLEAN DEFAULT FALSE,
    measures_temp   BOOLEAN DEFAULT TRUE,
    measures_humidity BOOLEAN DEFAULT TRUE,
    measures_no2    BOOLEAN DEFAULT FALSE,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_seen_at    TIMESTAMP,
    installed_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Tabela de leituras dos sensores (particionada por mês)
-- ============================================================
CREATE TABLE sensor_readings (
    id              BIGSERIAL,
    sensor_id       BIGINT NOT NULL REFERENCES sensors(id),
    co2_ppm         NUMERIC(7,2),
    pm25_ugm3       NUMERIC(7,3),
    pm10_ugm3       NUMERIC(7,3),
    tvoc_mgm3       NUMERIC(7,4),
    temperature_c   NUMERIC(5,2),
    humidity_pct    NUMERIC(5,2),
    no2_ppb         NUMERIC(7,3),
    aqi_value       INTEGER,
    aqi_category    VARCHAR(30),
    recorded_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, recorded_at)
) PARTITION BY RANGE (recorded_at);

-- Partições para os próximos 12 meses (exemplo para 2025-2026)
CREATE TABLE sensor_readings_2025_01 PARTITION OF sensor_readings
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
CREATE TABLE sensor_readings_2025_06 PARTITION OF sensor_readings
    FOR VALUES FROM ('2025-06-01') TO ('2025-07-01');
CREATE TABLE sensor_readings_2025_07 PARTITION OF sensor_readings
    FOR VALUES FROM ('2025-07-01') TO ('2025-08-01');
CREATE TABLE sensor_readings_2025_08 PARTITION OF sensor_readings
    FOR VALUES FROM ('2025-08-01') TO ('2025-09-01');
CREATE TABLE sensor_readings_2025_09 PARTITION OF sensor_readings
    FOR VALUES FROM ('2025-09-01') TO ('2025-10-01');
CREATE TABLE sensor_readings_2025_10 PARTITION OF sensor_readings
    FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');
CREATE TABLE sensor_readings_2025_11 PARTITION OF sensor_readings
    FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');
CREATE TABLE sensor_readings_2025_12 PARTITION OF sensor_readings
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');
CREATE TABLE sensor_readings_2026_01 PARTITION OF sensor_readings
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE sensor_readings_2026_02 PARTITION OF sensor_readings
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
CREATE TABLE sensor_readings_2026_03 PARTITION OF sensor_readings
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
CREATE TABLE sensor_readings_2026_04 PARTITION OF sensor_readings
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
CREATE TABLE sensor_readings_2026_05 PARTITION OF sensor_readings
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
CREATE TABLE sensor_readings_default PARTITION OF sensor_readings DEFAULT;

-- ============================================================
-- Índices de desempenho
-- ============================================================
CREATE INDEX idx_readings_sensor_time ON sensor_readings (sensor_id, recorded_at DESC);
CREATE INDEX idx_readings_aqi ON sensor_readings (aqi_category, recorded_at DESC);
CREATE INDEX idx_sensors_room ON sensors (room_id);
CREATE INDEX idx_rooms_school ON rooms (school_id);

-- ============================================================
-- Tabela de alertas
-- ============================================================
CREATE TABLE alerts (
    id              BIGSERIAL PRIMARY KEY,
    sensor_id       BIGINT NOT NULL REFERENCES sensors(id),
    reading_id      BIGINT,
    alert_type      VARCHAR(50) NOT NULL,
    severity        VARCHAR(20) NOT NULL,
    pollutant       VARCHAR(20),
    measured_value  NUMERIC(10,4),
    threshold_value NUMERIC(10,4),
    message         TEXT NOT NULL,
    acknowledged    BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_by VARCHAR(100),
    acknowledged_at TIMESTAMP,
    resolved        BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at     TIMESTAMP,
    notified        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alerts_sensor ON alerts (sensor_id, created_at DESC);
CREATE INDEX idx_alerts_unresolved ON alerts (resolved, severity) WHERE resolved = FALSE;

-- ============================================================
-- Tabela de usuários do sistema
-- ============================================================
CREATE TABLE system_users (
    id              BIGSERIAL PRIMARY KEY,
    school_id       BIGINT REFERENCES schools(id),
    name            VARCHAR(150) NOT NULL,
    email           VARCHAR(150) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(30) NOT NULL DEFAULT 'SCHOOL_ADMIN',
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Tabela de relatórios gerados
-- ============================================================
CREATE TABLE reports (
    id              BIGSERIAL PRIMARY KEY,
    school_id       BIGINT NOT NULL REFERENCES schools(id),
    report_type     VARCHAR(50) NOT NULL,
    period_start    TIMESTAMP NOT NULL,
    period_end      TIMESTAMP NOT NULL,
    generated_by    VARCHAR(150),
    file_path       VARCHAR(500),
    summary         JSONB,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Dados de exemplo
-- ============================================================
INSERT INTO schools (name, cnpj, address, city, state, zip_code, email, phone)
VALUES
  ('Escola Estadual João Pessoa', '12.345.678/0001-90', 'Rua das Flores, 123', 'São Paulo', 'SP', '01310-100', 'admin@eejp.edu.br', '(11) 3456-7890'),
  ('EMEF Maria Montessori', '98.765.432/0001-10', 'Av. Brasil, 456', 'Guarulhos', 'SP', '07010-000', 'admin@mariamontessori.edu.br', '(11) 2345-6789');

INSERT INTO rooms (school_id, name, floor, capacity, area_m2, room_type)
VALUES
  (1, 'Sala 101', '1º andar', 35, 48.0, 'CLASSROOM'),
  (1, 'Sala 102', '1º andar', 35, 48.0, 'CLASSROOM'),
  (1, 'Biblioteca', 'Térreo', 60, 80.0, 'LIBRARY'),
  (1, 'Laboratório de Ciências', '2º andar', 30, 60.0, 'LABORATORY'),
  (2, 'Sala A1', '1º andar', 40, 52.0, 'CLASSROOM'),
  (2, 'Refeitório', 'Térreo', 150, 200.0, 'CAFETERIA');

INSERT INTO sensors (room_id, serial_number, model, manufacturer, measures_tvoc, measures_no2, status, installed_at)
VALUES
  (1, 'SN-2024-001', 'AirGuard Pro 3000', 'SensorTech', TRUE, FALSE, 'ACTIVE', NOW()),
  (2, 'SN-2024-002', 'AirGuard Pro 3000', 'SensorTech', TRUE, FALSE, 'ACTIVE', NOW()),
  (3, 'SN-2024-003', 'EduAir Compact', 'ClimateWatch', FALSE, FALSE, 'ACTIVE', NOW()),
  (4, 'SN-2024-004', 'AirGuard Pro 3000', 'SensorTech', TRUE, TRUE, 'ACTIVE', NOW()),
  (5, 'SN-2024-005', 'EduAir Compact', 'ClimateWatch', FALSE, FALSE, 'ACTIVE', NOW()),
  (6, 'SN-2024-006', 'AirGuard Industrial', 'SensorTech', TRUE, TRUE, 'ACTIVE', NOW());

INSERT INTO system_users (school_id, name, email, password_hash, role)
VALUES
  (NULL, 'Administrador Sistema', 'admin@sistema.gov.br', '$2a$12$placeholder_hash', 'SYSTEM_ADMIN'),
  (1, 'Diretora Ana Silva', 'ana.silva@eejp.edu.br', '$2a$12$placeholder_hash', 'SCHOOL_ADMIN'),
  (2, 'Diretor Carlos Souza', 'carlos.souza@mariamontessori.edu.br', '$2a$12$placeholder_hash', 'SCHOOL_ADMIN');
