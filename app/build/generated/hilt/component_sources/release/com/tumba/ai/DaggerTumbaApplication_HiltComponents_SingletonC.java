package com.tumba.ai;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.tumba.ai.core.DefaultDispatcherProvider;
import com.tumba.ai.core.DispatcherProvider;
import com.tumba.ai.data.ai.AIRepositoryImpl;
import com.tumba.ai.data.ai.AiHttpClient;
import com.tumba.ai.data.ai.ProviderRegistry;
import com.tumba.ai.data.ai.local.LlamaCppEngine;
import com.tumba.ai.data.ai.providers.DeepSeekProvider;
import com.tumba.ai.data.ai.providers.GeminiProvider;
import com.tumba.ai.data.ai.providers.LocalProvider;
import com.tumba.ai.data.ai.providers.OpenAICompatibleProvider;
import com.tumba.ai.data.files.AttachmentStore;
import com.tumba.ai.data.local.TumbaDatabase;
import com.tumba.ai.data.local.dao.AttachmentDao;
import com.tumba.ai.data.local.dao.ConversationDao;
import com.tumba.ai.data.local.dao.CustomProviderDao;
import com.tumba.ai.data.local.dao.LocalModelDao;
import com.tumba.ai.data.local.dao.MessageDao;
import com.tumba.ai.data.network.ModelDiscoveryApi;
import com.tumba.ai.data.repository.ApiKeyRepositoryImpl;
import com.tumba.ai.data.repository.ConversationRepositoryImpl;
import com.tumba.ai.data.repository.CustomProviderRepositoryImpl;
import com.tumba.ai.data.repository.LocalModelRepositoryImpl;
import com.tumba.ai.data.repository.SettingsRepositoryImpl;
import com.tumba.ai.data.security.KeystoreSecretStore;
import com.tumba.ai.di.DatabaseModule_ProvideAttachmentDaoFactory;
import com.tumba.ai.di.DatabaseModule_ProvideConversationDaoFactory;
import com.tumba.ai.di.DatabaseModule_ProvideCustomProviderDaoFactory;
import com.tumba.ai.di.DatabaseModule_ProvideDatabaseFactory;
import com.tumba.ai.di.DatabaseModule_ProvideLocalModelDaoFactory;
import com.tumba.ai.di.DatabaseModule_ProvideMessageDaoFactory;
import com.tumba.ai.di.NetworkModule_ProvideJsonFactory;
import com.tumba.ai.di.NetworkModule_ProvideModelDiscoveryApiFactory;
import com.tumba.ai.di.NetworkModule_ProvideOkHttpClientFactory;
import com.tumba.ai.di.NetworkModule_ProvideRetrofitFactory;
import com.tumba.ai.ui.chat.ChatViewModel;
import com.tumba.ai.ui.chat.ChatViewModel_HiltModules;
import com.tumba.ai.ui.history.HistoryViewModel;
import com.tumba.ai.ui.history.HistoryViewModel_HiltModules;
import com.tumba.ai.ui.models.ModelsViewModel;
import com.tumba.ai.ui.models.ModelsViewModel_HiltModules;
import com.tumba.ai.ui.settings.SettingsViewModel;
import com.tumba.ai.ui.settings.SettingsViewModel_HiltModules;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import kotlinx.serialization.json.Json;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

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
public final class DaggerTumbaApplication_HiltComponents_SingletonC {
  private DaggerTumbaApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public TumbaApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements TumbaApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public TumbaApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements TumbaApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public TumbaApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements TumbaApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public TumbaApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements TumbaApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public TumbaApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements TumbaApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public TumbaApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements TumbaApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public TumbaApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements TumbaApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public TumbaApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends TumbaApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends TumbaApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends TumbaApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends TumbaApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(MapBuilder.<String, Boolean>newMapBuilder(4).put(LazyClassKeyProvider.com_tumba_ai_ui_chat_ChatViewModel, ChatViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_tumba_ai_ui_history_HistoryViewModel, HistoryViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_tumba_ai_ui_models_ModelsViewModel, ModelsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_tumba_ai_ui_settings_SettingsViewModel, SettingsViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_tumba_ai_ui_chat_ChatViewModel = "com.tumba.ai.ui.chat.ChatViewModel";

      static String com_tumba_ai_ui_settings_SettingsViewModel = "com.tumba.ai.ui.settings.SettingsViewModel";

      static String com_tumba_ai_ui_history_HistoryViewModel = "com.tumba.ai.ui.history.HistoryViewModel";

      static String com_tumba_ai_ui_models_ModelsViewModel = "com.tumba.ai.ui.models.ModelsViewModel";

      @KeepFieldType
      ChatViewModel com_tumba_ai_ui_chat_ChatViewModel2;

      @KeepFieldType
      SettingsViewModel com_tumba_ai_ui_settings_SettingsViewModel2;

      @KeepFieldType
      HistoryViewModel com_tumba_ai_ui_history_HistoryViewModel2;

      @KeepFieldType
      ModelsViewModel com_tumba_ai_ui_models_ModelsViewModel2;
    }
  }

