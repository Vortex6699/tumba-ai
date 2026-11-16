package com.tumba.ai.data.ai;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.serialization.json.Json;
import okhttp3.OkHttpClient;

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
public final class AiHttpClient_Factory implements Factory<AiHttpClient> {
  private final Provider<OkHttpClient> clientProvider;

  private final Provider<Json> jsonProvider;

  public AiHttpClient_Factory(Provider<OkHttpClient> clientProvider, Provider<Json> jsonProvider) {
    this.clientProvider = clientProvider;
    this.jsonProvider = jsonProvider;
  }

  @Override
  public AiHttpClient get() {
    return newInstance(clientProvider.get(), jsonProvider.get());
  }

  public static AiHttpClient_Factory create(Provider<OkHttpClient> clientProvider,
      Provider<Json> jsonProvider) {
    return new AiHttpClient_Factory(clientProvider, jsonProvider);
  }

  public static AiHttpClient newInstance(OkHttpClient client, Json json) {
    return new AiHttpClient(client, json);
  }
}
