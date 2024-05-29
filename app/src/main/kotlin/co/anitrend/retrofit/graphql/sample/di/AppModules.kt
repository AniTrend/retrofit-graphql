package co.anitrend.retrofit.graphql.sample.di

import android.net.ConnectivityManager
import co.anitrend.arch.core.provider.SupportFileProvider
import co.anitrend.arch.extension.ext.systemServiceOf
import co.anitrend.arch.extension.network.SupportConnectivity
import co.anitrend.arch.ui.view.widget.model.StateLayoutConfig
import co.anitrend.retrofit.graphql.core.koin.coreModules
import co.anitrend.retrofit.graphql.data.arch.koin.dataModules
import co.anitrend.retrofit.graphql.sample.R
import co.anitrend.retrofit.graphql.sample.presenter.BucketPresenter
import co.anitrend.retrofit.graphql.sample.presenter.MainPresenter
import co.anitrend.retrofit.graphql.sample.view.MainScreen
import co.anitrend.retrofit.graphql.sample.view.content.bucket.BucketContent
import co.anitrend.retrofit.graphql.sample.view.content.bucket.ui.adapter.BucketAdapter
import co.anitrend.retrofit.graphql.sample.view.content.bucket.viewmodel.BucketViewModel
import co.anitrend.retrofit.graphql.sample.view.content.bucket.viewmodel.UploadViewModel
import co.anitrend.retrofit.graphql.sample.view.content.market.MarketPlaceContent
import co.anitrend.retrofit.graphql.sample.view.content.market.ui.adapter.MarketPlaceAdapter
import co.anitrend.retrofit.graphql.sample.view.content.market.viewmodel.MarketPlaceViewModel
import co.anitrend.retrofit.graphql.sample.viewmodel.MainViewModel
import io.wax911.emojify.EmojiManager
import io.wax911.emojify.serializer.kotlinx.KotlinxDeserializer
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.fragment.dsl.fragment
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

private val appModule = module {
    factory {
        SupportFileProvider()
    }
    single {
        StateLayoutConfig(
            loadingDrawable = null,
            errorDrawable = null,
            loadingMessage = null,
            retryAction = R.string.action_retry
        )
    }
    single {
        SupportConnectivity(
            androidApplication()
                .systemServiceOf<ConnectivityManager>()
        )
    }
    single(createdAtStart = true) {
        EmojiManager.create(
            context = androidApplication(),
            serializer = KotlinxDeserializer()
        )
    }
}

private val viewModelModule = module {
    viewModel {
        MarketPlaceViewModel(
            useCase = get()
        )
    }
    viewModel {
        MainViewModel(
            useCase = get()
        )
    }
    viewModel {
        BucketViewModel(
            useCase = get(),
        )
    }
    viewModel {
        UploadViewModel(
            useCase = get(),
        )
    }
}

private val presenterModule = module {
    scope<MainScreen> {
        scoped {
            MainPresenter(
                context = androidContext(),
                settings = get(),
                stateLayoutConfig = get(),
                emojiManager = get(),
            )
        }
    }
    scope<BucketContent> {
        scoped {
            BucketPresenter(
                context = androidContext(),
                settings = get(),
                emojiManager = get(),
            )
        }
    }
}

private val fragmentModule = module {
    scope<MainScreen> {
        fragment {
            val stateConfig = get<StateLayoutConfig>()
            MarketPlaceContent(
                stateConfig = stateConfig,
                supportViewAdapter = MarketPlaceAdapter(
                    resources = androidContext().resources,
                    stateConfiguration = stateConfig
                ),
            )
        }
    }
    scope<MainScreen> {
        fragment {
            val stateConfig = get<StateLayoutConfig>()
            BucketContent(
                stateConfig = stateConfig,
                supportViewAdapter = BucketAdapter(
                    resources = androidContext().resources,
                    stateConfiguration = stateConfig
                )
            )
        }
    }
}

val appModules = listOf(
    appModule, viewModelModule, presenterModule, fragmentModule
) + dataModules + coreModules