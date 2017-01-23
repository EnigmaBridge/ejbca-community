package org.ejbca.core.ejb.vpn;

import java.io.Serializable;

/**
 * Simple Optional allowing null value.
 * Just used to detect if field was set.
 *
 * Created by dusanklinec on 18.01.17.
 */
public class OptionalNull<T> implements Serializable {
    private final T value;
    private final boolean empty;

    /**
     * Returns an empty Optional instance. No value is present for this Optional.
     * @param <T> T - Type of the non-existent value
     * @return an empty Optional
     */
    public static <T> OptionalNull<T> empty(){
        return new OptionalNull<T>();
    }

    /**
     * Returns an Optional with the specified present non-null value.
     * If null is given, exception is thrown.
     *
     * @param value - the value to be present, which must be non-null
     * @param <T> T - the class of the value
     * @return an Optional with the value present
     * @throws NullPointerException - if value is null
     */
    public static <T> OptionalNull<T> of(T value) {
        if (value == null){
            throw new NullPointerException();
        }

        return new OptionalNull<T>(value);
    }

    /**
     * Returns an Optional describing the specified value.
     * Null is permitted and valid value.
     *
     * @param value value - the possibly-null value to describe
     * @param <T> T - the class of the value
     * @return an Optional with a present value if the specified value is non-null, otherwise an empty Optional
     */
    public static <T> OptionalNull<T> ofNullable(T value) {
        return new OptionalNull<T>(value);
    }

    private OptionalNull() {
        this.value = null;
        this.empty = true;
    }

    private OptionalNull(T value) {
        this.value = value;
        this.empty = false;
    }

    /**
     * If a value is present in this Optional, returns the value, otherwise throws NoSuchElementException.
     * @return the non-null value held by this Optional
     */
    public T get(){
        if (empty){
            throw new NullPointerException();
        }

        return value;
    }

    /**
     * Return true if there is a value present, otherwise false.
     * @return true if there is a value present, otherwise false
     */
    public boolean isPresent(){
        return !empty;
    }

    /**
     * Return the value if present, otherwise return other.
     * @param other - the value to be returned if there is no value present, may be null
     * @return the value, if present, otherwise other
     */
    public T orElse(T other){
        return isPresent() ? value : other;
    }

}
