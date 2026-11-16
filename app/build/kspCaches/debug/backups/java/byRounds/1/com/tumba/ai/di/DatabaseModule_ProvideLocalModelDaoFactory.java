package com.tumba.ai.di;

import com.tumba.ai.data.local.TumbaDatabase;
import com.tumba.ai.data.local.dao.LocalModelDao;
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
public final class DatabaseModule_ProvideLocalModelDaoFactory implements Factory<LocalModelDao> {
  private final Provider<TumbaDatabase> databaseProvider;

  public DatabaseModule_ProvideLocalModelDaoFactory(Provider<TumbaDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public LocalModelDao get() {
    return provideLocalModelDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideLocalModelDaoFactory create(
      Provider<TumbaDatabase> databaseProvider) {
    return new DatabaseModule_ProvideLocalModelDaoFactory(databaseProvider);
  }

  public static LocalModelDao provideLocalModelDao(TumbaDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideLocalModelDao(database));
  }
}
