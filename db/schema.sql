CREATE SCHEMA wordle_solver;

CREATE TABLE IF NOT EXISTS wordle_solver.t_scraped_urls (
    id SERIAL PRIMARY KEY,
    url TEXT NOT NULL UNIQUE,
    word_count INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS wordle_solver.t_word_counts (
    id SERIAL PRIMARY KEY,
    word VARCHAR(16) NOT NULL UNIQUE,
    count INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS wordle_solver.t_invalid_words (
    id SERIAL PRIMARY KEY,
    word VARCHAR(16) NOT NULL UNIQUE 
);

CREATE INDEX word_idx ON wordle_solver.t_word_counts(word);
CREATE INDEX url_idx ON wordle_solver.t_scraped_urls(url);
CREATE INDEX invalid_word_idk ON wordle_solver.t_invalid_words(word);