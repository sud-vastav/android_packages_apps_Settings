/*
 * Copyright (C) 2016 The Xperia Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.xosp;

import android.content.ContentResolver;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface; 
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.TrafficStats;
import android.widget.EditText; 
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.view.View;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.xosp.SeekBarPreference;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cyanogenmod.providers.CMSettings;

public class StatusBarPersonalizations extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener{

    private static final String NETWORK_TRAFFIC_STATE = "network_traffic_state";
    private static final String NETWORK_TRAFFIC_UNIT = "network_traffic_unit";
    private static final String NETWORK_TRAFFIC_PERIOD = "network_traffic_period";
    private static final String NETWORK_TRAFFIC_AUTOHIDE = "network_traffic_autohide";
    private static final String NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD = "network_traffic_autohide_threshold";
    private static final String PREF_FONT_STYLE = "font_style";
    private static final String PREF_STATUS_BAR_CLOCK_FONT_SIZE  = "status_bar_clock_font_size";
    private static final String STATUS_BAR_DATE = "status_bar_date";
    private static final String STATUS_BAR_DATE_STYLE = "status_bar_date_style";
    private static final String STATUS_BAR_DATE_FORMAT = "status_bar_date_format";
    private static final String PREF_CLOCK_DATE_POSITION = "clock_date_position"; 
        
    public static final int CLOCK_DATE_STYLE_LOWERCASE = 1;
    public static final int CLOCK_DATE_STYLE_UPPERCASE = 2;
    private static final int CUSTOM_CLOCK_DATE_FORMAT_INDEX = 18;

    private ListPreference mNetTrafficState;
    private ListPreference mNetTrafficUnit;
    private ListPreference mNetTrafficPeriod;
    private SwitchPreference mNetTrafficAutohide;
    private SeekBarPreference mNetTrafficAutohideThreshold;
    private ListPreference mFontStyle;
    private ListPreference mStatusBarClockFontSize;
    private ListPreference mStatusBarDate;
    private ListPreference mStatusBarDateStyle;
    private ListPreference mStatusBarDateFormat;
    private ListPreference mClockDatePosition;    
    
    private int mNetTrafficVal;
    private int MASK_UP;
    private int MASK_DOWN;
    private int MASK_UNIT;
    private int MASK_PERIOD;
    private ListPreference mQuickPulldown;
        
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.xosp_statusbar_cat);
        loadResources();
        
        PreferenceScreen prefScreen = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();
        
        mNetTrafficState = (ListPreference) prefScreen.findPreference(NETWORK_TRAFFIC_STATE);
        mNetTrafficUnit = (ListPreference) prefScreen.findPreference(NETWORK_TRAFFIC_UNIT);
        mNetTrafficPeriod = (ListPreference) prefScreen.findPreference(NETWORK_TRAFFIC_PERIOD);
        mNetTrafficAutohide = (SwitchPreference) prefScreen.findPreference(NETWORK_TRAFFIC_AUTOHIDE);
        mNetTrafficAutohideThreshold = (SeekBarPreference) prefScreen.findPreference(NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD);

        // TrafficStats will return UNSUPPORTED if the device does not support it.
        if (TrafficStats.getTotalTxBytes() != TrafficStats.UNSUPPORTED &&
                TrafficStats.getTotalRxBytes() != TrafficStats.UNSUPPORTED) {
            mNetTrafficVal = Settings.System.getInt(resolver, Settings.System.NETWORK_TRAFFIC_STATE, 0);
            int intIndex = mNetTrafficVal & (MASK_UP + MASK_DOWN);
            intIndex = mNetTrafficState.findIndexOfValue(String.valueOf(intIndex));

            mNetTrafficState.setValueIndex(intIndex >= 0 ? intIndex : 0);
            mNetTrafficState.setSummary(mNetTrafficState.getEntry());
            mNetTrafficState.setOnPreferenceChangeListener(this);

            mNetTrafficUnit.setValueIndex(getBit(mNetTrafficVal, MASK_UNIT) ? 1 : 0);
            mNetTrafficUnit.setSummary(mNetTrafficUnit.getEntry());
            mNetTrafficUnit.setOnPreferenceChangeListener(this);

            intIndex = (mNetTrafficVal & MASK_PERIOD) >>> 16;
            intIndex = mNetTrafficPeriod.findIndexOfValue(String.valueOf(intIndex));
            mNetTrafficPeriod.setValueIndex(intIndex >= 0 ? intIndex : 1);
            mNetTrafficPeriod.setSummary(mNetTrafficPeriod.getEntry());
            mNetTrafficPeriod.setOnPreferenceChangeListener(this);

            int netTrafficAutohideThreshold = Settings.System.getInt(getContentResolver(),
                    Settings.System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD, 10);
            mNetTrafficAutohideThreshold.setValue(netTrafficAutohideThreshold / 1);
            mNetTrafficAutohideThreshold.setOnPreferenceChangeListener(this);

            mNetTrafficUnit.setEnabled(intIndex != 0);
            mNetTrafficPeriod.setEnabled(intIndex != 0);
            mNetTrafficAutohide.setEnabled(intIndex != 0);
            mNetTrafficAutohideThreshold.setEnabled(intIndex != 0);
        } else {
            prefScreen.removePreference(findPreference(NETWORK_TRAFFIC_STATE));
            prefScreen.removePreference(findPreference(NETWORK_TRAFFIC_UNIT));
            prefScreen.removePreference(findPreference(NETWORK_TRAFFIC_PERIOD));
            prefScreen.removePreference(findPreference(NETWORK_TRAFFIC_AUTOHIDE));
            prefScreen.removePreference(findPreference(NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD));
        }
        mFontStyle = (ListPreference) findPreference(PREF_FONT_STYLE);
        mFontStyle.setOnPreferenceChangeListener(this);
        mFontStyle.setValue(Integer.toString(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.STATUSBAR_CLOCK_FONT_STYLE,
                0)));
        mFontStyle.setSummary(mFontStyle.getEntry());
        
        mStatusBarClockFontSize = (ListPreference) findPreference(PREF_STATUS_BAR_CLOCK_FONT_SIZE);
        mStatusBarClockFontSize.setOnPreferenceChangeListener(this);
        mStatusBarClockFontSize.setValue(Integer.toString(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.STATUSBAR_CLOCK_FONT_SIZE, 
                14)));
        mStatusBarClockFontSize.setSummary(mStatusBarClockFontSize.getEntry());

        mStatusBarDate = (ListPreference) findPreference(STATUS_BAR_DATE);
        mStatusBarDateStyle = (ListPreference) findPreference(STATUS_BAR_DATE_STYLE);
        mStatusBarDateFormat = (ListPreference) findPreference(STATUS_BAR_DATE_FORMAT);

        int showDate = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_DATE, 0);
        mStatusBarDate.setValue(String.valueOf(showDate));
        mStatusBarDate.setSummary(mStatusBarDate.getEntry());
        mStatusBarDate.setOnPreferenceChangeListener(this);

        int dateStyle = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_DATE_STYLE, 0);
        mStatusBarDateStyle.setValue(String.valueOf(dateStyle));
        mStatusBarDateStyle.setSummary(mStatusBarDateStyle.getEntry());
        mStatusBarDateStyle.setOnPreferenceChangeListener(this);

        mStatusBarDateFormat.setOnPreferenceChangeListener(this);
        mStatusBarDateFormat.setSummary(mStatusBarDateFormat.getEntry());
        if (mStatusBarDateFormat.getValue() == null) {
            mStatusBarDateFormat.setValue("EEE");
        }

        parseClockDateFormats();

        mClockDatePosition = (ListPreference) findPreference(PREF_CLOCK_DATE_POSITION);
        mClockDatePosition.setOnPreferenceChangeListener(this);
        mClockDatePosition.setValue(Integer.toString(Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_CLOCK_DATE_POSITION,
                0)));
        mClockDatePosition.setSummary(mClockDatePosition.getEntry());
    }
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        AlertDialog dialog;
        ContentResolver resolver = getActivity().getContentResolver();
        
        if (preference == mNetTrafficState) {
            int intState = Integer.valueOf((String)newValue);
            mNetTrafficVal = setBit(mNetTrafficVal, MASK_UP, getBit(intState, MASK_UP));
            mNetTrafficVal = setBit(mNetTrafficVal, MASK_DOWN, getBit(intState, MASK_DOWN));
            Settings.System.putInt(resolver, Settings.System.NETWORK_TRAFFIC_STATE, mNetTrafficVal);
            int index = mNetTrafficState.findIndexOfValue((String) newValue);
            mNetTrafficState.setSummary(mNetTrafficState.getEntries()[index]);
            mNetTrafficUnit.setEnabled(intState != 0);
            mNetTrafficPeriod.setEnabled(intState != 0);
            mNetTrafficAutohide.setEnabled(intState != 0);
            mNetTrafficAutohideThreshold.setEnabled(intState != 0);
            return true;
        } else if (preference == mNetTrafficUnit) {
            // 1 = Display as Byte/s; default is bit/s
            mNetTrafficVal = setBit(mNetTrafficVal, MASK_UNIT, ((String)newValue).equals("1"));
            Settings.System.putInt(resolver, Settings.System.NETWORK_TRAFFIC_STATE, mNetTrafficVal);
            int index = mNetTrafficUnit.findIndexOfValue((String) newValue);
            mNetTrafficUnit.setSummary(mNetTrafficUnit.getEntries()[index]);
            return true;
        } else if (preference == mNetTrafficPeriod) {
            int intState = Integer.valueOf((String)newValue);
            mNetTrafficVal = setBit(mNetTrafficVal, MASK_PERIOD, false) + (intState << 16);
            Settings.System.putInt(resolver, Settings.System.NETWORK_TRAFFIC_STATE, mNetTrafficVal);
            int index = mNetTrafficPeriod.findIndexOfValue((String) newValue);
            mNetTrafficPeriod.setSummary(mNetTrafficPeriod.getEntries()[index]);
            return true;
        } else if (preference == mNetTrafficAutohideThreshold) {
            int threshold = (Integer) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD, threshold * 1);
            return true;
        } else if (preference == mFontStyle) {
            int val = Integer.parseInt((String) newValue);
            int index = mFontStyle.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_CLOCK_FONT_STYLE, val);
            mFontStyle.setSummary(mFontStyle.getEntries()[index]);
            return true;
        } else if (preference == mStatusBarClockFontSize) {
            int val = Integer.parseInt((String) newValue);
            int index = mStatusBarClockFontSize.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_CLOCK_FONT_SIZE, val);
            mStatusBarClockFontSize.setSummary(mStatusBarClockFontSize.getEntries()[index]);
            return true;
        } else if (preference == mStatusBarDate) {
            int statusBarDate = Integer.valueOf((String) newValue);
            int index = mStatusBarDate.findIndexOfValue((String) newValue);
            Settings.System.putInt(
                    resolver, STATUS_BAR_DATE, statusBarDate);
            mStatusBarDate.setSummary(mStatusBarDate.getEntries()[index]);
            return true;
        } else if (preference == mStatusBarDateStyle) {
            int statusBarDateStyle = Integer.parseInt((String) newValue);
            int index = mStatusBarDateStyle.findIndexOfValue((String) newValue);
            Settings.System.putInt(
                    resolver, STATUS_BAR_DATE_STYLE, statusBarDateStyle);
            mStatusBarDateStyle.setSummary(mStatusBarDateStyle.getEntries()[index]);
            return true;
        } else if (preference ==  mStatusBarDateFormat) {
            int index = mStatusBarDateFormat.findIndexOfValue((String) newValue);
            if (index == CUSTOM_CLOCK_DATE_FORMAT_INDEX) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setTitle(R.string.status_bar_date_string_edittext_title);
                alert.setMessage(R.string.status_bar_date_string_edittext_summary);

                final EditText input = new EditText(getActivity());
                String oldText = Settings.System.getString(
                    getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_DATE_FORMAT);
                if (oldText != null) {
                    input.setText(oldText);
                }
                alert.setView(input);

                alert.setPositiveButton(R.string.menu_save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int whichButton) {
                        String value = input.getText().toString();
                        if (value.equals("")) {
                            return;
                        }
                        Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.STATUS_BAR_DATE_FORMAT, value);

                        return;
                    }
                });

                alert.setNegativeButton(R.string.menu_cancel,
                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int which) {
                        return;
                    }
                });
                dialog = alert.create();
                dialog.show();
            } else {
                if ((String) newValue != null) {
                    Settings.System.putString(getActivity().getContentResolver(),
                        Settings.System.STATUS_BAR_DATE_FORMAT, (String) newValue);
                }
            }
            return true;
        } else if (preference == mClockDatePosition) {
            int val = Integer.parseInt((String) newValue);
            int index = mClockDatePosition.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_CLOCK_DATE_POSITION, val);
            mClockDatePosition.setSummary(mClockDatePosition.getEntries()[index]);
            parseClockDateFormats();
            return true;
        }
        return false;
    }
    
    private void loadResources() {
         Resources resources = getActivity().getResources();
         MASK_UP = resources.getInteger(R.integer.maskUp);
         MASK_DOWN = resources.getInteger(R.integer.maskDown);
         MASK_UNIT = resources.getInteger(R.integer.maskUnit);
         MASK_PERIOD = resources.getInteger(R.integer.maskPeriod);
    }
 
    private void enableStatusBarClockDependents() {
        int clockStyle = CMSettings.System.getInt(getActivity()
                .getContentResolver(), CMSettings.System.STATUS_BAR_CLOCK, 1);
        if (clockStyle == 0) {
            mStatusBarDate.setEnabled(false);
            mStatusBarDateStyle.setEnabled(false);
            mStatusBarDateFormat.setEnabled(false);
        } else {
            mStatusBarDate.setEnabled(true);
            mStatusBarDateStyle.setEnabled(true);
            mStatusBarDateFormat.setEnabled(true);
        }
    }

    private void parseClockDateFormats() {
        // Parse and repopulate mClockDateFormats's entries based on current date.
        String[] dateEntries = getResources().getStringArray(R.array.status_bar_date_format_entries_values);
        CharSequence parsedDateEntries[];
        parsedDateEntries = new String[dateEntries.length];
        Date now = new Date();

        int lastEntry = dateEntries.length - 1;
        int dateFormat = Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.STATUS_BAR_DATE_STYLE, 0);
        for (int i = 0; i < dateEntries.length; i++) {
            if (i == lastEntry) {
                parsedDateEntries[i] = dateEntries[i];
            } else {
                String newDate;
                CharSequence dateString = DateFormat.format(dateEntries[i], now);
                if (dateFormat == CLOCK_DATE_STYLE_LOWERCASE) {
                    newDate = dateString.toString().toLowerCase();
                } else if (dateFormat == CLOCK_DATE_STYLE_UPPERCASE) {
                    newDate = dateString.toString().toUpperCase();
                } else {
                    newDate = dateString.toString();
                }

                parsedDateEntries[i] = newDate;
            }
        }
        mStatusBarDateFormat.setEntries(parsedDateEntries);
    }

     // intMask should only have the desired bit(s) set
     private int setBit(int intNumber, int intMask, boolean blnState) {
         if (blnState) {
             return (intNumber | intMask);
         }
         return (intNumber & ~intMask);
     }
 
     private boolean getBit(int intNumber, int intMask) {
         return (intNumber & intMask) == intMask;
     }
    
     protected int getMetricsCategory()
     {
        return MetricsLogger.APPLICATION;
     }
}
