-- Lista de la compra compartida de la casa.
CREATE TABLE articulo_compra (
    id            BIGSERIAL PRIMARY KEY,
    nombre        VARCHAR(80)  NOT NULL,
    -- Nombre en minúsculas y sin tildes, para no duplicar "Plátanos" y "platanos"
    nombre_clave  VARCHAR(80)  NOT NULL,
    cantidad      VARCHAR(30),
    categoria     VARCHAR(20)  NOT NULL DEFAULT 'OTROS',
    comprado      BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en     TIMESTAMP WITH TIME ZONE NOT NULL,
    comprado_en   TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_articulo_compra_clave ON articulo_compra (nombre_clave);
