package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

@FunctionalInterface
public interface Trial<T> {

    <S> S map(Function<T, S> onSuccess, BiFunction<T, RuntimeException, S> onException);

    default T resume() {
        return map(Function.identity(), (value, exception) -> value);
    }

    default T resume(BiConsumer<T, RuntimeException> onException) {
        return map(Function.identity(), (value, exception) -> {
            onException.accept(value, exception);
            return value;
        });
    }

    default BiConsumer<Consumer<T>, BiConsumer<T, RuntimeException>> toConsumer() {
        return (onSuccess, onException) -> map(value -> {
            onSuccess.accept(value);
            return null;
        }, (value, exception) -> {
            onException.accept(value, exception);
            return null;
        });
    }

    default Optional<RuntimeException> toOptional() {
        return Optional.ofNullable(map(value -> null, (value, exception) -> exception));
    }

    static <U> Trial<U> of(U value) {
        return new Trial<>() {
            @Override
            public <S> S map(Function<U, S> onSuccess, BiFunction<U, RuntimeException, S> onException) {
                return onSuccess.apply(value);
            }
        };
    }

    static <U> Trial<U> of(U value, RuntimeException exception) {
        return new Trial<>() {
            @Override
            public <S> S map(Function<U, S> onSuccess, BiFunction<U, RuntimeException, S> onException) {
                return onException.apply(value, exception);
            }
        };
    }
}
