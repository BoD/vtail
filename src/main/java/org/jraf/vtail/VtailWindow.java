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
package org.jraf.vtail;

import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;

public class VtailWindow {
    private static final SimpleAttributeSet DEFAULT_STYLE = new SimpleAttributeSet();

    private final RememberingFrame mFrame;
    private BufferedReader mBufferedReader;
    private final WrapJTextPane mTextPane;
    private final Arguments mArguments;
    private final JScrollPane mScrollPane;
    private boolean mScrolling;
    private final String mTitle;
    private boolean mFirstLine = true;

    private final List<String> mLines = Collections.synchronizedList(new ArrayList<String>(30));


    public VtailWindow(final Arguments arguments) {
        mArguments = arguments;

        // Set System L&F
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final Exception e) {
            Log.w("Could not set system plaf", e);
        }

        mTitle = arguments.title;

        mFrame = new RememberingFrame(VtailWindow.class);

        mTextPane = new WrapJTextPane();
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
                Log.e("Cannot read next line: giving up", e);
                // give up
                return;
            }

            if (line == null) {
                // end of stream
                Log.d("End of stream");
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
            Log.e("insertString", e);
        }
    }

    public void show() {
        mFrame.pack();
        mFrame.setVisible(true);
    }

    private void initScrollPaneChangeListener() {
        mScrollPane.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(final MouseWheelEvent e) {
                scrollEvent();
            }
        });

        mTextPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                scrollEvent();
            }

            @Override
            public void keyReleased(final KeyEvent e) {
                scrollEvent();
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
}
