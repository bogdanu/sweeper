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
package gg.pistol.sweeper.core;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

/**
 * A SHA-1 hash function.
 *
 * @author Bogdan Pistol
 */
// package private
class HashFunction {

    private static final int BUFFER_SIZE = 16 * (1 << 10); // 16 KB

    private static final int TRACKING_THRESHOLD_SIZE = 5 * (1 << 20); // 5 MB

    private final MessageDigest sha1Algorithm;

    private final byte[] buf;

    HashFunction() throws NoSuchAlgorithmException {
        sha1Algorithm = MessageDigest.getInstance("SHA-1");
        buf = new byte[BUFFER_SIZE];
    }

    String compute(InputStream stream, @Nullable OperationTrackingListener listener, @Nullable AtomicBoolean abort) throws IOException, SweeperAbortException {
        Preconditions.checkNotNull(stream);
        int len;
        int trackingSize = 0;
        while ((len = stream.read(buf)) != -1) {
            sha1Algorithm.update(buf, 0, len);

            trackingSize += len;
            if (trackingSize >= TRACKING_THRESHOLD_SIZE) {
                listener.incrementMicroProgress(trackingSize);
                trackingSize = 0;
            }

            if (abort != null && abort.get()) {
                sha1Algorithm.reset();
                throw new SweeperAbortException();
            }
        }
        byte[] digest = sha1Algorithm.digest();
        sha1Algorithm.reset();
        Formatter formatter = new Formatter();
        for (byte b : digest) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    String compute(InputStream stream) throws IOException, SweeperAbortException {
        return compute(stream, null, null);
    }

}
