package com.example.cinerec.controller.api;

import com.example.cinerec.entity.Mood;
import com.example.cinerec.repository.MoodRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/moods")
public class AdminMoodApiController extends DictionaryApiController<Mood> {

    public AdminMoodApiController(MoodRepository moodRepository) {
        super(moodRepository, Mood::new, "Настроение");
    }
}
