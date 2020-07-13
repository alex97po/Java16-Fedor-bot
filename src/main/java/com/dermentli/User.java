package com.dermentli;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Data
@NoArgsConstructor
public class User {

    private long idUser;
    private List<Question> ratedQuestions = new ArrayList<>();

    public User(long idUser) {
        this.idUser = idUser;
    }

}
