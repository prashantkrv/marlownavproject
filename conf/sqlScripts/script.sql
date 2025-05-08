-- Run this script for automatically setting up all the databases, tables and
-- other dependencies required by the application
CREATE USER "marlow" WITH PASSWORD 'marlow';

CREATE DATABASE "marlowdb" WITH OWNER = "marlow";

ALTER USER "marlow" SET SEARCH_PATH = "$user", BANK;

\c marlowdb

CREATE SCHEMA IF NOT EXISTS BANK
    AUTHORIZATION "marlow";

GRANT ALL PRIVILEGES ON DATABASE marlowdb TO marlow;
ALTER DEFAULT PRIVILEGES IN SCHEMA BANK GRANT ALL ON TABLES TO marlow;
ALTER DEFAULT PRIVILEGES IN SCHEMA BANK GRANT ALL ON SEQUENCES TO marlow;
ALTER DEFAULT PRIVILEGES IN SCHEMA BANK GRANT ALL ON FUNCTIONS TO marlow;

CREATE TABLE BANK."Accounts" (
        "id" SERIAL PRIMARY KEY,
        "type" VARCHAR NOT NULL,
        "balance" INT NOT NULL,
        "owners" VARCHAR,
        "withdrawalLimit" INT NOT NULL,
        "createdAt" TIMESTAMP,
        "updatedAt" TIMESTAMP
);

CREATE TABLE BANK."Users" (
        "id" SERIAL PRIMARY KEY,
        "name" VARCHAR NOT NULL,
        "address" VARCHAR NOT NULL,
        "createdAt" TIMESTAMP,
        "updatedAt" TIMESTAMP
);


CREATE OR REPLACE FUNCTION update_timestamps()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        NEW."createdAt" = NOW();
        NEW."updatedAt" = NOW();
    ELSIF TG_OP = 'UPDATE' THEN
        NEW."updatedAt" = NOW();
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE TRIGGER accounts_update_timestamp
BEFORE INSERT OR UPDATE ON BANK."Accounts"
FOR EACH ROW
EXECUTE FUNCTION update_timestamps();

CREATE OR REPLACE TRIGGER users_update_timestamp
BEFORE INSERT OR UPDATE ON BANK."Users"
FOR EACH ROW
EXECUTE FUNCTION update_timestamps();