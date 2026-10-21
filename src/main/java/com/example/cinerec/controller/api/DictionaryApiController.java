package com.example.cinerec.controller.api;

import com.example.cinerec.controller.admin.DictionaryNames;
import com.example.cinerec.entity.NamedEntity;
import com.example.cinerec.repository.DictionaryRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.function.Supplier;

/**
 * Добавление, переименование и удаление записей справочника через API.
 * Жанры и настроения устроены одинаково, поэтому вся логика здесь,
 * а наследники задают только адрес, репозиторий и слово для сообщений.
 * При удалении связи с фильмами удаляет сама база (ON DELETE CASCADE).
 */
public abstract class DictionaryApiController<T extends NamedEntity> {

    public record NameRequest(String name) {
    }

    private final DictionaryRepository<T> repository;
    private final Supplier<T> factory;
    private final String label;

    protected DictionaryApiController(DictionaryRepository<T> repository, Supplier<T> factory, String label) {
        this.repository = repository;
        this.factory = factory;
        this.label = label;
    }

    @Operation(summary = "Добавить")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DictionaryItemDto create(@RequestBody NameRequest request) {
        String name = checkName(request.name());
        if (repository.existsByNameIgnoreCase(name)) {
            throw alreadyExists(name);
        }
        T item = factory.get();
        item.setName(name);
        return DictionaryItemDto.from(repository.save(item));
    }

    @Operation(summary = "Переименовать")
    @PutMapping("/{id}")
    public DictionaryItemDto rename(@PathVariable Long id, @RequestBody NameRequest request) {
        String name = checkName(request.name());
        T item = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись не найдена"));
        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw alreadyExists(name);
        }
        item.setName(name);
        return DictionaryItemDto.from(repository.save(item));
    }

    @Operation(summary = "Удалить (связи с фильмами удаляет база)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }

    private static String checkName(String name) {
        String error = DictionaryNames.check(name);
        if (error != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }
        return name.trim();
    }

    private ResponseStatusException alreadyExists(String name) {
        return new ResponseStatusException(HttpStatus.CONFLICT, label + " «" + name + "» уже есть");
    }
}
