SET CHARACTER SET utf8s;
DROP DATABASE IF EXISTS chess;
CREATE DATABASE chess CHARACTER SET utf8;
USE chess;

CREATE TABLE OpeningMove (
identity BIGINT AUTO_INCREMENT,
position VARCHAR(128) NOT NULL,
source SMALLINT NOT NULL,
sink SMALLINT NOT NULL,
rating BIGINT NOT NULL,
searchDepth TINYINT NOT NULL,
PRIMARY KEY (identity),
UNIQUE KEY (position, source, sink)
) ENGINE=InnoDB;

