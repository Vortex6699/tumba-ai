package com.tumba.ai.di;

import com.tumba.ai.data.network.ModelDiscoveryApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

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
public final class NetworkModule_ProvideModelDiscoveryApiFactory implements Factory<ModelDiscoveryApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideModelDiscoveryApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public ModelDiscoveryApi get() {
    return provideModelDiscoveryApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideModelDiscoveryApiFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideModelDiscoveryApiFactory(retrofitProvider);
  }

  public static ModelDiscoveryApi provideModelDiscoveryApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideModelDiscoveryApi(retrofit));
  }
}