  private static final class ViewModelCImpl extends TumbaApplication_HiltComponents.ViewModelC {
    private final SavedStateHandle savedStateHandle;

    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<ChatViewModel> chatViewModelProvider;

    private Provider<HistoryViewModel> historyViewModelProvider;

    private Provider<ModelsViewModel> modelsViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.savedStateHandle = savedStateHandleParam;
      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.chatViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.historyViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.modelsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(4).put(LazyClassKeyProvider.com_tumba_ai_ui_chat_ChatViewModel, ((Provider) chatViewModelProvider)).put(LazyClassKeyProvider.com_tumba_ai_ui_history_HistoryViewModel, ((Provider) historyViewModelProvider)).put(LazyClassKeyProvider.com_tumba_ai_ui_models_ModelsViewModel, ((Provider) modelsViewModelProvider)).put(LazyClassKeyProvider.com_tumba_ai_ui_settings_SettingsViewModel, ((Provider) settingsViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_tumba_ai_ui_settings_SettingsViewModel = "com.tumba.ai.ui.settings.SettingsViewModel";

      static String com_tumba_ai_ui_history_HistoryViewModel = "com.tumba.ai.ui.history.HistoryViewModel";

      static String com_tumba_ai_ui_models_ModelsViewModel = "com.tumba.ai.ui.models.ModelsViewModel";

      static String com_tumba_ai_ui_chat_ChatViewModel = "com.tumba.ai.ui.chat.ChatViewModel";

      @KeepFieldType
      SettingsViewModel com_tumba_ai_ui_settings_SettingsViewModel2;

      @KeepFieldType
      HistoryViewModel com_tumba_ai_ui_history_HistoryViewModel2;

      @KeepFieldType
      ModelsViewModel com_tumba_ai_ui_models_ModelsViewModel2;

      @KeepFieldType
      ChatViewModel com_tumba_ai_ui_chat_ChatViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.tumba.ai.ui.chat.ChatViewModel 
          return (T) new ChatViewModel(viewModelCImpl.savedStateHandle, singletonCImpl.conversationRepositoryImplProvider.get(), singletonCImpl.aIRepositoryImplProvider.get(), singletonCImpl.settingsRepositoryImplProvider.get(), singletonCImpl.apiKeyRepositoryImplProvider.get(), singletonCImpl.attachmentStoreProvider.get(), singletonCImpl.llamaCppEngineProvider.get());

          case 1: // com.tumba.ai.ui.history.HistoryViewModel 
          return (T) new HistoryViewModel(singletonCImpl.conversationRepositoryImplProvider.get());

          case 2: // com.tumba.ai.ui.models.ModelsViewModel 
          return (T) new ModelsViewModel(singletonCImpl.settingsRepositoryImplProvider.get(), singletonCImpl.aIRepositoryImplProvider.get(), singletonCImpl.customProviderRepositoryImplProvider.get(), singletonCImpl.localModelRepositoryImplProvider.get(), singletonCImpl.apiKeyRepositoryImplProvider.get(), singletonCImpl.llamaCppEngineProvider.get());

          case 3: // com.tumba.ai.ui.settings.SettingsViewModel 
          return (T) new SettingsViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.settingsRepositoryImplProvider.get(), singletonCImpl.apiKeyRepositoryImplProvider.get(), singletonCImpl.conversationRepositoryImplProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends TumbaApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends TumbaApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends TumbaApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<LlamaCppEngine> llamaCppEngineProvider;

    private Provider<TumbaDatabase> provideDatabaseProvider;

    private Provider<DefaultDispatcherProvider> defaultDispatcherProvider;

    private Provider<DispatcherProvider> bindDispatcherProvider;

    private Provider<ConversationRepositoryImpl> conversationRepositoryImplProvider;

    private Provider<OkHttpClient> provideOkHttpClientProvider;

    private Provider<Json> provideJsonProvider;

    private Provider<AiHttpClient> aiHttpClientProvider;

    private Provider<Retrofit> provideRetrofitProvider;

    private Provider<ModelDiscoveryApi> provideModelDiscoveryApiProvider;

    private Provider<GeminiProvider> geminiProvider;

    private Provider<OpenAICompatibleProvider> openAICompatibleProvider;

    private Provider<DeepSeekProvider> deepSeekProvider;

    private Provider<LocalModelRepositoryImpl> localModelRepositoryImplProvider;

    private Provider<LocalProvider> localProvider;

    private Provider<ProviderRegistry> providerRegistryProvider;

    private Provider<KeystoreSecretStore> keystoreSecretStoreProvider;

    private Provider<ApiKeyRepositoryImpl> apiKeyRepositoryImplProvider;

    private Provider<CustomProviderRepositoryImpl> customProviderRepositoryImplProvider;

    private Provider<AIRepositoryImpl> aIRepositoryImplProvider;

    private Provider<SettingsRepositoryImpl> settingsRepositoryImplProvider;

    private Provider<AttachmentStore> attachmentStoreProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private ConversationDao conversationDao() {
      return DatabaseModule_ProvideConversationDaoFactory.provideConversationDao(provideDatabaseProvider.get());
    }

    private MessageDao messageDao() {
      return DatabaseModule_ProvideMessageDaoFactory.provideMessageDao(provideDatabaseProvider.get());
    }

    private AttachmentDao attachmentDao() {
      return DatabaseModule_ProvideAttachmentDaoFactory.provideAttachmentDao(provideDatabaseProvider.get());
    }

    private LocalModelDao localModelDao() {
      return DatabaseModule_ProvideLocalModelDaoFactory.provideLocalModelDao(provideDatabaseProvider.get());
    }

    private CustomProviderDao customProviderDao() {
      return DatabaseModule_ProvideCustomProviderDaoFactory.provideCustomProviderDao(provideDatabaseProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.llamaCppEngineProvider = DoubleCheck.provider(new SwitchingProvider<LlamaCppEngine>(singletonCImpl, 0));
      this.provideDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<TumbaDatabase>(singletonCImpl, 2));
      this.defaultDispatcherProvider = new SwitchingProvider<>(singletonCImpl, 3);
      this.bindDispatcherProvider = DoubleCheck.provider((Provider) defaultDispatcherProvider);
      this.conversationRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<ConversationRepositoryImpl>(singletonCImpl, 1));
      this.provideOkHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 8));
      this.provideJsonProvider = DoubleCheck.provider(new SwitchingProvider<Json>(singletonCImpl, 9));
      this.aiHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<AiHttpClient>(singletonCImpl, 7));
      this.provideRetrofitProvider = DoubleCheck.provider(new SwitchingProvider<Retrofit>(singletonCImpl, 11));
      this.provideModelDiscoveryApiProvider = DoubleCheck.provider(new SwitchingProvider<ModelDiscoveryApi>(singletonCImpl, 10));
      this.geminiProvider = DoubleCheck.provider(new SwitchingProvider<GeminiProvider>(singletonCImpl, 6));
      this.openAICompatibleProvider = DoubleCheck.provider(new SwitchingProvider<OpenAICompatibleProvider>(singletonCImpl, 13));
      this.deepSeekProvider = DoubleCheck.provider(new SwitchingProvider<DeepSeekProvider>(singletonCImpl, 12));
      this.localModelRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<LocalModelRepositoryImpl>(singletonCImpl, 15));
      this.localProvider = DoubleCheck.provider(new SwitchingProvider<LocalProvider>(singletonCImpl, 14));
      this.providerRegistryProvider = DoubleCheck.provider(new SwitchingProvider<ProviderRegistry>(singletonCImpl, 5));
      this.keystoreSecretStoreProvider = DoubleCheck.provider(new SwitchingProvider<KeystoreSecretStore>(singletonCImpl, 17));
      this.apiKeyRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<ApiKeyRepositoryImpl>(singletonCImpl, 16));
      this.customProviderRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<CustomProviderRepositoryImpl>(singletonCImpl, 18));
      this.aIRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<AIRepositoryImpl>(singletonCImpl, 4));
      this.settingsRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<SettingsRepositoryImpl>(singletonCImpl, 19));
      this.attachmentStoreProvider = DoubleCheck.provider(new SwitchingProvider<AttachmentStore>(singletonCImpl, 20));
    }

    @Override
    public void injectTumbaApplication(TumbaApplication tumbaApplication) {
      injectTumbaApplication2(tumbaApplication);
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private TumbaApplication injectTumbaApplication2(TumbaApplication instance) {
      TumbaApplication_MembersInjector.injectLocalEngine(instance, llamaCppEngineProvider.get());
      return instance;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.tumba.ai.data.ai.local.LlamaCppEngine 
          return (T) new LlamaCppEngine(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 1: // com.tumba.ai.data.repository.ConversationRepositoryImpl 
          return (T) new ConversationRepositoryImpl(singletonCImpl.conversationDao(), singletonCImpl.messageDao(), singletonCImpl.attachmentDao(), singletonCImpl.bindDispatcherProvider.get());

          case 2: // com.tumba.ai.data.local.TumbaDatabase 
          return (T) DatabaseModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 3: // com.tumba.ai.core.DefaultDispatcherProvider 
          return (T) new DefaultDispatcherProvider();

          case 4: // com.tumba.ai.data.ai.AIRepositoryImpl 
          return (T) new AIRepositoryImpl(singletonCImpl.providerRegistryProvider.get(), singletonCImpl.apiKeyRepositoryImplProvider.get(), singletonCImpl.customProviderRepositoryImplProvider.get(), singletonCImpl.localModelRepositoryImplProvider.get());

          case 5: // com.tumba.ai.data.ai.ProviderRegistry 
          return (T) new ProviderRegistry(singletonCImpl.geminiProvider.get(), singletonCImpl.deepSeekProvider.get(), singletonCImpl.openAICompatibleProvider.get(), singletonCImpl.localProvider.get());

          case 6: // com.tumba.ai.data.ai.providers.GeminiProvider 
          return (T) new GeminiProvider(singletonCImpl.aiHttpClientProvider.get(), singletonCImpl.provideModelDiscoveryApiProvider.get(), singletonCImpl.provideJsonProvider.get());

          case 7: // com.tumba.ai.data.ai.AiHttpClient 
          return (T) new AiHttpClient(singletonCImpl.provideOkHttpClientProvider.get(), singletonCImpl.provideJsonProvider.get());

          case 8: // okhttp3.OkHttpClient 
          return (T) NetworkModule_ProvideOkHttpClientFactory.provideOkHttpClient();

          case 9: // kotlinx.serialization.json.Json 
          return (T) NetworkModule_ProvideJsonFactory.provideJson();

          case 10: // com.tumba.ai.data.network.ModelDiscoveryApi 
          return (T) NetworkModule_ProvideModelDiscoveryApiFactory.provideModelDiscoveryApi(singletonCImpl.provideRetrofitProvider.get());

          case 11: // retrofit2.Retrofit 
          return (T) NetworkModule_ProvideRetrofitFactory.provideRetrofit(singletonCImpl.provideOkHttpClientProvider.get());

          case 12: // com.tumba.ai.data.ai.providers.DeepSeekProvider 
          return (T) new DeepSeekProvider(singletonCImpl.openAICompatibleProvider.get());

          case 13: // com.tumba.ai.data.ai.providers.OpenAICompatibleProvider 
          return (T) new OpenAICompatibleProvider(singletonCImpl.aiHttpClientProvider.get(), singletonCImpl.provideModelDiscoveryApiProvider.get(), singletonCImpl.provideJsonProvider.get());

          case 14: // com.tumba.ai.data.ai.providers.LocalProvider 
          return (T) new LocalProvider(singletonCImpl.llamaCppEngineProvider.get(), singletonCImpl.localModelRepositoryImplProvider.get());

          case 15: // com.tumba.ai.data.repository.LocalModelRepositoryImpl 
          return (T) new LocalModelRepositoryImpl(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.localModelDao(), singletonCImpl.bindDispatcherProvider.get(), singletonCImpl.provideOkHttpClientProvider.get());

          case 16: // com.tumba.ai.data.repository.ApiKeyRepositoryImpl 
          return (T) new ApiKeyRepositoryImpl(singletonCImpl.keystoreSecretStoreProvider.get(), singletonCImpl.bindDispatcherProvider.get());

          case 17: // com.tumba.ai.data.security.KeystoreSecretStore 
          return (T) new KeystoreSecretStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 18: // com.tumba.ai.data.repository.CustomProviderRepositoryImpl 
          return (T) new CustomProviderRepositoryImpl(singletonCImpl.customProviderDao(), singletonCImpl.bindDispatcherProvider.get());

          case 19: // com.tumba.ai.data.repository.SettingsRepositoryImpl 
          return (T) new SettingsRepositoryImpl(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 20: // com.tumba.ai.data.files.AttachmentStore 
          return (T) new AttachmentStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.bindDispatcherProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
