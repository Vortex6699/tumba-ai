package com.tumba.ai.ui.settings;

import android.content.Context;
import com.tumba.ai.domain.repository.ApiKeyRepository;
import com.tumba.ai.domain.repository.ConversationRepository;
import com.tumba.ai.domain.repository.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<ApiKeyRepository> apiKeysProvider;

  private final Provider<ConversationRepository> conversationRepositoryProvider;

  public SettingsViewModel_Factory(Provider<Context> contextProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ApiKeyRepository> apiKeysProvider,
      Provider<ConversationRepository> conversationRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.apiKeysProvider = apiKeysProvider;
    this.conversationRepositoryProvider = conversationRepositoryProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(contextProvider.get(), settingsRepositoryProvider.get(), apiKeysProvider.get(), conversationRepositoryProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<Context> contextProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ApiKeyRepository> apiKeysProvider,
      Provider<ConversationRepository> conversationRepositoryProvider) {
    return new SettingsViewModel_Factory(contextProvider, settingsRepositoryProvider, apiKeysProvider, conversationRepositoryProvider);
  }

  public static SettingsViewModel newInstance(Context context,
      SettingsRepository settingsRepository, ApiKeyRepository apiKeys,
      ConversationRepository conversationRepository) {
    return new SettingsViewModel(context, settingsRepository, apiKeys, conversationRepository);
  }
}
