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

package de.webducer.android.oss.zeiterfassung.widgets.provider;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.widget.RemoteViews;

import de.webducer.android.oss.zeiterfassung.widgets.Constants;
import de.webducer.android.oss.zeiterfassung.widgets.R;
import de.webducer.android.utilities.LogHelper;
import de.webducer.android.utilities.TextHelper;
import de.webducer.android.zeiterfassung.contract.TimeTrackingContract;
import de.webducer.android.zeiterfassung.contract.enums.DurationFormat;


/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link de.webducer.android.oss.zeiterfassung.widgets.config.StatisticWidgetConfigureActivity StatisticWidgetProviderConfigureActivity}
 */
public class StatisticWidgetProvider extends AppWidgetProvider {
   private final static String _TAG = TextHelper.getTag(Constants.TAG_PREFIX, StatisticWidgetProvider.class.getSimpleName());

   @Override
   public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
      LogHelper.d(_TAG, "Entry: onUpdate(context: %s, appWidgetProvider: %s, appWidgetIds: %s)", context, appWidgetManager, appWidgetIds);

      SharedPreferences prefs = context.getSharedPreferences(Constants.STATISTIC_PREFS, Context.MODE_PRIVATE);

      for (int widgetId : appWidgetIds) {
         long statisticId = prefs.getLong(Constants.STATISTIC_PREFIX + widgetId, TimeTrackingContract.NO_ID);

         if (TimeTrackingContract.NO_ID != statisticId) {
            updateWidget(context, appWidgetManager, widgetId, statisticId);
         }
      }

      LogHelper.d(_TAG, "Exit: onUpdate");
   }

   @Override
   public void onDeleted(Context context, int[] appWidgetIds) {
      LogHelper.d(_TAG, "Entry: onDeleted(context: %s, appWidgetIds: %s)", context, appWidgetIds);

      // Delete configuration for deleted widgets
      SharedPreferences prefs = context.getSharedPreferences(Constants.STATISTIC_PREFS, Context.MODE_PRIVATE);

      for (int id : appWidgetIds) {
         prefs.edit().remove(Constants.STATISTIC_PREFIX + id).apply();
      }

      super.onDeleted(context, appWidgetIds);

      LogHelper.d(_TAG, "Entry: onDeleted");
   }

   public static void updateWidget(Context context, AppWidgetManager appWidgetManager, int widgetId, long statisticId) {

      LogHelper.d(_TAG, "Entry: updateWidget(context: %s, appWidgetManager: %s, widgetId: %s, statisticId: %s)", context, appWidgetManager, widgetId, statisticId);

      // Load data from data base
      Uri statisticUri = ContentUris.withAppendedId(TimeTrackingContract.ReportData.StatisticData.CONTENT_URI, statisticId);

      Cursor statisticData = context.getContentResolver().query(statisticUri, new String[] {TimeTrackingContract.ReportData.StatisticData.Columns.CAPTION, TimeTrackingContract.ReportData.StatisticData.Columns.VALUE, TimeTrackingContract.ReportData.StatisticData.Columns.DURATION_FORMAT}, null, null, null);

      RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.statistic_widget);

      if (statisticData != null && statisticData.moveToFirst()) {
         fillWidgetView(statisticData, widgetView);
      } else {
         fillDeletedWidgetView(context, widgetView);
      }

      if (statisticData != null) {
         statisticData.close();
      }

      appWidgetManager.updateAppWidget(widgetId, widgetView);

      LogHelper.d(_TAG, "Exit: updateWidget");
   }

   static void fillWidgetView(Cursor data, RemoteViews widgetView) {
      LogHelper.d(_TAG, "Entry: fillWidgetView(data: %s, widgetView: %s)", data, widgetView);

      if (data.moveToFirst()) {
         String caption = data.getString(data.getColumnIndex(TimeTrackingContract.ReportData.StatisticData.Columns.CAPTION));
         int value = data.getInt(data.getColumnIndex(TimeTrackingContract.ReportData.StatisticData.Columns.VALUE));
         DurationFormat durationFormat = DurationFormat.getDurationFormatByCode(data.getInt(data.getColumnIndex(TimeTrackingContract.ReportData.StatisticData.Columns.DURATION_FORMAT)));

         widgetView.setTextViewText(android.R.id.text1, caption);
         widgetView.setTextViewText(android.R.id.text2, durationFormat.format(value));
      }

      LogHelper.d(_TAG, "Exit: fillWidgetView");
   }

   static void fillDeletedWidgetView(Context context, RemoteViews widgetView) {
      LogHelper.d(_TAG, "Entry: fillDeletedWidgetView(context: %s, widgetView: %s)", context, widgetView);

      String deleted = context.getString(R.string.statistic_deleted);
      widgetView.setTextViewText(android.R.id.text1, deleted);
      widgetView.setTextViewText(android.R.id.text2, deleted);

      LogHelper.d(_TAG, "Exit: fillDeletedWidgetView");
   }
}


