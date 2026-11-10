package team.gothaj.client.api.manager;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class ListManager<T> {

    protected final List<T> items = new ArrayList<>();

    public void init() {}

    public void add(T item) {
        if (!items.contains(item)) {
            items.add(item);
        }
    }

    public void remove(T item) {
        items.remove(item);
    }

    public void clear() {
        items.clear();
    }

    public int size() {
        return items.size();
    }
}
