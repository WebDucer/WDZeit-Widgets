/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Eugen [WebDucer] Richter
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.webducer.android.oss.zeiterfassung.widgets.config;

import android.appwidget.AppWidgetManager;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import de.webducer.android.oss.zeiterfassung.widgets.Constants;
import de.webducer.android.oss.zeiterfassung.widgets.R;
import de.webducer.android.oss.zeiterfassung.widgets.provider.StatisticWidgetProvider;
import de.webducer.android.utilities.LogHelper;
import de.webducer.android.utilities.TextHelper;
import de.webducer.android.zeiterfassung.contract.TimeTrackingContract;

import static de.webducer.android.zeiterfassung.contract.TimeTrackingContract.Permissions;
import static de.webducer.android.zeiterfassung.contract.TimeTrackingContract.ReportData.Statistic;
import static de.webducer.android.zeiterfassung.contract.TimeTrackingContract.ReportData.StatisticData;


/**
 * The configuration screen for the {@link StatisticWidgetProvider StatisticWidgetProvider} AppWidget.
 */
public class StatisticWidgetConfigureActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {
   private final static String _TAG = TextHelper.getTag(Constants.TAG_PREFIX, StatisticWidgetConfigureActivity.class.getSimpleName());
   private final static int _REQUIRED_MIN_VERSION = 23;
   // Adapter
   private final static int _STATISTIC_LOADER_ID = 100;
   private int _widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
   // UI elements
   private Spinner _statisticSelector;
   private Button _applyButton;
   private TextView _description;
   private TextView _statisticSelectorLabel;
   // Listener
   private View.OnClickListener _buttonAction;
   private SimpleCursorAdapter _statisticAdapter;

   @Override
   public void onCreate(Bundle bundle) {
      LogHelper.d(_TAG, "Entry: onCreate(bundle: %s)", bundle);

      super.onCreate(bundle);

      // Set the result to CANCELED.  This will cause the widget host to cancel
      // out of the widget placement if the user presses the back button.
      setResult(RESULT_CANCELED);

      setContentView(R.layout.statistic_widget_configure);

      // Find the widget id from the intent.
      Intent intent = getIntent();
      Bundle extras = intent.getExtras();
      if (extras != null) {
         _widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
      }

      // If this activity was started with an intent without an app widget ID, finish with an error.
      if (_widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
         finish();
         return;
      }

      // Init UI elements
      _statisticSelector = (Spinner) findViewById(R.id.StatisticSelector);
      _applyButton = (Button) findViewById(R.id.ApplyButton);
      _description = (TextView) findViewById(R.id.DescriptionText);
      _statisticSelectorLabel = (TextView) findViewById(R.id.StatisticSelectorLabel);

      LogHelper.d(_TAG, "Exit: onCreate");
   }

   @Override
   protected void onPause() {
      LogHelper.d(_TAG, "Entry: onPause()");

      super.onPause();

      _applyButton.setOnClickListener(null);
      _statisticSelector.setAdapter(null);

      LogHelper.d(_TAG, "Exit: onPause");
   }

   @Override
   protected void onResume() {
      LogHelper.d(_TAG, "Entry: onResume()");

      super.onResume();

      _applyButton.setOnClickListener(_buttonAction);
      _statisticSelector.setAdapter(_statisticAdapter);

      LogHelper.d(_TAG, "Exit: onResume");
   }

   @Override
   protected void onStart() {
      LogHelper.d(_TAG, "Entry: onStart()");

      super.onStart();

      // Check prerequisites
      if (checkContentProviderAndAppVersion() == false) {
         // App not installed or too old
         intCancellationScreen();
      } else if (checkPermissions() == false) {
         // Widget installed before app
         initReinstallScreen();
      } else {
         // OK
         getSupportLoaderManager().restartLoader(_STATISTIC_LOADER_ID, null, this);
         initConfigurationScreen();
      }

      LogHelper.d(_TAG, "Exit: onStart");
   }

   private void initConfigurationScreen() {
      LogHelper.d(_TAG, "Entry: initConfigurationScreen()");

      // Init listener
      _buttonAction = new OnApplyButtonClicked();

      _statisticAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, null, new String[] {StatisticData.Columns.CAPTION}, new int[] {android.R.id.text1}, SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

      LogHelper.d(_TAG, "Exit: initConfigurationScreen");
   }

   private void intCancellationScreen() {
      LogHelper.d(_TAG, "Entry: intCancellationScreen()");

      // Hide statistic selector
      _statisticSelector.setVisibility(View.GONE);
      _statisticSelectorLabel.setVisibility(View.GONE);

      // Init listener
      _buttonAction = new OnCancelButtonClicked();

      // Init labels
      _applyButton.setText(R.string.cancel_button);
      _description.setText(R.string.statistic_widget_app_not_installed);

      LogHelper.d(_TAG, "Exit: intCancellationScreen");
   }

