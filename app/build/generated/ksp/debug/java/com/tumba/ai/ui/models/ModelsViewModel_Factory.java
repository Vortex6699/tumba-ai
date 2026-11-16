package com.tumba.ai.ui.models;

import com.tumba.ai.data.ai.local.LocalInferenceEngine;
import com.tumba.ai.domain.ai.AIRepository;
import com.tumba.ai.domain.repository.ApiKeyRepository;
import com.tumba.ai.domain.repository.CustomProviderRepository;
import com.tumba.ai.domain.repository.LocalModelRepository;
import com.tumba.ai.domain.repository.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class ModelsViewModel_Factory implements Factory<ModelsViewModel> {
  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<AIRepository> aiRepositoryProvider;

  private final Provider<CustomProviderRepository> customProviderRepositoryProvider;

  private final Provider<LocalModelRepository> localModelRepositoryProvider;

  private final Provider<ApiKeyRepository> apiKeysProvider;

  private final Provider<LocalInferenceEngine> engineProvider;

  public ModelsViewModel_Factory(Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<AIRepository> aiRepositoryProvider,
      Provider<CustomProviderRepository> customProviderRepositoryProvider,
      Provider<LocalModelRepository> localModelRepositoryProvider,
      Provider<ApiKeyRepository> apiKeysProvider, Provider<LocalInferenceEngine> engineProvider) {
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.aiRepositoryProvider = aiRepositoryProvider;
    this.customProviderRepositoryProvider = customProviderRepositoryProvider;
    this.localModelRepositoryProvider = localModelRepositoryProvider;
    this.apiKeysProvider = apiKeysProvider;
    this.engineProvider = engineProvider;
  }

  @Override
  public ModelsViewModel get() {
    return newInstance(settingsRepositoryProvider.get(), aiRepositoryProvider.get(), customProviderRepositoryProvider.get(), localModelRepositoryProvider.get(), apiKeysProvider.get(), engineProvider.get());
  }

  public static ModelsViewModel_Factory create(
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<AIRepository> aiRepositoryProvider,
      Provider<CustomProviderRepository> customProviderRepositoryProvider,
      Provider<LocalModelRepository> localModelRepositoryProvider,
      Provider<ApiKeyRepository> apiKeysProvider, Provider<LocalInferenceEngine> engineProvider) {
    return new ModelsViewModel_Factory(settingsRepositoryProvider, aiRepositoryProvider, customProviderRepositoryProvider, localModelRepositoryProvider, apiKeysProvider, engineProvider);
  }

  public static ModelsViewModel newInstance(SettingsRepository settingsRepository,
      AIRepository aiRepository, CustomProviderRepository customProviderRepository,
      LocalModelRepository localModelRepository, ApiKeyRepository apiKeys,
      LocalInferenceEngine engine) {
    return new ModelsViewModel(settingsRepository, aiRepository, customProviderRepository, localModelRepository, apiKeys, engine);
  }
}
