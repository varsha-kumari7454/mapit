ALTER TABLE registeredappuser ADD COLUMN superuserscount BIGINT DEFAULT 0;
ALTER TABLE registeredappuser ADD COLUMN createdassubuser BOOLEAN DEFAULT false;