package com.dermentli;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class User {
    long idUser;
    List<Question> ratedQuestions = new ArrayList<>();

    public User(long idUser) {
        this.idUser = idUser;
    }

    public User(long idUser, int id, String language, String subject, int difficulty, int likes) {
        this.idUser = idUser;
        ratedQuestions.add(new Question(id, language, subject, difficulty, likes));
    }
}
