package com.example.cinerec.repository;

import com.example.cinerec.entity.MovieType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieTypeRepository extends JpaRepository<MovieType, Long> {
}
