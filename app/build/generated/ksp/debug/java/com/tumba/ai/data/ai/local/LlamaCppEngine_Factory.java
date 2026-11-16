package com.tumba.ai.data.ai.local;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class LlamaCppEngine_Factory implements Factory<LlamaCppEngine> {
  private final Provider<Context> contextProvider;

  public LlamaCppEngine_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public LlamaCppEngine get() {
    return newInstance(contextProvider.get());
  }

  public static LlamaCppEngine_Factory create(Provider<Context> contextProvider) {
    return new LlamaCppEngine_Factory(contextProvider);
  }

  public static LlamaCppEngine newInstance(Context context) {
    return new LlamaCppEngine(context);
  }
}
