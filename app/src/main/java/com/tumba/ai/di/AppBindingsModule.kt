package com.tumba.ai.di

import com.tumba.ai.core.DefaultDispatcherProvider
import com.tumba.ai.core.DispatcherProvider
import com.tumba.ai.data.ai.AIRepositoryImpl
import com.tumba.ai.data.ai.local.LlamaCppEngine
import com.tumba.ai.data.ai.local.LocalInferenceEngine
import com.tumba.ai.data.repository.ApiKeyRepositoryImpl
import com.tumba.ai.data.repository.ConversationRepositoryImpl
import com.tumba.ai.data.repository.CustomProviderRepositoryImpl
import com.tumba.ai.data.repository.LocalModelRepositoryImpl
import com.tumba.ai.data.repository.SettingsRepositoryImpl
import com.tumba.ai.data.security.KeystoreSecretStore
import com.tumba.ai.data.security.SecretStore
import com.tumba.ai.domain.ai.AIRepository
import com.tumba.ai.domain.repository.ApiKeyRepository
import com.tumba.ai.domain.repository.ConversationRepository
import com.tumba.ai.domain.repository.CustomProviderRepository
import com.tumba.ai.domain.repository.LocalModelRepository
import com.tumba.ai.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindingsModule {

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider

    @Binds
    @Singleton
    abstract fun bindSecretStore(impl: KeystoreSecretStore): SecretStore

    @Binds
    @Singleton
    abstract fun bindConversationRepository(
        impl: ConversationRepositoryImpl,
    ): ConversationRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindApiKeyRepository(impl: ApiKeyRepositoryImpl): ApiKeyRepository

    @Binds
    @Singleton
    abstract fun bindCustomProviderRepository(
        impl: CustomProviderRepositoryImpl,
    ): CustomProviderRepository

    @Binds
    @Singleton
    abstract fun bindLocalModelRepository(impl: LocalModelRepositoryImpl): LocalModelRepository

    @Binds
    @Singleton
    abstract fun bindAiRepository(impl: AIRepositoryImpl): AIRepository

    @Binds
    @Singleton
    abstract fun bindLocalInferenceEngine(impl: LlamaCppEngine): LocalInferenceEngine
}
