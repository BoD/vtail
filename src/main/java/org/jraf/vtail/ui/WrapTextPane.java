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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JTextPane;


public class WrapTextPane extends JTextPane {
    private boolean mWrap;

    public void setWrap(final boolean wrap) {
        mWrap = wrap;
    }

    public boolean isWrap() {
        return mWrap;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        if (!mWrap) {
            if (getParent() == null) {
                return true;
            }
            return ui.getPreferredSize(this).width < getParent().getSize().width;
        }
        return super.getScrollableTracksViewportWidth();
    }

    @Override
    public void setBounds(final int x, final int y, final int width, final int height) {
        if (mWrap) {
            super.setBounds(x, y, width, height);
        } else {
            final Dimension size = getPreferredSize();
            super.setBounds(x, y, Math.max(size.width, width), Math.max(size.height, height));
        }
    }

    @Override
    protected void paintComponent(final Graphics g) {
        //((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // this enables antialiasing (prettier)
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP); // this enables antialiasing (prettier)
        super.paintComponent(g);
    }
}