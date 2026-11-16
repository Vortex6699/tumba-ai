package com.tumba.ai.di;

import com.tumba.ai.data.local.TumbaDatabase;
import com.tumba.ai.data.local.dao.AttachmentDao;
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
public final class DatabaseModule_ProvideAttachmentDaoFactory implements Factory<AttachmentDao> {
  private final Provider<TumbaDatabase> databaseProvider;

  public DatabaseModule_ProvideAttachmentDaoFactory(Provider<TumbaDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public AttachmentDao get() {
    return provideAttachmentDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideAttachmentDaoFactory create(
      Provider<TumbaDatabase> databaseProvider) {
    return new DatabaseModule_ProvideAttachmentDaoFactory(databaseProvider);
  }

  public static AttachmentDao provideAttachmentDao(TumbaDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideAttachmentDao(database));
  }
}