   private void initReinstallScreen() {
      LogHelper.d(_TAG, "Entry: initReinstallScreen()");

      // Hide statistic selector
      _statisticSelector.setVisibility(View.GONE);
      _statisticSelectorLabel.setVisibility(View.GONE);

      // Init listener
      _buttonAction = new OnCancelButtonClicked();

      // Init labels
      _applyButton.setText(R.string.cancel_button);
      _description.setText(R.string.statistic_widget_permission_no_permission);

      LogHelper.d(_TAG, "Exit: initReinstallScreen");
   }

   private boolean checkPermissions() {
      LogHelper.d(_TAG, "Entry: checkPermissions()");

      boolean hasPermission = this.checkCallingOrSelfPermission(Permissions.PERMISSION_READ_REPORT) != PackageManager.PERMISSION_DENIED;

      LogHelper.d(_TAG, "Exit: checkPermissions -> %s", hasPermission);

      return hasPermission;
   }

   private boolean checkContentProviderAndAppVersion() {
      LogHelper.d(_TAG, "Entry: checkContentProviderAndAppVersion()");

      PackageInfo info = null;
      ContentProviderClient client = getContentResolver().acquireContentProviderClient(TimeTrackingContract.AUTHORITY);
      try {
         info = this.getPackageManager().getPackageInfo(de.webducer.android.zeiterfassung.contract.Constants.CORE_PACKAGE_NAME, 0);
      } catch (PackageManager.NameNotFoundException e) {
         e.printStackTrace();
      }

      boolean hasRightVersion = client != null && info != null && info.versionCode >= _REQUIRED_MIN_VERSION;

      LogHelper.d(_TAG, "Exit: checkContentProviderAndAppVersion -> %s", hasRightVersion);

      return hasRightVersion;
   }

   @Override
   public Loader<Cursor> onCreateLoader(int id, Bundle args) {
      LogHelper.d(_TAG, "Entry: onCreateLoader(id: %s, args: %s)", id, args);

      CursorLoader loader = null;

      switch (id) {
         case _STATISTIC_LOADER_ID:
            loader = new CursorLoader(this, Statistic.CONTENT_URI, new String[] {BaseColumns._ID, Statistic.Columns.CAPTION}, null, null, Statistic.Columns.CAPTION + " ASC");
            break;
      }

      LogHelper.d(_TAG, "Exit: onCreateLoader -> %s", loader);

      return loader;
   }

   @Override
   public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
      LogHelper.d(_TAG, "Entry: onLoadFinished(loader: %s, data: %s)", loader, data);

      switch (loader.getId()) {
         case _STATISTIC_LOADER_ID:
            initStatisticSelection(data);
            break;
      }

      LogHelper.d(_TAG, "Exit: onLoadFinished");
   }

   @Override
   public void onLoaderReset(Loader<Cursor> loader) {
      LogHelper.d(_TAG, "Entry: onLoaderReset(loader: %s)", loader);

      switch (loader.getId()) {
         case _STATISTIC_LOADER_ID:
            if (_statisticAdapter == null) {
               return;
            }

            if (_statisticAdapter.getCursor() != null) {
               _statisticAdapter.getCursor().close();
            }

            _statisticAdapter.swapCursor(null);
            break;
      }

      LogHelper.d(_TAG, "Exit: onLoaderReset");
   }

   private void initStatisticSelection(Cursor data) {
      LogHelper.d(_TAG, "Entry: initStatisticSelection(data: %s)", data);


      if (data == null || data.getCount() == 0) {
         // No statistics defined in the core app

         // Hide statistic selector
         _statisticSelector.setVisibility(View.GONE);
         _statisticSelectorLabel.setVisibility(View.GONE);

         // Init listener
         _buttonAction = new OnCancelButtonClicked();
         _applyButton.setOnClickListener(_buttonAction);

         // Init labels
         _applyButton.setText(R.string.cancel_button);
         _description.setText(R.string.statistic_widget_no_statistic_defined);
      } else {
         _statisticAdapter.swapCursor(data);
      }

      LogHelper.d(_TAG, "Exit: initStatisticSelection");
   }

   private class OnCancelButtonClicked implements View.OnClickListener {


      @Override
      public void onClick(View v) {
         // Close configuration (with cancel)
         StatisticWidgetConfigureActivity.this.finish();
      }
   }

   private class OnApplyButtonClicked implements View.OnClickListener {

      @Override
      public void onClick(View v) {
         long statisticId = _statisticSelector.getSelectedItemId();

         if (statisticId > 0) {
            SharedPreferences prefs = getSharedPreferences(Constants.STATISTIC_PREFS, Context.MODE_PRIVATE);

            prefs.edit().putLong(Constants.STATISTIC_PREFIX + _widgetId, statisticId).apply();

            StatisticWidgetProvider.updateWidget(getApplicationContext(), AppWidgetManager.getInstance(getApplicationContext()), _widgetId, statisticId);

            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, _widgetId);
            setResult(RESULT_OK, resultValue);
         }

         StatisticWidgetConfigureActivity.this.finish();
      }
   }
}



