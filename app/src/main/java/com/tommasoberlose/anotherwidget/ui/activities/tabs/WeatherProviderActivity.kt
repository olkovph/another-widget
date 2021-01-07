package com.tommasoberlose.anotherwidget.ui.activities.tabs

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.BottomSheetWeatherProviderSettings
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper
import com.tommasoberlose.anotherwidget.ui.fragments.MainFragment
import com.tommasoberlose.anotherwidget.ui.viewmodels.WeatherProviderViewModel
import kotlinx.android.synthetic.main.activity_weather_provider.*
import kotlinx.coroutines.launch
import net.idik.lib.slimadapter.SlimAdapter
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class WeatherProviderActivity : AppCompatActivity() {

    private lateinit var adapter: SlimAdapter
    private lateinit var viewModel: WeatherProviderViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_provider)

        viewModel = ViewModelProvider(this).get(WeatherProviderViewModel::class.java)

        list_view.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        list_view.layoutManager = mLayoutManager

        adapter = SlimAdapter.create()
        adapter
            .register<Constants.WeatherProvider>(R.layout.weather_provider_list_item) { provider, injector ->
                injector
                    .text(R.id.text, WeatherHelper.getProviderName(this, provider))
                    .clicked(R.id.item) {
                        if (Preferences.weatherProvider != provider.value) {
                            Preferences.weatherProviderError = "-"
                            Preferences.weatherProviderLocationError = ""
                        }
                        val oldValue = Preferences.weatherProvider
                        Preferences.weatherProvider = provider.value
                        updateListItem(oldValue)
                        updateListItem()
                        loader.isVisible = true

                        lifecycleScope.launch {
                            WeatherHelper.updateWeather(this@WeatherProviderActivity)
                        }
                    }
                    .clicked(R.id.radioButton) {
                        if (Preferences.weatherProvider != provider.value) {
                            Preferences.weatherProviderError = "-"
                            Preferences.weatherProviderLocationError = ""
                        }
                        val oldValue = Preferences.weatherProvider
                        Preferences.weatherProvider = provider.value
                        updateListItem(oldValue)
                        updateListItem()
                        loader.isVisible = true

                        lifecycleScope.launch {
                            WeatherHelper.updateWeather(this@WeatherProviderActivity)
                        }
                    }
                    .checked(R.id.radioButton, provider.value == Preferences.weatherProvider)
                    .with<TextView>(R.id.text2) {
                        if (WeatherHelper.isKeyRequired(provider)) {
                            it.text = getString(R.string.api_key_required_message)
                        }

                        if (provider == Constants.WeatherProvider.WEATHER_GOV) {
                            it.text = getString(R.string.us_only_message)
                        }

                        if (provider == Constants.WeatherProvider.YR) {
                            it.text = getString(R.string.celsius_only_message)
                        }
                    }
                    .clicked(R.id.action_configure) {
                        BottomSheetWeatherProviderSettings(this) {
                            lifecycleScope.launch {
                                loader.isVisible = true
                                WeatherHelper.updateWeather(this@WeatherProviderActivity)
                            }
                        }.show()
                    }
                    .visibility(R.id.action_configure, if (/*WeatherHelper.isKeyRequired(provider) && */provider.value == Preferences.weatherProvider) View.VISIBLE else View.GONE)
                    .with<TextView>(R.id.provider_error) {
                        if (Preferences.weatherProviderError != "" && Preferences.weatherProviderError != "-") {
                            it.text = Preferences.weatherProviderError
                            it.isVisible = provider.value == Preferences.weatherProvider
                        } else if (Preferences.weatherProviderLocationError != "") {
                            it.text = Preferences.weatherProviderLocationError
                            it.isVisible = provider.value == Preferences.weatherProvider
                        } else {
                            it.isVisible = false
                        }
                    }
                    .image(R.id.action_configure, ContextCompat.getDrawable(this, if (WeatherHelper.isKeyRequired(provider)) R.drawable.round_settings_24 else R.drawable.outline_info_24))
            }.attachTo(list_view)

        adapter.updateData(
            Constants.WeatherProvider.values().asList()
                .filter { it != Constants.WeatherProvider.HERE }
                .filter { it != Constants.WeatherProvider.ACCUWEATHER }
        )

        setupListener()
        subscribeUi(viewModel)
    }

    private fun subscribeUi(viewModel: WeatherProviderViewModel) {
        viewModel.weatherProviderError.observe(this) {
            updateListItem()
        }

        viewModel.weatherProviderLocationError.observe(this) {
            updateListItem()
        }
    }

    private fun updateListItem(provider: Int = Preferences.weatherProvider) {
        (adapter.data).forEachIndexed { index, item ->
            if (item is Constants.WeatherProvider && item.value == provider) {
                adapter.notifyItemChanged(index)
            }
        }
    }

    private fun setupListener() {
        action_back.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(ignore: MainFragment.UpdateUiMessageEvent?) {
        loader.isVisible = Preferences.weatherProviderError == "-"
        if (Preferences.weatherProviderError == "" && Preferences.weatherProviderLocationError == "") {
            Snackbar.make(list_view, getString(R.string.settings_weather_provider_api_key_subtitle_all_set), Snackbar.LENGTH_LONG).show()
        }
    }
}