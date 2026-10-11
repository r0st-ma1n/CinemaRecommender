package com.example.cinerec.repository;

import com.example.cinerec.entity.Mood;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoodRepository extends JpaRepository<Mood, Long> {
}
