/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.typescript.codegen.auth.http;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.ServiceIndex.AuthSchemeMode;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.typescript.codegen.CodegenUtils;
import software.amazon.smithy.typescript.codegen.TypeScriptDelegator;
import software.amazon.smithy.typescript.codegen.TypeScriptDependency;
import software.amazon.smithy.typescript.codegen.TypeScriptSettings;
import software.amazon.smithy.typescript.codegen.auth.AuthUtils;
import software.amazon.smithy.typescript.codegen.auth.http.HttpAuthOptionProperty.Type;
import software.amazon.smithy.typescript.codegen.integration.TypeScriptIntegration;
import software.amazon.smithy.utils.SmithyInternalApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * feat(experimentalIdentityAndAuth): Generator for {@code HttpAuthSchemeProvider} and corresponding interfaces.
 *
 * Code generated includes:
 *
 * - {@code $ServiceHttpAuthSchemeParameters}
 * - {@code default$ServiceHttpAuthSchemeParametersProvider}
 * - {@code create$AuthSchemeIdHttpAuthOption}
 * - {@code $ServiceHttpAuthSchemeProvider}
 * - {@code default$ServiceHttpAuthSchemeProvider}
 */
@SmithyInternalApi
public class HttpAuthSchemeProviderGenerator implements Runnable {
    private final TypeScriptDelegator delegator;
    private final Model model;

    private final SupportedHttpAuthSchemesIndex authIndex;
    private final ServiceIndex serviceIndex;
    private final ServiceShape serviceShape;
    private final Symbol serviceSymbol;
    private final String serviceName;
    private final Map<String, HttpAuthSchemeParameter> httpAuthSchemeParameters;

    /**
     * Create an HttpAuthSchemeProviderGenerator.
     * @param delegator delegator
     * @param settings settings
     * @param model model
     * @param symbolProvider symbolProvider
     * @param integrations integrations
     */
    public HttpAuthSchemeProviderGenerator(
        TypeScriptDelegator delegator,
        TypeScriptSettings settings,
        Model model,
        SymbolProvider symbolProvider,
        List<TypeScriptIntegration> integrations
    ) {
        this.delegator = delegator;
        this.model = model;

        this.authIndex = new SupportedHttpAuthSchemesIndex(integrations);
        this.serviceIndex = ServiceIndex.of(model);
        this.serviceShape = settings.getService(model);
        this.serviceSymbol = symbolProvider.toSymbol(serviceShape);
        this.serviceName = CodegenUtils.getServiceName(settings, model, symbolProvider);
        this.httpAuthSchemeParameters =
            AuthUtils.collectHttpAuthSchemeParameters(authIndex.getSupportedHttpAuthSchemes().values());
    }

    @Override
    public void run() {
        generateHttpAuthSchemeParametersInterface();
        generateHttpAuthSchemeParametersProviderInterface();
        generateDefaultHttpAuthSchemeParametersProviderFunction();
        generateHttpAuthOptionFunctions();
        generateHttpAuthSchemeProviderInterface();
        generateDefaultHttpAuthSchemeProviderFunction();
    }

    /*
    import { HttpAuthSchemeParameters } from "@smithy/types";

    // ...

    export interface WeatherHttpAuthSchemeParameters extends HttpAuthSchemeParameters {
    }
    */
    private void generateHttpAuthSchemeParametersInterface() {
        delegator.useFileWriter(AuthUtils.HTTP_AUTH_SCHEME_PROVIDER_PATH, w -> {
            w.addDependency(TypeScriptDependency.EXPERIMENTAL_IDENTITY_AND_AUTH);
            w.addImport("HttpAuthSchemeParameters", null, TypeScriptDependency.EXPERIMENTAL_IDENTITY_AND_AUTH);
            w.openBlock("""
                /**
                 * @internal
                 */
                export interface $LHttpAuthSchemeParameters extends HttpAuthSchemeParameters {""", "}",
                serviceName,
                () -> {
                for (HttpAuthSchemeParameter parameter : httpAuthSchemeParameters.values()) {
                    w.write("$L?: $C;", parameter.name(), parameter.type());
                }
            });
        });
    }

