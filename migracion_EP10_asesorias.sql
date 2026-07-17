-- ============================================================
-- Migración EP10 — Carreras/Materias UPT y Agendado de Asesorías
-- Convenciones idénticas a BDEduFlow.sql (InnoDB, utf8mb4, FKs con
-- ON DELETE/UPDATE RESTRICT). El backend también crea estas tablas
-- automáticamente al iniciar (SchemaUtils.createMissingTablesAndColumns),
-- igual que el resto del esquema actual — este script es de respaldo.
-- ============================================================

-- railway.asesor_disponibilidad definition

CREATE TABLE IF NOT EXISTS `asesor_disponibilidad` (
  `id` int NOT NULL AUTO_INCREMENT,
  `asesor_id` int NOT NULL,
  `dia_semana` int NOT NULL,        -- 1=Lun, 2=Mar, 3=Mié, 4=Jue, 5=Vie
  `hora_inicio` varchar(5) NOT NULL, -- "09:00"
  `hora_fin` varchar(5) NOT NULL,    -- "10:00"
  PRIMARY KEY (`id`),
  KEY `fk_asesor_disponibilidad_asesor_id__id` (`asesor_id`),
  CONSTRAINT `fk_asesor_disponibilidad_asesor_id__id` FOREIGN KEY (`asesor_id`) REFERENCES `usuarios` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- railway.asesorias definition

CREATE TABLE IF NOT EXISTS `asesorias` (
  `id` int NOT NULL AUTO_INCREMENT,
  `asesor_id` int NOT NULL,
  `alumno_id` int NOT NULL,
  `disponibilidad_id` int NOT NULL,
  `fecha` date NOT NULL,
  `estado` varchar(20) NOT NULL DEFAULT 'PENDIENTE',  -- PENDIENTE | ACEPTADA | CANCELADA
  `mensaje_asesor` text,
  `enlace` varchar(500) DEFAULT NULL,
  `ubicacion` varchar(200) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_asesorias_asesor_id__id` (`asesor_id`),
  KEY `fk_asesorias_alumno_id__id` (`alumno_id`),
  KEY `fk_asesorias_disponibilidad_id__id` (`disponibilidad_id`),
  CONSTRAINT `fk_asesorias_alumno_id__id` FOREIGN KEY (`alumno_id`) REFERENCES `usuarios` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_asesorias_asesor_id__id` FOREIGN KEY (`asesor_id`) REFERENCES `usuarios` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_asesorias_disponibilidad_id__id` FOREIGN KEY (`disponibilidad_id`) REFERENCES `asesor_disponibilidad` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- railway.notificaciones definition

CREATE TABLE IF NOT EXISTS `notificaciones` (
  `id` int NOT NULL AUTO_INCREMENT,
  `usuario_id` int NOT NULL,
  `titulo` varchar(200) NOT NULL,
  `contenido` text NOT NULL,
  `leida` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_notificaciones_usuario_id__id` (`usuario_id`),
  CONSTRAINT `fk_notificaciones_usuario_id__id` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
