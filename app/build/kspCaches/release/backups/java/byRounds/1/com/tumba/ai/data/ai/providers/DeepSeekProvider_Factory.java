package com.tumba.ai.data.ai.providers;

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
public final class DeepSeekProvider_Factory implements Factory<DeepSeekProvider> {
  private final Provider<OpenAICompatibleProvider> delegateProvider;

  public DeepSeekProvider_Factory(Provider<OpenAICompatibleProvider> delegateProvider) {
    this.delegateProvider = delegateProvider;
  }

  @Override
  public DeepSeekProvider get() {
    return newInstance(delegateProvider.get());
  }

  public static DeepSeekProvider_Factory create(
      Provider<OpenAICompatibleProvider> delegateProvider) {
    return new DeepSeekProvider_Factory(delegateProvider);
  }

  public static DeepSeekProvider newInstance(OpenAICompatibleProvider delegate) {
    return new DeepSeekProvider(delegate);
  }
}