    /*
    import { HttpAuthSchemeParametersProvider } from "@smithy/types";
    import { WeatherClientResolvedConfig } from "../WeatherClient";

    // ...

    export interface WeatherHttpAuthSchemeParametersProvider extends
      HttpAuthSchemeParametersProvider<WeatherClientResolvedConfig, WeatherHttpAuthSchemeParameters> {}
    */
    private void generateHttpAuthSchemeParametersProviderInterface() {
        delegator.useFileWriter(AuthUtils.HTTP_AUTH_SCHEME_PROVIDER_PATH, w -> {
            w.addRelativeImport(serviceSymbol.getName() + "ResolvedConfig", null,
                Paths.get(".", serviceSymbol.getNamespace()));
            w.addDependency(TypeScriptDependency.EXPERIMENTAL_IDENTITY_AND_AUTH);
            w.addImport("HttpAuthSchemeParametersProvider", null, TypeScriptDependency.EXPERIMENTAL_IDENTITY_AND_AUTH);
            w.write("""
                /**
                 * @internal
                 */
                export interface $LHttpAuthSchemeParametersProvider extends \
                HttpAuthSchemeParametersProvider<$LResolvedConfig, $LHttpAuthSchemeParameters> {}""",
                serviceName, serviceSymbol.getName(), serviceName);
        });
    }

    /*
    export const defaultWeatherHttpAuthSchemeParametersProvider: WeatherHttpAuthSchemeParametersProvider =
    async (config, context, input) => {
      return {
        operation: context.commandName,
      };
    };
    */
    private void generateDefaultHttpAuthSchemeParametersProviderFunction() {
        delegator.useFileWriter(AuthUtils.HTTP_AUTH_SCHEME_PROVIDER_PATH, w -> {
            w.addDependency(TypeScriptDependency.UTIL_MIDDLEWARE);
            w.addImport("getSmithyContext", null, TypeScriptDependency.UTIL_MIDDLEWARE);
            w.openBlock("""
                /**
                 * @internal
                 */
                export const default$LHttpAuthSchemeParametersProvider: \
                $LHttpAuthSchemeParametersProvider = \
                async (config, context, input) => {""", "};",
                serviceName, serviceName,
                () -> {
                w.openBlock("return {", "};", () -> {
                    w.write("operation: getSmithyContext(context).operation as string,");
                    for (HttpAuthSchemeParameter parameter : httpAuthSchemeParameters.values()) {
                        w.write("$L: $C,", parameter.name(), parameter.source());
                    }
                });
            });
        });
    }

    private void generateHttpAuthOptionFunctions() {
        Map<ShapeId, HttpAuthScheme> effectiveAuthSchemes =
            AuthUtils.getAllEffectiveNoAuthAwareAuthSchemes(serviceShape, serviceIndex, authIndex);
        for (Entry<ShapeId, HttpAuthScheme> entry : effectiveAuthSchemes.entrySet()) {
            generateHttpAuthOptionFunction(entry.getKey(), entry.getValue());
        }
    }

    /*
    import { HttpAuthOption } from "@smithy/types";

    // ...

    function createSmithyApiHttpApiKeyAuthHttpAuthOption(authParameters: WeatherHttpAuthSchemeParameters):
    HttpAuthOption[] {
        return {
            schemeId: "smithy.api#httpApiKeyAuth",
            signingProperties: {
                name: "Authorization",
                in: HttpApiKeyAuthLocation.HEADER,
                scheme: "",
            },
        };
    };
    */
    private void generateHttpAuthOptionFunction(ShapeId shapeId, HttpAuthScheme authScheme) {
        delegator.useFileWriter(AuthUtils.HTTP_AUTH_SCHEME_PROVIDER_PATH, w -> {
            String normalizedAuthSchemeName = normalizeAuthSchemeName(shapeId);
            w.addDependency(TypeScriptDependency.EXPERIMENTAL_IDENTITY_AND_AUTH);
            w.addImport("HttpAuthOption", null, TypeScriptDependency.EXPERIMENTAL_IDENTITY_AND_AUTH);
            w.openBlock("""
                function create$LHttpAuthOption(authParameters: $LHttpAuthSchemeParameters): \
                HttpAuthOption {""", "};",
                normalizedAuthSchemeName, serviceName,
                () -> {
                w.openBlock("return {", "};", () -> {
                    w.write("schemeId: $S,", shapeId.toString());
                    // If no HttpAuthScheme is registered, there are no HttpAuthOptionProperties available.
                    if (authScheme == null) {
                        return;
                    }
                    Trait trait = serviceShape.findTrait(authScheme.getTraitId()).orElse(null);
                    List<HttpAuthOptionProperty> identityProperties =
                        authScheme.getAuthSchemeOptionParametersByType(Type.IDENTITY);
                    if (!identityProperties.isEmpty()) {
                        w.openBlock("identityProperties: {", "},", () -> {
                            for (HttpAuthOptionProperty parameter : identityProperties) {
                                w.write("$L: $C,", parameter.name(), parameter.source().apply(trait));
                            }
                        });
                    }
                    List<HttpAuthOptionProperty> signingProperties =
                        authScheme.getAuthSchemeOptionParametersByType(Type.SIGNING);
                    if (!signingProperties.isEmpty()) {
                        w.openBlock("signingProperties: {", "},", () -> {
                            for (HttpAuthOptionProperty parameter : signingProperties) {
                                w.write("$L: $C,", parameter.name(), parameter.source().apply(trait));
                            }
                        });
                    }
                });
            });
        });
    }

