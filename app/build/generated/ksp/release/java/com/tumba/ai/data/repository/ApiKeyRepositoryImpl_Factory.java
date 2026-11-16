package com.tumba.ai.data.repository;

import com.tumba.ai.core.DispatcherProvider;
import com.tumba.ai.data.security.SecretStore;
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
public final class ApiKeyRepositoryImpl_Factory implements Factory<ApiKeyRepositoryImpl> {
  private final Provider<SecretStore> storeProvider;

  private final Provider<DispatcherProvider> dispatchersProvider;

  public ApiKeyRepositoryImpl_Factory(Provider<SecretStore> storeProvider,
      Provider<DispatcherProvider> dispatchersProvider) {
    this.storeProvider = storeProvider;
    this.dispatchersProvider = dispatchersProvider;
  }

  @Override
  public ApiKeyRepositoryImpl get() {
    return newInstance(storeProvider.get(), dispatchersProvider.get());
  }

  public static ApiKeyRepositoryImpl_Factory create(Provider<SecretStore> storeProvider,
      Provider<DispatcherProvider> dispatchersProvider) {
    return new ApiKeyRepositoryImpl_Factory(storeProvider, dispatchersProvider);
  }

  public static ApiKeyRepositoryImpl newInstance(SecretStore store,
      DispatcherProvider dispatchers) {
    return new ApiKeyRepositoryImpl(store, dispatchers);
  }
}
