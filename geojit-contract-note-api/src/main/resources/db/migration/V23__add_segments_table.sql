CREATE TABLE segments (
  id           SERIAL PRIMARY KEY,
  code         VARCHAR(50)  UNIQUE NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  s3_folder    VARCHAR(50)  NOT NULL,
  is_active    BOOLEAN      NOT NULL DEFAULT true,
  created_at   TIMESTAMP    NOT NULL DEFAULT now()
);

INSERT INTO segments (code, display_name, s3_folder)
VALUES ('EQUITY-COMBINEMARGIN', 'Equity Combine Margin', 'equity');
