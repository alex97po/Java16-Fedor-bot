package com.dermentli;

import lombok.Data;

@Data
public class Question {
    private int id;
    private String language;
    private String subject;
    private String content;
    private String answer;
    private int difficulty;
    private int likes;

    public Question() {

    }

    public Question(int id, String language, String subject, int difficulty, int likes) {
        this.id = id;
        this.language = language;
        this.subject = subject;
        this.difficulty = difficulty;
        this.likes = likes;
    }
}
