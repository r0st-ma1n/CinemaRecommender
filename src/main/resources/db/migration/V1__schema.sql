-- Схема базы данных CinemaRecommender

-- Тип: фильм, сериал, мультфильм
CREATE TABLE type (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE genre (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE mood (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE movie (
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(200)     NOT NULL,
    release_year INTEGER          NOT NULL,
    description  TEXT             NOT NULL,
    poster_url   VARCHAR(500),
    rating       DOUBLE PRECISION NOT NULL CHECK (rating BETWEEN 0 AND 10),
    type_id      BIGINT           NOT NULL REFERENCES type (id)
);

-- Связи многие-ко-многим. При удалении фильма, жанра или настроения
-- строки связи удаляются автоматически (ON DELETE CASCADE).
CREATE TABLE movie_genre (
    movie_id BIGINT NOT NULL REFERENCES movie (id) ON DELETE CASCADE,
    genre_id BIGINT NOT NULL REFERENCES genre (id) ON DELETE CASCADE,
    PRIMARY KEY (movie_id, genre_id)
);

CREATE TABLE movie_mood (
    movie_id BIGINT NOT NULL REFERENCES movie (id) ON DELETE CASCADE,
    mood_id  BIGINT NOT NULL REFERENCES mood (id) ON DELETE CASCADE,
    PRIMARY KEY (movie_id, mood_id)
);
