package com.tumba.ai.data.security;

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
public final class KeystoreSecretStore_Factory implements Factory<KeystoreSecretStore> {
  private final Provider<Context> contextProvider;

  public KeystoreSecretStore_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public KeystoreSecretStore get() {
    return newInstance(contextProvider.get());
  }

  public static KeystoreSecretStore_Factory create(Provider<Context> contextProvider) {
    return new KeystoreSecretStore_Factory(contextProvider);
  }

  public static KeystoreSecretStore newInstance(Context context) {
    return new KeystoreSecretStore(context);
  }
}
