package com.tumba.ai.data.ai.providers;

import com.tumba.ai.data.ai.local.LocalInferenceEngine;
import com.tumba.ai.domain.repository.LocalModelRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class LocalProvider_Factory implements Factory<LocalProvider> {
  private final Provider<LocalInferenceEngine> engineProvider;

  private final Provider<LocalModelRepository> localModelsProvider;

  public LocalProvider_Factory(Provider<LocalInferenceEngine> engineProvider,
      Provider<LocalModelRepository> localModelsProvider) {
    this.engineProvider = engineProvider;
    this.localModelsProvider = localModelsProvider;
  }

  @Override
  public LocalProvider get() {
    return newInstance(engineProvider.get(), localModelsProvider.get());
  }

  public static LocalProvider_Factory create(Provider<LocalInferenceEngine> engineProvider,
      Provider<LocalModelRepository> localModelsProvider) {
    return new LocalProvider_Factory(engineProvider, localModelsProvider);
  }

  public static LocalProvider newInstance(LocalInferenceEngine engine,
      LocalModelRepository localModels) {
    return new LocalProvider(engine, localModels);
  }
}
