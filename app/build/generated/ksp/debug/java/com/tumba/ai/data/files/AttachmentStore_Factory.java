package com.tumba.ai.data.files;

import android.content.Context;
import com.tumba.ai.core.DispatcherProvider;
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
public final class AttachmentStore_Factory implements Factory<AttachmentStore> {
  private final Provider<Context> contextProvider;

  private final Provider<DispatcherProvider> dispatchersProvider;

  public AttachmentStore_Factory(Provider<Context> contextProvider,
      Provider<DispatcherProvider> dispatchersProvider) {
    this.contextProvider = contextProvider;
    this.dispatchersProvider = dispatchersProvider;
  }

  @Override
  public AttachmentStore get() {
    return newInstance(contextProvider.get(), dispatchersProvider.get());
  }

  public static AttachmentStore_Factory create(Provider<Context> contextProvider,
      Provider<DispatcherProvider> dispatchersProvider) {
    return new AttachmentStore_Factory(contextProvider, dispatchersProvider);
  }

  public static AttachmentStore newInstance(Context context, DispatcherProvider dispatchers) {
    return new AttachmentStore(context, dispatchers);
  }
}
