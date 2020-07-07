package com.dermentli;

import java.util.Comparator;

public class QuestionCompLike implements Comparator<Question> {
    @Override
    public int compare(Question o1, Question o2) {
        return o2.getLikes() - o1.getLikes();
    }
}
