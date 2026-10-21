package com.example.cinerec.controller.api;

import com.example.cinerec.repository.GenreRepository;
import com.example.cinerec.repository.MoodRepository;
import com.example.cinerec.repository.MovieRepository;
import com.example.cinerec.repository.MovieTypeRepository;
import com.example.cinerec.service.Criteria;
import com.example.cinerec.service.MovieService;
import com.example.cinerec.service.RecommendationService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

/** Публичная часть API для фронтенда на React: каталог, справочники и подбор. */
@RestController
@RequestMapping("/api")
public class CatalogApiController {

    private final MovieRepository movieRepository;
    private final MovieTypeRepository typeRepository;
    private final GenreRepository genreRepository;
    private final MoodRepository moodRepository;
    private final MovieService movieService;
    private final RecommendationService recommendationService;

    public CatalogApiController(MovieRepository movieRepository,
                                MovieTypeRepository typeRepository,
                                GenreRepository genreRepository,
                                MoodRepository moodRepository,
                                MovieService movieService,
                                RecommendationService recommendationService) {
        this.movieRepository = movieRepository;
        this.typeRepository = typeRepository;
        this.genreRepository = genreRepository;
        this.moodRepository = moodRepository;
        this.movieService = movieService;
        this.recommendationService = recommendationService;
    }

    @GetMapping("/movies")
    public List<MovieDto> movies() {
        return movieRepository.findAllWithDetailsForCatalog().stream().map(MovieDto::from).toList();
    }

    @GetMapping("/movies/{id}")
    public MovieDto movie(@PathVariable Long id) {
        return MovieDto.from(movieService.find(id));
    }

    // Порядок как в выпадающих списках на сайте: типы и настроения по id, жанры по алфавиту
    @GetMapping("/types")
    public List<DictionaryItemDto> types() {
        return typeRepository.findAll(Sort.by("id")).stream().map(DictionaryItemDto::from).toList();
    }

    @GetMapping("/genres")
    public List<DictionaryItemDto> genres() {
        return genreRepository.findAll(Sort.by("name")).stream().map(DictionaryItemDto::from).toList();
    }

    @GetMapping("/moods")
    public List<DictionaryItemDto> moods() {
        return moodRepository.findAll(Sort.by("id")).stream().map(DictionaryItemDto::from).toList();
    }

    /** Списки id приходят через запятую: /api/recommend?moodId=1&genreIds=2,5&typeIds=1 */
    @GetMapping("/recommend")
    public List<RecommendationDto> recommend(@RequestParam(required = false) Long moodId,
                                             @RequestParam(required = false) Set<Long> genreIds,
                                             @RequestParam(required = false) Set<Long> typeIds,
                                             @RequestParam(required = false) Set<Long> excludedGenreIds,
                                             @RequestParam(required = false) Set<Long> excludedTypeIds,
                                             @RequestParam(required = false) Integer yearFrom,
                                             @RequestParam(required = false) Integer yearTo) {
        Criteria criteria = new Criteria(moodId, genreIds, typeIds, excludedGenreIds, excludedTypeIds,
                yearFrom, yearTo);
        if (!criteria.hasWishes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Выберите хотя бы что-нибудь: настроение, жанр или тип.");
        }
        return recommendationService.rank(criteria).stream()
                .limit(RecommendationService.LIMIT)
                .map(RecommendationDto::from)
                .toList();
    }
}
