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
package org.jraf.vtail.core;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

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
    private final JTextField mFilterTextField;
    private final JTextField mHighlightTextField;
    private final JPanel mBottomPanel;
    private boolean mScrollingMode;
    private boolean mFilteringMode;
    private boolean mHighlightingMode;
    private boolean mShowFiltering;
    private boolean mShowHighlighting;
    private final String mTitle;
    private boolean mFirstLine = true;
    private int mOldScrollbarMax;

    private final List<String> mLines = Collections.synchronizedList(new ArrayList<String>(1000));
    private int mLineCursor;

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
        updateTitle();
        mFrame.setIconImage(new ImageIcon(getClass().getResource("/icon.png")).getImage());

        mBottomPanel = new JPanel(new GridLayout(0, 1));
        mFrame.getContentPane().add(mBottomPanel, BorderLayout.PAGE_END);

        mFilterTextField = new JTextField();
        mHighlightTextField = new JTextField();

        initScrollPaneChangeListener();
        initPopupMenu();
        initFilterListener();
        initHighlightListener();
        initToolBar();
    }

    public void startReadWriteLoop(final InputStream inputStream, final String charset) throws UnsupportedEncodingException {
        // wait a bit for the window to be shown
        MiscUtil.sleep(500);

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
                printLoop();
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

    private void printLoop() {
        while (true) {
            boolean moved = false;
            synchronized (mLines) {
                for (final int len = mLines.size(); mLineCursor < len; mLineCursor++) {
                    final String line = mLines.get(mLineCursor);
                    if (isFilterMatch(line)) {
                        printLine(line);
                        moved = true;
                    }
                }
            }
            if (moved) {
                scrollDown();
            }
            MiscUtil.sleep(250);
        }
    }

    private void scrollDown() {
        if (!mScrollingMode) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    final JScrollBar verticalScrollBar = mScrollPane.getVerticalScrollBar();
                    verticalScrollBar.setValue(verticalScrollBar.getMaximum());
                }
            });
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

        final StyledDocument document = (StyledDocument) mTextPane.getDocument();

        final int posStart = document.getLength();
        try {
            document.insertString(posStart, (mFirstLine ? "" : "\n") + line, style);
        } catch (final BadLocationException e) {
            // should never happen
            Log.e(TAG, "insertString", e);
        }

        if (mHighlightingMode) {
            if (isHighlightMatch(line)) {
                style = new SimpleAttributeSet();
                style.addAttribute(StyleConstants.Foreground, Color.BLACK);
                style.addAttribute(StyleConstants.Background, Color.YELLOW);
                document.setCharacterAttributes(posStart, line.length() + 1, style, true);
            }
        } else {
            SimpleAttributeSet s = null;
            if (mArguments.highlightList != null) {
                for (final Highlight highlight : mArguments.highlightList) {
                    final Matcher matcher = highlight.pattern.matcher(line);
                    s = new SimpleAttributeSet();
                    s.addAttributes(highlight.style);
                    while (matcher.find()) {
                        final int start = matcher.start();
                        document.setCharacterAttributes(posStart + start, matcher.end() - start + 1, s, false);
                    }
                }
            }
        }

        if (mFirstLine) {
            mFirstLine = false;
        }
    }

    public void show() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                mFrame.setVisible(true);
                mFrame.pack();
                mTextPane.requestFocusInWindow();
            }
        });
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
        if (Config.LOGD) Log.d(TAG, "scrollEvent");
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final JScrollBar scrollBar = mScrollPane.getVerticalScrollBar();
                final int value = scrollBar.getValue() + scrollBar.getVisibleAmount();
                final int max = scrollBar.getMaximum();
                final boolean oldScrollingMode = mScrollingMode;
                if (value < max) {
                    mScrollingMode = true;
                    if (!oldScrollingMode) {
                        updateBackgroundColor();
                        updateTitle();
                    }
                } else {
                    mScrollingMode = false;
                    if (oldScrollingMode) {
                        updateBackgroundColor();
                        updateTitle();
                    }
                }
            }
        });
    }

    private void updateBackgroundColor() {
        if (Config.LOGD) Log.d(TAG, "updateBackgroundColor");
        if (mScrollingMode || mFilteringMode) {
            mTextPane.setBackground(mArguments.scrollingBackground.color);
        } else {
            mTextPane.setBackground(mArguments.background.color);
        }
    }

    private void updateTitle() {
        if (Config.LOGD) Log.d(TAG, "updateTitle");
        final StringBuilder title = new StringBuilder(mTitle);
        if (mScrollingMode) {
            title.append(" [scrolling]");
        }
        if (mFilteringMode) {
            title.append(" [filtering]");
        }
        mFrame.setTitle(title.toString());
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
        popup.add(mClearAction);
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

    private void initFilterListener() {
        mFilterTextField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filter();
            }
        });
    }

    private void initHighlightListener() {
        mHighlightTextField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filter();
            }
        });
    }

    protected void filter() {
        if (Config.LOGD) Log.d(TAG, "filter");

        mFilteringMode = mShowFiltering && mFilterTextField.getText().trim().length() != 0;
        mHighlightingMode = mShowHighlighting && mHighlightTextField.getText().trim().length() != 0;
        updateBackgroundColor();
        updateTitle();

        mTextPane.setText("");
        mFirstLine = true;
        synchronized (mLines) {
            for (final String line : mLines) {
                if (mFilteringMode && isFilterMatch(line) || !mFilteringMode) {
                    printLine(line);
                }
            }
        }
        scrollDown();
    }

    private boolean isFilterMatch(String line) {
        if (!mFilteringMode) {
            return true;
        }
        return line.toLowerCase().contains(mFilterTextField.getText().toLowerCase());
    }

    private boolean isHighlightMatch(String line) {
        if (!mHighlightingMode) {
            return true;
        }
        return line.toLowerCase().contains(mHighlightTextField.getText().toLowerCase());
    }

    private void initToolBar() {
        final JToolBar toolBar = new JToolBar();

        mFilterAction.putValue(Action.SELECTED_KEY, Boolean.FALSE);
        JToggleButton toggleButton = new JToggleButton(mFilterAction);
        toggleButton.setFocusable(false);
        toolBar.add(toggleButton);

        mHighlightAction.putValue(Action.SELECTED_KEY, Boolean.FALSE);
        toggleButton = new JToggleButton(mHighlightAction);
        toggleButton.setFocusable(false);
        toolBar.add(toggleButton);

        final JButton button = new JButton(mClearAction);
        button.setFocusable(false);
        toolBar.add(button);


        toolBar.setRollover(true);
        toolBar.setFloatable(false);

        mFrame.getContentPane().add(toolBar, BorderLayout.PAGE_START);
    }

    private final Action mFilterAction = new AbstractAction("Filter") {
        @Override
        public void actionPerformed(ActionEvent e) {
            final Boolean selected = (Boolean) getValue(Action.SELECTED_KEY);
            if (Config.LOGD) Log.d(TAG, "actionPerformed Filter selected=" + selected);
            mShowFiltering = selected;
            if (selected) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        mBottomPanel.add(mFilterTextField, 0);
                        mFilterTextField.requestFocusInWindow();
                        mFrame.getContentPane().validate();
                        scrollDown();
                        if (mFilterTextField.getText().length() > 0) {
                            filter();
                        }
                    }
                });
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        mBottomPanel.remove(mFilterTextField);
                        mFrame.getContentPane().validate();
                        filter();
                    }
                });
            }
        }
    };

    private final Action mHighlightAction = new AbstractAction("Highlight") {
        @Override
        public void actionPerformed(ActionEvent e) {
            final Boolean selected = (Boolean) getValue(Action.SELECTED_KEY);
            if (Config.LOGD) Log.d(TAG, "actionPerformed Highlight selected=" + selected);
            mShowHighlighting = selected;
            if (selected) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        int pos = 0;
                        if (mShowFiltering) {
                            pos = 1;
                        }
                        mBottomPanel.add(mHighlightTextField, pos);
                        mHighlightTextField.requestFocusInWindow();
                        mFrame.getContentPane().validate();

                        scrollDown();
                        if (mHighlightTextField.getText().length() > 0) {
                            filter();
                        }
                    }
                });
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        mBottomPanel.remove(mHighlightTextField);
                        mFrame.getContentPane().validate();
                        filter();
                    }
                });
            }
        }
    };

    private final AbstractAction mClearAction = new AbstractAction("Clear") {
        @Override
        public void actionPerformed(ActionEvent e) {
            mTextPane.setText("");
            mFirstLine = true;
            mLines.clear();
            mLineCursor = 0;
            mScrollingMode = false;
            updateTitle();
            updateBackgroundColor();
        }
    };
}
