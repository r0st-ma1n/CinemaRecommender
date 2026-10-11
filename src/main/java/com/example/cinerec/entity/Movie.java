package com.example.cinerec.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "movie")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Укажите название")
    @Size(max = 200, message = "Не длиннее 200 символов")
    @Column(nullable = false, length = 200)
    private String title;

    @NotNull(message = "Укажите год")
    @Min(value = 1888, message = "Слишком ранний год")
    @Max(value = 2100, message = "Слишком поздний год")
    @Column(name = "release_year", nullable = false)
    private Integer year;

    @NotBlank(message = "Добавьте описание")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    // Ссылка на постер; если её нет, на сайте показывается заглушка
    @Size(max = 500)
    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @NotNull(message = "Укажите рейтинг")
    @DecimalMin(value = "0.0", message = "Рейтинг от 0 до 10")
    @DecimalMax(value = "10.0", message = "Рейтинг от 0 до 10")
    @Column(nullable = false)
    private Double rating;

    @NotNull(message = "Выберите тип")
    @ManyToOne
    @JoinColumn(name = "type_id", nullable = false)
    private MovieType type;

    @ManyToMany
    @JoinTable(name = "movie_genre",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<Genre> genres = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "movie_mood",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "mood_id"))
    private Set<Mood> moods = new HashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public MovieType getType() { return type; }
    public void setType(MovieType type) { this.type = type; }

    public Set<Genre> getGenres() { return genres; }
    public void setGenres(Set<Genre> genres) { this.genres = genres; }

    public Set<Mood> getMoods() { return moods; }
    public void setMoods(Set<Mood> moods) { this.moods = moods; }
}
