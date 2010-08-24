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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import org.jraf.vtail.misc.MiscUtil;

public class NeverEndingFileInputStream extends InputStream {
    private final File mFile;
    private final RandomAccessFile mRandomAccessFile;

    public NeverEndingFileInputStream(final File file) throws FileNotFoundException {
        mFile = file;
        mRandomAccessFile = new RandomAccessFile(file, "r");
    }

    @Override
    public int read() throws IOException {
        // this one should never be called
        return -1;
    }

    @Override
    public int available() throws IOException {
        return (int) (mFile.length() - mRandomAccessFile.getFilePointer());
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        int res = mRandomAccessFile.read(b, off, len);
        while (res == -1) {
            MiscUtil.sleep(500);
            res = mRandomAccessFile.read(b, off, len);
        }
        return res;
    }
}
