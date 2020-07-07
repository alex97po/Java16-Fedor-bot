package com.dermentli;

import lombok.Data;

@Data
public class Question {
    private int id;
    private String content;
    private String answer;
    private int difficulty;
    private int likes;

}
