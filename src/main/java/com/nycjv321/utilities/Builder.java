package com.nycjv321.utilities;

/**
 * Abstract Builder class that allows for inheritance
 * @param <T> Type to build
 * @param <B> Builder Implementation Parent
 */
public abstract class Builder<T, B extends Builder<T, B>> {
    private T entity;

    protected T getEntity() {
        return entity;
    }

    protected void setEntity(T entity) {
        this.entity = entity;
    }

    protected abstract B getThis();

    public T build() {
        return entity;
    }

}