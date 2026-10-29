CREATE TABLE usuarios (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(255), email VARCHAR(255) UNIQUE,
 senha VARCHAR(255), telefone VARCHAR(255), data_nascimento DATE, cidade VARCHAR(255),
 peso DOUBLE, altura DOUBLE, objetivo VARCHAR(255), face_descriptor TEXT
);
CREATE TABLE sono (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, usuario_id BIGINT, horas INTEGER, qualidade VARCHAR(255), data_registro DATE
);
CREATE TABLE humor (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, usuario_id BIGINT, estado VARCHAR(255), observacao VARCHAR(500), data_registro DATE
);
CREATE TABLE ciclo_menstrual (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, usuario_id BIGINT, data_ultima_menstruacao DATE,
 duracao_ciclo INTEGER, duracao_menstruacao INTEGER, proxima_menstruacao DATE, ovulacao DATE,
 inicio_periodo_fertil DATE, fim_periodo_fertil DATE
);
CREATE TABLE consulta (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, usuario_id BIGINT, tipo VARCHAR(255) NOT NULL,
 titulo VARCHAR(255) NOT NULL, local VARCHAR(255), profissional VARCHAR(255), data_agendada DATE NOT NULL,
 hora_agendada TIME(6), observacao VARCHAR(500), status VARCHAR(255) NOT NULL, lembrete_dias_antes INTEGER
);
