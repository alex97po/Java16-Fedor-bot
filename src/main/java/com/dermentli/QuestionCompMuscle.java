package com.dermentli;

import java.util.Comparator;

public class QuestionCompMuscle implements Comparator<Question> {
    @Override
    public int compare(Question o1, Question o2) {
        return o2.getDifficulty() - o1.getDifficulty();
    }
}
