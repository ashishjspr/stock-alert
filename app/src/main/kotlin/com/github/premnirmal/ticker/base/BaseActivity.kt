package com.github.premnirmal.ticker.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.IStocksProvider.FetchState
import com.github.premnirmal.ticker.showDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by premnirmal on 2/26/16.
 */
abstract class BaseActivity<T: ViewBinding> : AppCompatActivity() {

  abstract val simpleName: String
  abstract val binding: T
  open val subscribeToErrorEvents = true
  private var isErrorDialogShowing = false
  private val holder: InjectionHolder by lazy { InjectionHolder() }

  override fun onCreate(
      savedInstanceState: Bundle?
  ) {
    super.onCreate(savedInstanceState)
    setContentView(binding.root)
    savedInstanceState?.let { isErrorDialogShowing = it.getBoolean(IS_ERROR_DIALOG_SHOWING, false) }
  }

  override fun onPostCreate(savedInstanceState: Bundle?) {
    super.onPostCreate(savedInstanceState)
    holder.analytics.trackScreenView(simpleName, this)
  }

  override fun onResume() {
    super.onResume()
    if (subscribeToErrorEvents) {
      lifecycleScope.launch {
        holder.stocksProvider.fetchState.collect { state ->
          if (state is FetchState.Failure) {
            if (this.isActive && !isErrorDialogShowing && !isFinishing) {
              isErrorDialogShowing = true
              showDialog(state.displayString).setOnDismissListener { isErrorDialogShowing = false }
              delay(500L)
            }
          }
        }
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(IS_ERROR_DIALOG_SHOWING, isErrorDialogShowing)
  }

  override fun onRestoreInstanceState(savedInstanceState: Bundle) {
    try {
      super.onRestoreInstanceState(savedInstanceState)
    } catch (ex: Throwable) {
      // android bug
      Timber.w(ex)
    }
  }

  override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      onBackPressed()
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  companion object {
    private const val IS_ERROR_DIALOG_SHOWING = "IS_ERROR_DIALOG_SHOWING"
  }

  class InjectionHolder {
    @Inject internal lateinit var analytics: Analytics
    @Inject internal lateinit var stocksProvider: IStocksProvider

    init {
      Injector.appComponent.inject(this)
    }
  }
}
