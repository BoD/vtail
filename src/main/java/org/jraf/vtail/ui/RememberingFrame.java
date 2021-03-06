/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2010 Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jraf.vtail.ui;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.prefs.Preferences;

import javax.swing.JFrame;

import org.jraf.vtail.misc.Config;
import org.jraf.vtail.misc.Log;

public class RememberingFrame extends JFrame {
    private static final String TAG = RememberingFrame.class.getName();

    private static final String PREF_MAXIMIZED = "maximized";
    private static final String PREF_WIDTH = "width";
    private static final String PREF_HEIGHT = "height";
    private static final String PREF_X = "x";
    private static final String PREF_Y = "y";

    private final Preferences mPreferences;

    public RememberingFrame(final Class clazz) {
        super();
        mPreferences = Preferences.userNodeForPackage(clazz).node(clazz.getSimpleName());
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent componentEvent) {
                final boolean maximized = (getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH;
                if (Config.LOGD) Log.d(TAG, "componentResized maximized=" + maximized + " getExtendedState()=" + getExtendedState());
                if (maximized) {
                    mPreferences.put(PREF_MAXIMIZED, "true");
                } else {
                    mPreferences.put(PREF_MAXIMIZED, "false");
                    mPreferences.put(PREF_WIDTH, "" + getWidth());
                    mPreferences.put(PREF_HEIGHT, "" + getHeight());

                }
            }

            @Override
            public void componentMoved(final ComponentEvent componentEvent) {
                final boolean maximized = (getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH;
                if (Config.LOGD) Log.d(TAG, "componentMoved maximized=" + maximized);
                if (!maximized) {
                    mPreferences.put(PREF_X, "" + getX());
                    mPreferences.put(PREF_Y, "" + getY());
                }
            }
        });
    }

    @Override
    public void pack() {
        final boolean maximize = "true".equals(mPreferences.get(PREF_MAXIMIZED, "false"));
        if (Config.LOGD) Log.d(TAG, "pref maximized=" + maximize);
        if (mPreferences.get(PREF_X, null) == null) {
            setSize(320, 200);
            setLocationRelativeTo(null);
        } else {
            if (Config.LOGD) Log.d(TAG, "setBounds");
            setBounds(Integer.parseInt(mPreferences.get(PREF_X, "320")), Integer.parseInt(mPreferences.get(PREF_Y, "240")),
                    Integer.parseInt(mPreferences.get(PREF_WIDTH, "320")), Integer.parseInt(mPreferences.get(PREF_HEIGHT, "240")));
            if (maximize) {
                if (Config.LOGD) Log.d(TAG, "setExtendedState");
                setExtendedState(MAXIMIZED_BOTH);
            }
        }
    }


}