CREATE TABLE diario (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 usuario_id BIGINT NOT NULL,
 data_registro DATE NOT NULL,
 fluxo VARCHAR(30) NOT NULL,
 intensidade_dor INTEGER,
 sintomas VARCHAR(500) NOT NULL,
 observacoes VARCHAR(2000),
 sono_id BIGINT,
 humor_id BIGINT,
 atualizado_em TIMESTAMP(6) NOT NULL,
 CONSTRAINT uk_diario_usuario_data UNIQUE (usuario_id, data_registro),
 CONSTRAINT fk_diario_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);
CREATE TABLE lembrete_preferencia (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 usuario_id BIGINT NOT NULL UNIQUE,
 ativo BOOLEAN NOT NULL,
 diario BOOLEAN NOT NULL,
 sono BOOLEAN NOT NULL,
 consultas BOOLEAN NOT NULL,
 ciclo BOOLEAN NOT NULL,
 navegador_ativo BOOLEAN NOT NULL,
 email_ativo BOOLEAN NOT NULL,
 horario TIME(6) NOT NULL,
 fuso_horario VARCHAR(80) NOT NULL,
 dias_semana VARCHAR(30) NOT NULL,
 antecedencia_consulta INTEGER NOT NULL,
 antecedencia_ciclo INTEGER NOT NULL,
 CONSTRAINT fk_lembrete_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);
CREATE TABLE lembrete_entrega (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 usuario_id BIGINT NOT NULL,
 data_referencia DATE NOT NULL,
 canal VARCHAR(20) NOT NULL,
 chave VARCHAR(120) NOT NULL,
 status VARCHAR(20) NOT NULL,
 tentativas INTEGER NOT NULL,
 ultima_tentativa TIMESTAMP(6) NOT NULL,
 CONSTRAINT uk_lembrete_entrega UNIQUE (usuario_id, data_referencia, canal, chave),
 CONSTRAINT fk_entrega_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);
