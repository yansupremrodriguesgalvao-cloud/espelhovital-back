ALTER TABLE ciclo_menstrual ADD COLUMN irregular BOOLEAN;
ALTER TABLE ciclo_menstrual ADD COLUMN duracao_minima INTEGER;
ALTER TABLE ciclo_menstrual ADD COLUMN duracao_maxima INTEGER;
ALTER TABLE usuarios ADD COLUMN auth_version INTEGER NOT NULL DEFAULT 0;
CREATE INDEX idx_sono_usuario_data ON sono(usuario_id, data_registro, id);
CREATE INDEX idx_humor_usuario_data ON humor(usuario_id, data_registro, id);
CREATE INDEX idx_ciclo_usuario_data ON ciclo_menstrual(usuario_id, data_ultima_menstruacao, id);
CREATE INDEX idx_consulta_usuario_data ON consulta(usuario_id, data_agendada, status);
CREATE TABLE password_reset (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, usuario_id BIGINT NOT NULL UNIQUE,
 token_hash VARCHAR(64) NOT NULL UNIQUE, expires_at TIMESTAMP(6) NOT NULL
);
