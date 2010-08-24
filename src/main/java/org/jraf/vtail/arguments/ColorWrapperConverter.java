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
package org.jraf.vtail.arguments;

import java.awt.Color;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

public class ColorWrapperConverter extends BaseConverter<ColorWrapper> {
    public ColorWrapperConverter(final String optionName) {
        super(optionName);
    }

    @Override
    public ColorWrapper convert(final String value) {
        Color color = null;
        if ("BLACK".equalsIgnoreCase(value)) {
            color = Color.BLACK;
        } else if ("BLUE".equalsIgnoreCase(value)) {
            color = Color.BLUE;
        } else if ("CYAN".equalsIgnoreCase(value)) {
            color = Color.CYAN;
        } else if ("DARK_GRAY".equalsIgnoreCase(value)) {
            color = Color.DARK_GRAY;
        } else if ("GRAY".equalsIgnoreCase(value)) {
            color = Color.GRAY;
        } else if ("GREEN".equalsIgnoreCase(value)) {
            color = Color.GREEN;
        } else if ("LIGHT_GRAY".equalsIgnoreCase(value)) {
            color = Color.LIGHT_GRAY;
        } else if ("MAGENTA".equalsIgnoreCase(value)) {
            color = Color.MAGENTA;
        } else if ("ORANGE".equalsIgnoreCase(value)) {
            color = Color.ORANGE;
        } else if ("PINK".equalsIgnoreCase(value)) {
            color = Color.PINK;
        } else if ("RED".equalsIgnoreCase(value)) {
            color = Color.RED;
        } else if ("WHITE".equalsIgnoreCase(value)) {
            color = Color.WHITE;
        } else if ("YELLOW".equalsIgnoreCase(value)) {
            color = Color.YELLOW;
        } else {
            try {
                color = Color.decode(value);
            } catch (final NumberFormatException e) {
                throw new ParameterException(getOptionName() + ": could not parse color '" + value
                        + "'. Color must be a logical name like red or black, or an hex value like #ff0000.");
            }
        }
        return new ColorWrapper(color);
    }
}
