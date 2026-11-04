-- Base extensions used by the schema.
-- pgcrypto: gen_random_uuid()
-- citext: case-insensitive email storage
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;
