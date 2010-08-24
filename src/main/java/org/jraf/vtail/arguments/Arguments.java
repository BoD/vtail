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
import java.awt.Font;
import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;


@Parameters(separators = " =")
public class Arguments {
    public static String DEFAULT_TITLE = "vtail";

    @Parameter(names = { "-h", "--help" }, description = "Display this help and exit")
    public boolean help;

    @Parameter(names = { "-f", "--fontname" }, description = "Font name - logical font names Dialog, DialogInput, SansSerif, Serif and Monospaced are accepted")
    public String fontName = Font.MONOSPACED;

    @Parameter(names = { "-s", "--fontsize" }, description = "Font size")
    public int fontSize = 10;

    @Parameter(names = { "-nw", "--nowrap" }, description = "Don't wrap long lines")
    public boolean nowrap;

    @Parameter(names = { "-hl", "--highlight" }, converter = HighlightConverter.class, description = "Regular expression for lines to highlight and corresponding style. E.g.: -hl .*foobar.*:red,white,bold")
    public List<Highlight> highlightList;

    @Parameter(names = { "-i", "--ignore" }, converter = PatternConverter.class, description = "Regular expression for lines to ignore. E.g.: -i .*foobar.*")
    public List<Pattern> ignoreList;

    @Parameter(names = { "-bg", "--background" }, converter = ColorWrapperConverter.class, description = "Background color - can be a logical name like red or black, or an hex value like #ff0000")
    public ColorWrapper background = new ColorWrapper(Color.WHITE);

    @Parameter(names = { "-fg", "--foreground" }, converter = ColorWrapperConverter.class, description = "Foreground color - can be a logical name like red or black, or an hex value like #ff0000")
    public ColorWrapper foreground = new ColorWrapper(Color.BLACK);

    @Parameter(names = { "-bgs", "--scrollingbackground" }, converter = ColorWrapperConverter.class, description = "Background color when scrolling - can be a logical name like red or black, or an hex value like #ff0000")
    public ColorWrapper scrollingBackground = new ColorWrapper(Color.LIGHT_GRAY);

    @Parameter(names = { "-t", "--title" }, description = "Window title")
    public String title = DEFAULT_TITLE;

    @Parameter(description = "file", arity = 1)
    public List<File> fileList;
}
