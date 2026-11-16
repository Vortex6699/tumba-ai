package com.tumba.ai.ui.chat;

import androidx.lifecycle.SavedStateHandle;
import com.tumba.ai.data.ai.local.LocalInferenceEngine;
import com.tumba.ai.data.files.AttachmentStore;
import com.tumba.ai.domain.ai.AIRepository;
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
public final class ChatViewModel_Factory implements Factory<ChatViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<ConversationRepository> conversationRepositoryProvider;

  private final Provider<AIRepository> aiRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<ApiKeyRepository> apiKeysProvider;

  private final Provider<AttachmentStore> attachmentStoreProvider;

  private final Provider<LocalInferenceEngine> engineProvider;

  public ChatViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<ConversationRepository> conversationRepositoryProvider,
      Provider<AIRepository> aiRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ApiKeyRepository> apiKeysProvider, Provider<AttachmentStore> attachmentStoreProvider,
      Provider<LocalInferenceEngine> engineProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.conversationRepositoryProvider = conversationRepositoryProvider;
    this.aiRepositoryProvider = aiRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.apiKeysProvider = apiKeysProvider;
    this.attachmentStoreProvider = attachmentStoreProvider;
    this.engineProvider = engineProvider;
  }

  @Override
  public ChatViewModel get() {
    return newInstance(savedStateHandleProvider.get(), conversationRepositoryProvider.get(), aiRepositoryProvider.get(), settingsRepositoryProvider.get(), apiKeysProvider.get(), attachmentStoreProvider.get(), engineProvider.get());
  }

  public static ChatViewModel_Factory create(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<ConversationRepository> conversationRepositoryProvider,
      Provider<AIRepository> aiRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ApiKeyRepository> apiKeysProvider, Provider<AttachmentStore> attachmentStoreProvider,
      Provider<LocalInferenceEngine> engineProvider) {
    return new ChatViewModel_Factory(savedStateHandleProvider, conversationRepositoryProvider, aiRepositoryProvider, settingsRepositoryProvider, apiKeysProvider, attachmentStoreProvider, engineProvider);
  }

  public static ChatViewModel newInstance(SavedStateHandle savedStateHandle,
      ConversationRepository conversationRepository, AIRepository aiRepository,
      SettingsRepository settingsRepository, ApiKeyRepository apiKeys,
      AttachmentStore attachmentStore, LocalInferenceEngine engine) {
    return new ChatViewModel(savedStateHandle, conversationRepository, aiRepository, settingsRepository, apiKeys, attachmentStore, engine);
  }
}
