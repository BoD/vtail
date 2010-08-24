/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright 2010 Benoit 'BoD' Lubek (BoD@JRAF.org).  All Rights Reserved.
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
package org.jraf.vtail.core;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;

import org.jraf.vtail.arguments.Arguments;
import org.jraf.vtail.arguments.Highlight;
import org.jraf.vtail.misc.Config;
import org.jraf.vtail.misc.Log;
import org.jraf.vtail.misc.MiscUtil;
import org.jraf.vtail.ui.RememberingFrame;
import org.jraf.vtail.ui.WrapTextPane;

public class VtailWindow {
    private static final String TAG = VtailWindow.class.getName();

    private static final SimpleAttributeSet DEFAULT_STYLE = new SimpleAttributeSet();

    private final RememberingFrame mFrame;
    private BufferedReader mBufferedReader;
    private final WrapTextPane mTextPane;
    private final Arguments mArguments;
    private final JScrollPane mScrollPane;
    private boolean mScrolling;
    private final String mTitle;
    private boolean mFirstLine = true;
    private int mOldScrollbarMax;

    private final List<String> mLines = Collections.synchronizedList(new ArrayList<String>(30));



    public VtailWindow(final Arguments arguments) {
        mArguments = arguments;

        // Set System L&F
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final Exception e) {
            Log.w(TAG, "Could not set system plaf", e);
        }

        mTitle = arguments.title;

        mFrame = new RememberingFrame(VtailWindow.class);

        mTextPane = new WrapTextPane();
        mTextPane.setFont(new Font(mArguments.fontName, Font.PLAIN, mArguments.fontSize));
        mTextPane.setWrap(!arguments.nowrap);
        mTextPane.setBackground(arguments.background.color);
        mTextPane.setForeground(arguments.foreground.color);

        mFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mScrollPane = new JScrollPane(mTextPane);
        mFrame.getContentPane().add(mScrollPane);
        mFrame.setTitle(mTitle);
        mFrame.setIconImage(new ImageIcon(getClass().getResource("/icon.png")).getImage());

        initScrollPaneChangeListener();
        initPopupMenu();
    }

    public void startReadWriteLoop(final InputStream inputStream, final String charset) throws UnsupportedEncodingException {
        // wait a bit for the window to be shown
        MiscUtil.sleep(250);

        mBufferedReader = new BufferedReader(new InputStreamReader(inputStream, charset));

        new Thread(new Runnable() {
            @Override
            public void run() {
                readLoop();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                writeLoop();
            }
        }).start();
    }

    private void readLoop() {
        while (true) {
            String line = null;
            try {
                line = mBufferedReader.readLine();
            } catch (final IOException e) {
                // should never happen since ready == true
                Log.e(TAG, "Cannot read next line: giving up", e);
                // give up
                return;
            }

            if (line == null) {
                // end of stream
                if (Config.LOGD) Log.d(TAG, "End of stream");
                break;
            }

            if (line.length() == 0) {
                continue;
            }

            if (!isIgnored(line)) {
                mLines.add(line);
            }
        }
    }

    private void writeLoop() {
        while (true) {
            synchronized (mLines) {
                for (final String line : mLines) {
                    printLine(line);
                }
            }

            if (!mScrolling) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        final JScrollBar verticalScrollBar = mScrollPane.getVerticalScrollBar();
                        verticalScrollBar.setValue(verticalScrollBar.getMaximum());
                    }
                });
            }
            mLines.clear();
            MiscUtil.sleep(250);
        }
    }

    private boolean isIgnored(final String line) {
        if (mArguments.ignoreList == null) {
            return false;
        }
        for (final Pattern pattern : mArguments.ignoreList) {
            if (pattern.matcher(line).matches()) {
                return true;
            }
        }
        return false;
    }

    private void printLine(final String line) {
        SimpleAttributeSet style = DEFAULT_STYLE;
        if (mArguments.highlightList != null) {
            for (final Highlight highlight : mArguments.highlightList) {
                if (highlight.pattern.matcher(line).matches()) {
                    if (style == DEFAULT_STYLE) {
                        style = new SimpleAttributeSet();
                    }
                    style.addAttributes(highlight.style);
                }
            }
        }

        final Document document = mTextPane.getDocument();
        try {
            document.insertString(document.getLength(), (mFirstLine ? "" : "\n") + line, style);
            if (mFirstLine) {
                mFirstLine = false;
            }
        } catch (final BadLocationException e) {
            // should never happen
            Log.e(TAG, "insertString", e);
        }
    }

    public void show() {
        mFrame.pack();
        mFrame.setVisible(true);
    }

    private void initScrollPaneChangeListener() {
        mScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                final int max = e.getAdjustable().getMaximum();
                if (Config.LOGD) Log.d(TAG, "max=" + max);
                if (mOldScrollbarMax == max) {
                    scrollEvent();
                }
                mOldScrollbarMax = max;
            }
        });
    }

    private void scrollEvent() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final JScrollBar scrollBar = mScrollPane.getVerticalScrollBar();
                final int value = scrollBar.getValue() + scrollBar.getVisibleAmount();
                final int max = scrollBar.getMaximum();
                if (value < max) {
                    mScrolling = true;
                    mTextPane.setBackground(mArguments.scrollingBackground.color);
                    mFrame.setTitle(mTitle + " [scrolling]");
                } else {
                    mScrolling = false;
                    mTextPane.setBackground(mArguments.background.color);
                    mFrame.setTitle(mTitle);
                }
            }
        });
    }

    private void initPopupMenu() {
        Action copyAction = null;
        Action selectAllAction = null;
        final Action[] actions = mTextPane.getActions();
        for (final Action action : actions) {
            if (action.getValue(Action.NAME).equals("copy-to-clipboard")) {
                copyAction = action;
            } else if (action.getValue(Action.NAME).equals("select-all")) {
                selectAllAction = action;
            }
        }

        if (copyAction != null && selectAllAction != null) {
            copyAction.putValue(Action.NAME, "Copy");
            selectAllAction.putValue(Action.NAME, "Select all");
        }

        final JPopupMenu popup = new JPopupMenu();
        popup.add(copyAction);
        popup.add(selectAllAction);
        popup.add(new AbstractAction("Clear") {
            private static final long serialVersionUID = -4399679337757745275L;

            @Override
            public void actionPerformed(ActionEvent e) {
                mTextPane.setText("");
                mFirstLine = true;
            }
        });
        final JCheckBoxMenuItem wrapMenuItem = new JCheckBoxMenuItem("Line wrapping");
        wrapMenuItem.setState(!mArguments.nowrap);
        wrapMenuItem.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                mTextPane.setWrap(wrapMenuItem.isSelected());
                mScrollPane.doLayout();
            }
        });

        popup.add(wrapMenuItem);

        mTextPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showPopupIfNeeded(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showPopupIfNeeded(e);
            }

            private void showPopupIfNeeded(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }
}
