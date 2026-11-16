package com.tumba.ai.data.ai.providers;

import com.tumba.ai.data.ai.AiHttpClient;
import com.tumba.ai.data.network.ModelDiscoveryApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.serialization.json.Json;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class GeminiProvider_Factory implements Factory<GeminiProvider> {
  private final Provider<AiHttpClient> httpProvider;

  private final Provider<ModelDiscoveryApi> discoveryProvider;

  private final Provider<Json> jsonProvider;

  public GeminiProvider_Factory(Provider<AiHttpClient> httpProvider,
      Provider<ModelDiscoveryApi> discoveryProvider, Provider<Json> jsonProvider) {
    this.httpProvider = httpProvider;
    this.discoveryProvider = discoveryProvider;
    this.jsonProvider = jsonProvider;
  }

  @Override
  public GeminiProvider get() {
    return newInstance(httpProvider.get(), discoveryProvider.get(), jsonProvider.get());
  }

  public static GeminiProvider_Factory create(Provider<AiHttpClient> httpProvider,
      Provider<ModelDiscoveryApi> discoveryProvider, Provider<Json> jsonProvider) {
    return new GeminiProvider_Factory(httpProvider, discoveryProvider, jsonProvider);
  }

  public static GeminiProvider newInstance(AiHttpClient http, ModelDiscoveryApi discovery,
      Json json) {
    return new GeminiProvider(http, discovery, json);
  }
}
