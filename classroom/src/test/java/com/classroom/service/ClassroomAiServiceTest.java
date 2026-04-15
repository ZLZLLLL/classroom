package com.classroom.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ClassroomAiServiceTest {

    @Test
    void shouldPickTeacherPromptForTeacherRole() {
        String prompt = ClassroomAiService.pickRolePrompt(1, "default", "teacher", "student");
        assertEquals("teacher", prompt);
    }

    @Test
    void shouldPickStudentPromptForStudentRole() {
        String prompt = ClassroomAiService.pickRolePrompt(2, "default", "teacher", "student");
        assertEquals("student", prompt);
    }

    @Test
    void shouldFallbackToDefaultPromptForUnknownRole() {
        String prompt = ClassroomAiService.pickRolePrompt(3, "default", "teacher", "student");
        assertEquals("default", prompt);
    }

    @Test
    void shouldFallbackToBuiltInWhenDefaultPromptBlank() {
        String prompt = ClassroomAiService.pickRolePrompt(null, "   ", "teacher", "student");
        assertFalse(prompt.isBlank());
    }
}


