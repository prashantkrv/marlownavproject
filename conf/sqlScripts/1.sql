
CREATE USER "marlownav9" WITH PASSWORD 'marlownav9';

CREATE DATABASE "marlownavdb9" WITH OWNER = "marlownav9";

ALTER USER "marlownav9" SET SEARCH_PATH = "$user", BANK;

\c marlownavdb9

CREATE SCHEMA IF NOT EXISTS BANK
    AUTHORIZATION "marlownav9";

GRANT ALL PRIVILEGES ON DATABASE marlownavdb9 TO marlownav9;
--GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA BANK TO marlownav9;
--GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA BANK TO marlownav9;
--GRANT USAGE, CREATE ON SCHEMA BANK TO marlownav9;
ALTER DEFAULT PRIVILEGES IN SCHEMA BANK GRANT ALL ON TABLES TO marlownav9;
ALTER DEFAULT PRIVILEGES IN SCHEMA BANK GRANT ALL ON SEQUENCES TO marlownav9;
ALTER DEFAULT PRIVILEGES IN SCHEMA BANK GRANT ALL ON FUNCTIONS TO marlownav9;

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