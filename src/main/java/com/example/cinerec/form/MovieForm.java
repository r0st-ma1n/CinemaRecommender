package com.example.cinerec.form;

import com.example.cinerec.entity.Genre;
import com.example.cinerec.entity.Mood;
import com.example.cinerec.entity.Movie;
import jakarta.validation.constraints.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Данные формы добавления/изменения фильма.
 * Тип, жанры и настроения приходят из формы как id, а не как объекты:
 * так форма проще, а нужные записи контроллер сам загружает из базы.
 */
public class MovieForm {

    private Long id;

    @NotBlank(message = "Укажите название")
    @Size(max = 200, message = "Не длиннее 200 символов")
    private String title;

    @NotNull(message = "Укажите год")
    @Min(value = 1888, message = "Слишком ранний год")
    @Max(value = 2100, message = "Слишком поздний год")
    private Integer year;

    @NotBlank(message = "Добавьте описание")
    private String description;

    @Size(max = 500, message = "Не длиннее 500 символов")
    private String posterUrl;

    @NotNull(message = "Укажите рейтинг")
    @DecimalMin(value = "0.0", message = "Рейтинг от 0 до 10")
    @DecimalMax(value = "10.0", message = "Рейтинг от 0 до 10")
    private Double rating;

    @NotNull(message = "Выберите тип")
    private Long typeId;

    @NotEmpty(message = "Выберите хотя бы один жанр")
    private List<Long> genreIds = new ArrayList<>();

    @NotEmpty(message = "Выберите хотя бы одно настроение")
    private List<Long> moodIds = new ArrayList<>();

    /** Заполнить форму данными существующего фильма (для страницы изменения). */
    public static MovieForm fromMovie(Movie movie) {
        MovieForm form = new MovieForm();
        form.id = movie.getId();
        form.title = movie.getTitle();
        form.year = movie.getYear();
        form.description = movie.getDescription();
        form.posterUrl = movie.getPosterUrl();
        form.rating = movie.getRating();
        form.typeId = movie.getType().getId();
        form.genreIds = movie.getGenres().stream().map(Genre::getId).toList();
        form.moodIds = movie.getMoods().stream().map(Mood::getId).toList();
        return form;
    }

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

    public Long getTypeId() { return typeId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }

    public List<Long> getGenreIds() { return genreIds; }
    public void setGenreIds(List<Long> genreIds) { this.genreIds = genreIds; }

    public List<Long> getMoodIds() { return moodIds; }
    public void setMoodIds(List<Long> moodIds) { this.moodIds = moodIds; }
}
