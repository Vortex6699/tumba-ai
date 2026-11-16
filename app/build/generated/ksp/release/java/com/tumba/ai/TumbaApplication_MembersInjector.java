package com.tumba.ai;

import com.tumba.ai.data.ai.local.LocalInferenceEngine;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class TumbaApplication_MembersInjector implements MembersInjector<TumbaApplication> {
  private final Provider<LocalInferenceEngine> localEngineProvider;

  public TumbaApplication_MembersInjector(Provider<LocalInferenceEngine> localEngineProvider) {
    this.localEngineProvider = localEngineProvider;
  }

  public static MembersInjector<TumbaApplication> create(
      Provider<LocalInferenceEngine> localEngineProvider) {
    return new TumbaApplication_MembersInjector(localEngineProvider);
  }

  @Override
  public void injectMembers(TumbaApplication instance) {
    injectLocalEngine(instance, localEngineProvider.get());
  }

  @InjectedFieldSignature("com.tumba.ai.TumbaApplication.localEngine")
  public static void injectLocalEngine(TumbaApplication instance,
      LocalInferenceEngine localEngine) {
    instance.localEngine = localEngine;
  }
}
