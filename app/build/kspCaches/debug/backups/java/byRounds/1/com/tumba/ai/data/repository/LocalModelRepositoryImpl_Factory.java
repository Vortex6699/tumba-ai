package com.tumba.ai.data.repository;

import android.content.Context;
import com.tumba.ai.core.DispatcherProvider;
import com.tumba.ai.data.local.dao.LocalModelDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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
public final class LocalModelRepositoryImpl_Factory implements Factory<LocalModelRepositoryImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<LocalModelDao> daoProvider;

  private final Provider<DispatcherProvider> dispatchersProvider;

  private final Provider<OkHttpClient> httpClientProvider;

  public LocalModelRepositoryImpl_Factory(Provider<Context> contextProvider,
      Provider<LocalModelDao> daoProvider, Provider<DispatcherProvider> dispatchersProvider,
      Provider<OkHttpClient> httpClientProvider) {
    this.contextProvider = contextProvider;
    this.daoProvider = daoProvider;
    this.dispatchersProvider = dispatchersProvider;
    this.httpClientProvider = httpClientProvider;
  }

  @Override
  public LocalModelRepositoryImpl get() {
    return newInstance(contextProvider.get(), daoProvider.get(), dispatchersProvider.get(), httpClientProvider.get());
  }

  public static LocalModelRepositoryImpl_Factory create(Provider<Context> contextProvider,
      Provider<LocalModelDao> daoProvider, Provider<DispatcherProvider> dispatchersProvider,
      Provider<OkHttpClient> httpClientProvider) {
    return new LocalModelRepositoryImpl_Factory(contextProvider, daoProvider, dispatchersProvider, httpClientProvider);
  }

  public static LocalModelRepositoryImpl newInstance(Context context, LocalModelDao dao,
      DispatcherProvider dispatchers, OkHttpClient httpClient) {
    return new LocalModelRepositoryImpl(context, dao, dispatchers, httpClient);
  }
}
