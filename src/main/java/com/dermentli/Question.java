package com.dermentli;

import lombok.Data;

@Data
public class Question {
    private int id;
    private String language;
    private String subject;
    private String content;
    private String answer;
    private int muscle;
    private int likes;

    public Question() {

    }

    public Question(int id, String language, String subject, int muscle, int likes) {
        this.id = id;
        this.language = language;
        this.subject = subject;
        this.muscle = muscle;
        this.likes = likes;
    }
}
