-- Additive migration after V001; select the target database explicitly before execution.
-- Existing historical rows remain NULL; only new checkout orders use replay matching.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS request_fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL;
