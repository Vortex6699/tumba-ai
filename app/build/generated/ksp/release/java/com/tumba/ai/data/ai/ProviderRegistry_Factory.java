package com.tumba.ai.data.ai;

import com.tumba.ai.data.ai.providers.DeepSeekProvider;
import com.tumba.ai.data.ai.providers.GeminiProvider;
import com.tumba.ai.data.ai.providers.LocalProvider;
import com.tumba.ai.data.ai.providers.OpenAICompatibleProvider;
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
public final class ProviderRegistry_Factory implements Factory<ProviderRegistry> {
  private final Provider<GeminiProvider> geminiProvider;

  private final Provider<DeepSeekProvider> deepSeekProvider;

  private final Provider<OpenAICompatibleProvider> openAiCompatibleProvider;

  private final Provider<LocalProvider> localProvider;

  public ProviderRegistry_Factory(Provider<GeminiProvider> geminiProvider,
      Provider<DeepSeekProvider> deepSeekProvider,
      Provider<OpenAICompatibleProvider> openAiCompatibleProvider,
      Provider<LocalProvider> localProvider) {
    this.geminiProvider = geminiProvider;
    this.deepSeekProvider = deepSeekProvider;
    this.openAiCompatibleProvider = openAiCompatibleProvider;
    this.localProvider = localProvider;
  }

  @Override
  public ProviderRegistry get() {
    return newInstance(geminiProvider.get(), deepSeekProvider.get(), openAiCompatibleProvider.get(), localProvider.get());
  }

  public static ProviderRegistry_Factory create(Provider<GeminiProvider> geminiProvider,
      Provider<DeepSeekProvider> deepSeekProvider,
      Provider<OpenAICompatibleProvider> openAiCompatibleProvider,
      Provider<LocalProvider> localProvider) {
    return new ProviderRegistry_Factory(geminiProvider, deepSeekProvider, openAiCompatibleProvider, localProvider);
  }

  public static ProviderRegistry newInstance(GeminiProvider gemini, DeepSeekProvider deepSeek,
      OpenAICompatibleProvider openAiCompatible, LocalProvider local) {
    return new ProviderRegistry(gemini, deepSeek, openAiCompatible, local);
  }
}
