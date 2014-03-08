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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.jraf.vtail.arguments.Arguments;

public class Main {

    public static void main(final String[] args) {
        final Arguments arguments = new Arguments();
        final JCommander jCommander = new JCommander(arguments, args);
        jCommander.setProgramName("vtail");

        if (arguments.help) {
            jCommander.usage();
            return;
        }

        InputStream inputStream = System.in;
        if (arguments.fileList != null) {
            final File file = arguments.fileList.get(0);
            try {
                inputStream = new NeverEndingFileInputStream(file);
            } catch (final FileNotFoundException e) {
                System.err.println("Cannot find file " + e.getMessage());
                System.exit(-1);
            }
            if (Arguments.DEFAULT_TITLE.equals(arguments.title)) {
                arguments.title = file.toString();
            }
        }

        final VtailWindow vtailWindow = new VtailWindow(arguments);
        vtailWindow.show();

        try {
            vtailWindow.startReadWriteLoop(inputStream, Charset.defaultCharset().name());
        } catch (final UnsupportedEncodingException e) {
            // can never happen
            throw new AssertionError(e);
        }
    }
}