    private static String normalizeAuthSchemeName(ShapeId shapeId) {
        return String.join("", Arrays
            .asList(shapeId.toString().split("[.#]"))
            .stream().map(StringUtils::capitalize)
            .toList());
    }

    /*
    import { HttpAuthSchemeProvider } from "@smithy/types";

    // ...

    export interface WeatherHttpAuthSchemeProvider extends HttpAuthSchemeProvider<WeatherHttpAuthSchemeParameters> {}
    */
    private void generateHttpAuthSchemeProviderInterface() {
        delegator.useFileWriter(AuthUtils.HTTP_AUTH_SCHEME_PROVIDER_PATH, w -> {
            w.addDependency(TypeScriptDependency.EXPERIMENTAL_IDENTITY_AND_AUTH);
            w.addImport("HttpAuthSchemeProvider", null, TypeScriptDependency.EXPERIMENTAL_IDENTITY_AND_AUTH);
            w.write("""
            /**
             * @internal
             */
            export interface $LHttpAuthSchemeProvider extends HttpAuthSchemeProvider<$LHttpAuthSchemeParameters> {}
            """, serviceName, serviceName);
        });
    }

    /*
    export const defaultWeatherHttpAuthSchemeProvider: WeatherHttpAuthSchemeProvider =
    (authParameters) => {
        const options: HttpAuthOption[] = [];
        switch (authParameters.operation) {
            default: {
                options.push(createSmithyApiHttpApiKeyAuthHttpAuthOption(authParameters));
            };
        };
        return options;
    };
    */
    private void generateDefaultHttpAuthSchemeProviderFunction() {
        delegator.useFileWriter(AuthUtils.HTTP_AUTH_SCHEME_PROVIDER_PATH, w -> {
            w.openBlock("""
            /**
             * @internal
             */
            export const default$LHttpAuthSchemeProvider: $LHttpAuthSchemeProvider = \
            (authParameters) => {""", "};",
            serviceName, serviceName, () -> {
                w.write("const options: HttpAuthOption[] = [];");
                w.openBlock("switch (authParameters.operation) {", "};", () -> {
                    var serviceAuthSchemes = serviceIndex.getEffectiveAuthSchemes(
                        serviceShape, AuthSchemeMode.NO_AUTH_AWARE);
                    for (ShapeId operationShapeId : serviceShape.getAllOperations()) {
                        var operationAuthSchemes = serviceIndex.getEffectiveAuthSchemes(
                            serviceShape, operationShapeId, AuthSchemeMode.NO_AUTH_AWARE);
                        // Skip operation generation if operation auth schemes are equivalent to the default service
                        // auth schemes.
                        if (serviceAuthSchemes.equals(operationAuthSchemes)) {
                            continue;
                        }
                        w.openBlock("case $S: {", "};", operationShapeId.getName(), () -> {
                            operationAuthSchemes.keySet().forEach(shapeId -> {
                                w.write("options.push(create$LHttpAuthOption(authParameters));",
                                    normalizeAuthSchemeName(shapeId));
                            });
                            w.write("break;");
                        });
                    }
                    w.openBlock("default: {", "};", () -> {
                        serviceAuthSchemes.keySet().forEach(shapeId -> {
                            w.write("options.push(create$LHttpAuthOption(authParameters));",
                                normalizeAuthSchemeName(shapeId));
                        });
                    });
                });
                w.write("return options;");
            });
        });
    }
}
