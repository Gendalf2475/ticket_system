package com.github.henriquemb.ticketsystem.util;

import com.github.henriquemb.ticketsystem.exceptions.PaginationException;

import java.util.ArrayList;
import java.util.List;

public class Pagination<T> {
    private final List<T> items;
    private final int max;

    public Pagination(List<T> items, int max) {
        this.items = items;
        this.max = max;
    }

    public int length() {
        return (int) Math.ceil(items.size() / (float) max);
    }

    public List<T> getPag(int value) throws PaginationException {
        if (value > length() || value < 0) throw new PaginationException();

        List<T> pag = new ArrayList<>();

        int startIndex = (max * value) - (max);
        int endIndex = Math.min(startIndex + max, items.size());

        for (int i = startIndex; i < endIndex; i++) {
            pag.add(items.get(i));
        }

        return pag;
    }
}
