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

}
