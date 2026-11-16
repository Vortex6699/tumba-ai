package com.tumba.ai.data.repository;

import com.tumba.ai.core.DispatcherProvider;
import com.tumba.ai.data.local.dao.AttachmentDao;
import com.tumba.ai.data.local.dao.ConversationDao;
import com.tumba.ai.data.local.dao.MessageDao;
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
public final class ConversationRepositoryImpl_Factory implements Factory<ConversationRepositoryImpl> {
  private final Provider<ConversationDao> conversationDaoProvider;

  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<AttachmentDao> attachmentDaoProvider;

  private final Provider<DispatcherProvider> dispatchersProvider;

  public ConversationRepositoryImpl_Factory(Provider<ConversationDao> conversationDaoProvider,
      Provider<MessageDao> messageDaoProvider, Provider<AttachmentDao> attachmentDaoProvider,
      Provider<DispatcherProvider> dispatchersProvider) {
    this.conversationDaoProvider = conversationDaoProvider;
    this.messageDaoProvider = messageDaoProvider;
    this.attachmentDaoProvider = attachmentDaoProvider;
    this.dispatchersProvider = dispatchersProvider;
  }

  @Override
  public ConversationRepositoryImpl get() {
    return newInstance(conversationDaoProvider.get(), messageDaoProvider.get(), attachmentDaoProvider.get(), dispatchersProvider.get());
  }

  public static ConversationRepositoryImpl_Factory create(
      Provider<ConversationDao> conversationDaoProvider, Provider<MessageDao> messageDaoProvider,
      Provider<AttachmentDao> attachmentDaoProvider,
      Provider<DispatcherProvider> dispatchersProvider) {
    return new ConversationRepositoryImpl_Factory(conversationDaoProvider, messageDaoProvider, attachmentDaoProvider, dispatchersProvider);
  }

  public static ConversationRepositoryImpl newInstance(ConversationDao conversationDao,
      MessageDao messageDao, AttachmentDao attachmentDao, DispatcherProvider dispatchers) {
    return new ConversationRepositoryImpl(conversationDao, messageDao, attachmentDao, dispatchers);
  }
}
