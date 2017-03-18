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
package org.jraf.vtail.arguments;

import java.awt.Color;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.text.StyleConstants;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;


public class HighlightConverter extends BaseConverter<Highlight> {
    public HighlightConverter(final String optionName) {
        super(optionName);
    }

    @Override
    public Highlight convert(final String value) {
        final int colonIndex = value.lastIndexOf(':');
        if (colonIndex == -1) {
            throw new ParameterException(getOptionName() + ": cannot parse '" + value
                    + "'. Value must be of the form <regular expression>:<highlight expression>, eg: -hl .*foobar.*:red,bold or -hl .*foobar.*:#ff0000");
        }

        final Highlight res = new Highlight();

        final String regexpStr = value.substring(0, colonIndex);
        try {
            res.pattern = Pattern.compile(regexpStr);
        } catch (final PatternSyntaxException e) {
            throw new ParameterException(getOptionName() + ": cannot parse '" + value
                    + "'. Your regular expression is malformed, please consult this document; http://download.oracle.com/javase/tutorial/essential/regex/\n"
                    + e.getMessage());
        }


        final String styleStr = value.substring(colonIndex + 1);
        if (styleStr.length() == 0) {
            throw new ParameterException(getOptionName() + ": cannot parse '" + value
                    + "'. Value must be of the form <regular expression>:<highlight expression>, eg: -hl .*foobar.*:red,bold or -hl .*foobar.*:#ff0000");
        }

        final String[] styleParts = styleStr.split(",");
        boolean foreground = true;
        for (final String part : styleParts) {
            Color color = null;
            if ("bold".equals(part)) {
                res.style.addAttribute(StyleConstants.Bold, true);
            } else if ("italic".equals(part)) {
                res.style.addAttribute(StyleConstants.Italic, true);
            } else if ("underline".equals(part)) {
                res.style.addAttribute(StyleConstants.Underline, true);
            } else if ("BLACK".equalsIgnoreCase(part)) {
                color = Color.BLACK;
            } else if ("BLUE".equalsIgnoreCase(part)) {
                color = Color.BLUE;
            } else if ("CYAN".equalsIgnoreCase(part)) {
                color = Color.CYAN;
            } else if ("DARK_GRAY".equalsIgnoreCase(part)) {
                color = Color.DARK_GRAY;
            } else if ("GRAY".equalsIgnoreCase(part)) {
                color = Color.GRAY;
            } else if ("GREEN".equalsIgnoreCase(part)) {
                color = Color.GREEN;
            } else if ("LIGHT_GRAY".equalsIgnoreCase(part)) {
                color = Color.LIGHT_GRAY;
            } else if ("MAGENTA".equalsIgnoreCase(part)) {
                color = Color.MAGENTA;
            } else if ("ORANGE".equalsIgnoreCase(part)) {
                color = Color.ORANGE;
            } else if ("PINK".equalsIgnoreCase(part)) {
                color = Color.PINK;
            } else if ("RED".equalsIgnoreCase(part)) {
                color = Color.RED;
            } else if ("WHITE".equalsIgnoreCase(part)) {
                color = Color.WHITE;
            } else if ("YELLOW".equalsIgnoreCase(part)) {
                color = Color.YELLOW;
            } else {
                try {
                    color = Color.decode(part);
                } catch (final NumberFormatException e) {
                    throw new ParameterException(
                            getOptionName()
                                    + ": could not parse the style part '"
                                    + part
                                    + "'. Style must be a comma separated sequence of a color (a logical name like red or black, or an hex value like #ff0000) or an attribute (bold, italic, or underline).\nIf 2 colors are present, the first one is for the foreground and the second one for the background.");
                }
            }
            if (color != null) {
                if (foreground) {
                    res.style.addAttribute(StyleConstants.Foreground, color);
                    foreground = false;
                } else {
                    res.style.addAttribute(StyleConstants.Background, color);
                }
            }
        }
        return res;
    }
}
