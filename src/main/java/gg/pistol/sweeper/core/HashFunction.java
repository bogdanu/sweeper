/*
 * Sweeper - Duplicate file cleaner
 * Copyright (C) 2012 Bogdan Ciprian Pistol
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
package gg.pistol.sweeper.core;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Preconditions;

/**
 * SHA-1 hash function implementation.
 *
 * @author Bogdan Pistol
 */
// package private
class HashFunction {

    private static final int BUFFER_SIZE = 16 * (1 << 10); // 16 KB

    /*
     * Track the progress in minimum chunks of TRACKING_THRESHOLD_SIZE.
     */
    private static final int TRACKING_THRESHOLD_SIZE = 5 * (1 << 20); // 5 MB

    private final MessageDigest sha1Algorithm;

    private final byte[] buf;


    HashFunction() throws NoSuchAlgorithmException {
        sha1Algorithm = MessageDigest.getInstance("SHA-1");
        buf = new byte[BUFFER_SIZE];
    }

    /**
     * Computes the SHA-1 hash from the {@code inputStream} bytes with progress indication through the provided
     * {@code listener}.
     *
     * <p>If the {@code abortFlag} flag changes while this method executes an {@link SweeperAbortException} will be
     * thrown.
     *
     * @return the hexadecimal representation of the computed SHA-1 hash
     */
    String compute(InputStream inputStream, OperationTrackingListener listener, AtomicBoolean abortFlag)
            throws IOException,SweeperAbortException {

        Preconditions.checkNotNull(inputStream);
        Preconditions.checkNotNull(listener);
        Preconditions.checkNotNull(abortFlag);

        try {
            return doCompute(inputStream, listener, abortFlag);
        } finally {
            // Reset the hash for further use.
            sha1Algorithm.reset();
        }
    }

    private String doCompute(InputStream inputStream, OperationTrackingListener listener, AtomicBoolean abortFlag)
            throws IOException,SweeperAbortException {
        int len;
        int trackingSize = 0;

        while ((len = inputStream.read(buf)) != -1) {
            sha1Algorithm.update(buf, 0, len);

            trackingSize += len;
            if (trackingSize >= TRACKING_THRESHOLD_SIZE) {
                // The micro-progress is a subdivision of a target (from which the input stream is retrieved) action
                // progress.
                listener.incrementTargetActionProgress(trackingSize);
                trackingSize = 0;
            }

            if (abortFlag.get()) {
                throw new SweeperAbortException();
            }
        }

        byte[] digest = sha1Algorithm.digest();

        // Transform the hash into a hexadecimal representation.
        Formatter formatter = new Formatter();
        for (byte b : digest) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

}
