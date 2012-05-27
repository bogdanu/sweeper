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

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

/**
 * SHA-1 hash
 * 
 * @author Bogdan Pistol
 */
public class Sha1Sum {

    private static final int BUFFER_SIZE = 8192;
    
    @Nullable private static MessageDigest sha1Algorithm;
    
    public String compute(InputStream stream) throws NoSuchAlgorithmException, IOException {
        Preconditions.checkNotNull(stream);
        MessageDigest sha1 = getSha1Algorithm();
        byte[] buf = new byte[BUFFER_SIZE];
        int len;
        while ((len = stream.read(buf)) != -1) {
            sha1.update(buf, 0, len);
        }
        byte[] digest = sha1.digest();
        sha1.reset();
        Formatter formatter = new Formatter();
        for (byte b : digest) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
    
    protected MessageDigest getSha1Algorithm() throws NoSuchAlgorithmException {
        if (sha1Algorithm == null) {
            sha1Algorithm = MessageDigest.getInstance("SHA-1");
        }
        return sha1Algorithm;
    }
    
}
