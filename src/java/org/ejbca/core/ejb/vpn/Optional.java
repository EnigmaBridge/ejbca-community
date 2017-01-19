package org.ejbca.core.ejb.vpn;

/**
 * Simple Optional - if there is no guava and java8, we use this.
 *
 * Created by dusanklinec on 18.01.17.
 */
public class Optional<T> {
    private final T value;
    private final boolean empty;

    /**
     * Returns an empty Optional instance. No value is present for this Optional.
     * @param <T> T - Type of the non-existent value
     * @return an empty Optional
     */
    public static <T> Optional<T>empty(){
        return new Optional<T>();
    }

    /**
     * Returns an Optional with the specified present non-null value.
     * @param value - the value to be present, which must be non-null
     * @param <T> T - the class of the value
     * @return an Optional with the value present
     * @throws NullPointerException - if value is null
     */
    public static <T> Optional<T> of(T value) {
        if (value == null){
            throw new NullPointerException();
        }

        return new Optional<T>(value);
    }

    /**
     * Returns an Optional describing the specified value, if non-null, otherwise returns an empty Optional.
     * @param value value - the possibly-null value to describe
     * @param <T> T - the class of the value
     * @return an Optional with a present value if the specified value is non-null, otherwise an empty Optional
     */
    public static <T> Optional<T> ofNullable(T value) {
        if (value == null){
            return Optional.<T>empty();
        }

        final Optional<T> opt = new Optional<>(value);
        return opt;
    }

    private Optional() {
        this.value = null;
        this.empty = true;
    }

    private Optional(T value) {
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
