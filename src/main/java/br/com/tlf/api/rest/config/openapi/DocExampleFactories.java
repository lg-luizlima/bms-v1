package br.com.tlf.api.rest.config.openapi;

import java.util.function.Supplier;

/** Reflective instantiation shared by every doc-example customizer in this package. */
final class DocExampleFactories {

    private DocExampleFactories() {}

    @SuppressWarnings("unchecked")
    static <T> T instantiate(Class<? extends Supplier<?>> type) {
        try {
            return ((Supplier<T>) type.getDeclaredConstructor().newInstance()).get();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate " + type.getName()
                    + " — it needs a public no-arg constructor", e);
        }
    }
}
