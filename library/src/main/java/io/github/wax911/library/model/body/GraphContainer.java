package io.github.wax911.library.model.body;

import java.util.List;

import io.github.wax911.library.model.attribute.GraphError;

public class GraphContainer<T> {

    private T data;
    private List<GraphError> errors;

    public T getData() {
        return data;
    }

    public List<GraphError> getErrors() {
        return errors;
    }

    public boolean isEmpty() {
        return data == null;
    }
}
