/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.typescript.codegen.auth.http;

import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.typescript.codegen.TypeScriptWriter;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Definition of an HttpAuthOptionProperty.
 *
 * @param name name of the auth option property
 * @param type the type of {@link Type}
 * @param source a function that provides the auth trait to a writer, and writes
 *  properties from the trait or from {@code authParameters}.
 */
@SmithyUnstableApi
public final record HttpAuthOptionProperty(
    String name,
    Type type,
    Function<Trait, Consumer<TypeScriptWriter>> source
) implements ToSmithyBuilder<HttpAuthOptionProperty> {
    /**
     * Defines the type of the auth option property.
     */
    public enum Type {
        /**
         * Specifies the property should be included in {@code identityProperties}.
         */
        IDENTITY,
        /**
         * Specifies the property should be included in {@code signingProperties}.
         */
        SIGNING
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public SmithyBuilder<HttpAuthOptionProperty> toBuilder() {
        return builder()
            .name(name)
            .type(type)
            .source(source);
    }

    public static final class Builder implements SmithyBuilder<HttpAuthOptionProperty> {
        private String name;
        private Type type;
        private Function<Trait, Consumer<TypeScriptWriter>> source;

        @Override
        public HttpAuthOptionProperty build() {
            return new HttpAuthOptionProperty(
                SmithyBuilder.requiredState("name", name),
                SmithyBuilder.requiredState("type", type),
                SmithyBuilder.requiredState("source", source));
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        public Builder source(Function<Trait, Consumer<TypeScriptWriter>> source) {
            this.source = source;
            return this;
        }
    }
}
