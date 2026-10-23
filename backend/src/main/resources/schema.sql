CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE SEQUENCE IF NOT EXISTS ticket_id_seq START 1001;

CREATE TABLE IF NOT EXISTS ticket (
  id text PRIMARY KEY CHECK (id ~ '^TKT-[0-9]+$'),
  description text NOT NULL CHECK (length(btrim(description)) > 0),
  status text NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'CANCELLED')),
  priority text NOT NULL CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW')),
  assignee text,
  category text NOT NULL CHECK (length(btrim(category)) > 0)
);

CREATE TABLE IF NOT EXISTS comment (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  ticket_id text NOT NULL REFERENCES ticket (id) ON DELETE CASCADE,
  body text NOT NULL CHECK (length(btrim(body)) > 0),
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS comment_ticket_id_idx ON comment (ticket_id, created_at, id);

CREATE TABLE IF NOT EXISTS ticket_resolution (
  ticket_id text PRIMARY KEY REFERENCES ticket (id) ON DELETE CASCADE,
  notes text NOT NULL CHECK (length(btrim(notes)) > 0)
);

CREATE TABLE IF NOT EXISTS vector_store (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  content text NOT NULL,
  metadata json NOT NULL,
  embedding vector(768) NOT NULL
);

CREATE INDEX IF NOT EXISTS vector_store_embedding_idx ON vector_store USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS vector_store_ticket_id_idx ON vector_store ((metadata->>'ticketId'));
