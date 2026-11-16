package com.tumba.ai.data.repository;

import com.tumba.ai.core.DispatcherProvider;
import com.tumba.ai.data.local.dao.CustomProviderDao;
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
public final class CustomProviderRepositoryImpl_Factory implements Factory<CustomProviderRepositoryImpl> {
  private final Provider<CustomProviderDao> daoProvider;

  private final Provider<DispatcherProvider> dispatchersProvider;

  public CustomProviderRepositoryImpl_Factory(Provider<CustomProviderDao> daoProvider,
      Provider<DispatcherProvider> dispatchersProvider) {
    this.daoProvider = daoProvider;
    this.dispatchersProvider = dispatchersProvider;
  }

  @Override
  public CustomProviderRepositoryImpl get() {
    return newInstance(daoProvider.get(), dispatchersProvider.get());
  }

  public static CustomProviderRepositoryImpl_Factory create(Provider<CustomProviderDao> daoProvider,
      Provider<DispatcherProvider> dispatchersProvider) {
    return new CustomProviderRepositoryImpl_Factory(daoProvider, dispatchersProvider);
  }

  public static CustomProviderRepositoryImpl newInstance(CustomProviderDao dao,
      DispatcherProvider dispatchers) {
    return new CustomProviderRepositoryImpl(dao, dispatchers);
  }
}
