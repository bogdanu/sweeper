/*
 * Sweeper
 * Copyright (C) 2012 Bogdan Pistol
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package gg.pistol.sweeper.core.hash;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import com.google.common.base.Preconditions;

/**
 * SHA-1 hash
 *
 * @author Bogdan Pistol
 */
public class HashFunction {

    private static final int BUFFER_SIZE = 8 * (1 << 20);

    private final MessageDigest sha1Algorithm;

    private final byte[] buf;

    public HashFunction() throws NoSuchAlgorithmException {
        sha1Algorithm = MessageDigest.getInstance("SHA-1");
        buf = new byte[BUFFER_SIZE];
    }

    public String compute(InputStream stream) throws IOException {
        Preconditions.checkNotNull(stream);
        int len;
        while ((len = stream.read(buf)) != -1) {
            sha1Algorithm.update(buf, 0, len);
        }
        byte[] digest = sha1Algorithm.digest();
        sha1Algorithm.reset();
        Formatter formatter = new Formatter();
        for (byte b : digest) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

}
