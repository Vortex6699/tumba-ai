package com.tumba.ai.di;

import com.tumba.ai.data.local.TumbaDatabase;
import com.tumba.ai.data.local.dao.CustomProviderDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class DatabaseModule_ProvideCustomProviderDaoFactory implements Factory<CustomProviderDao> {
  private final Provider<TumbaDatabase> databaseProvider;

  public DatabaseModule_ProvideCustomProviderDaoFactory(Provider<TumbaDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public CustomProviderDao get() {
    return provideCustomProviderDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideCustomProviderDaoFactory create(
      Provider<TumbaDatabase> databaseProvider) {
    return new DatabaseModule_ProvideCustomProviderDaoFactory(databaseProvider);
  }

  public static CustomProviderDao provideCustomProviderDao(TumbaDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideCustomProviderDao(database));
  }
}
