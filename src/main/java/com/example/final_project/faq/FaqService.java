package com.example.final_project.faq;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class FaqService {

    private final FaqRepository faqRepository;
}
