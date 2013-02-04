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
package gg.pistol.sweeper.i18n;

import com.google.common.base.Preconditions;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Resource bundle loaded from XML properties.
 *
 * @author Bogdan Pistol
 */
// package private
@ThreadSafe
class XMLResourceBundle extends ResourceBundle {
    private final Properties properties;
    private final Lock lock;
    @GuardedBy("lock") @Nullable private Collection<String> keys;

    XMLResourceBundle(InputStream stream) throws IOException {
        Preconditions.checkNotNull(stream);
        properties = new Properties();
        lock = new ReentrantLock();
        properties.loadFromXML(stream);
    }

    protected Object handleGetObject(String key) {
        Preconditions.checkNotNull(key);
        return properties.getProperty(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        // It is synchronized to ensure thread-safeness when concurrently accessing the keys.
        lock.lock();
        try {
            return getKeys0();
        } finally {
            lock.unlock();
        }
    }

    private Enumeration<String> getKeys0() {
        if (keys == null) {
            keys = new LinkedHashSet<String>();
            for (Object o : properties.keySet()) {
                keys.add((String) o);
            }
            if (parent != null) {
                Enumeration<String> e = parent.getKeys();
                while (e.hasMoreElements()) {
                    keys.add(e.nextElement());
                }
            }
        }
        return Collections.enumeration(keys);
    }
}
