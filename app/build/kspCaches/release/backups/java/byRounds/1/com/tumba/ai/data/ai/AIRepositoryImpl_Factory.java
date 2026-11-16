package com.tumba.ai.data.ai;

import com.tumba.ai.domain.repository.ApiKeyRepository;
import com.tumba.ai.domain.repository.CustomProviderRepository;
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
public final class AIRepositoryImpl_Factory implements Factory<AIRepositoryImpl> {
  private final Provider<ProviderRegistry> registryProvider;

  private final Provider<ApiKeyRepository> apiKeysProvider;

  private final Provider<CustomProviderRepository> customProvidersProvider;

  private final Provider<LocalModelRepository> localModelsProvider;

  public AIRepositoryImpl_Factory(Provider<ProviderRegistry> registryProvider,
      Provider<ApiKeyRepository> apiKeysProvider,
      Provider<CustomProviderRepository> customProvidersProvider,
      Provider<LocalModelRepository> localModelsProvider) {
    this.registryProvider = registryProvider;
    this.apiKeysProvider = apiKeysProvider;
    this.customProvidersProvider = customProvidersProvider;
    this.localModelsProvider = localModelsProvider;
  }

  @Override
  public AIRepositoryImpl get() {
    return newInstance(registryProvider.get(), apiKeysProvider.get(), customProvidersProvider.get(), localModelsProvider.get());
  }

  public static AIRepositoryImpl_Factory create(Provider<ProviderRegistry> registryProvider,
      Provider<ApiKeyRepository> apiKeysProvider,
      Provider<CustomProviderRepository> customProvidersProvider,
      Provider<LocalModelRepository> localModelsProvider) {
    return new AIRepositoryImpl_Factory(registryProvider, apiKeysProvider, customProvidersProvider, localModelsProvider);
  }

  public static AIRepositoryImpl newInstance(ProviderRegistry registry, ApiKeyRepository apiKeys,
      CustomProviderRepository customProviders, LocalModelRepository localModels) {
    return new AIRepositoryImpl(registry, apiKeys, customProviders, localModels);
  }
}
