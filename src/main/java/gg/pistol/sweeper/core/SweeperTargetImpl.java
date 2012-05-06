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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.joda.time.DateTime;

import com.google.common.base.Preconditions;

public class SweeperTargetImpl implements SweeperTarget, Comparable<SweeperTargetImpl> {
    
    private static final int BUFFER_SIZE = 8192;
    
    private static MessageDigest sha1Hash;
    
    private final String name;
    
    private final Type type;
    
    private final File resource;
    
    private List<SweeperTargetImpl> children;
    
    private long size;
    
    private DateTime modificationDate;
    
    private String hash;
    
    private boolean expanded = false;
    
    private boolean computed = false;
    
    private Mark mark = Mark.DECIDE_LATER;
    
    public SweeperTargetImpl(List<File> targetResources) throws IOException {
        Preconditions.checkNotNull(targetResources);
        Preconditions.checkArgument(!targetResources.isEmpty(), "targetResources is empty");
        name = null;
        resource = null;
        type = Type.ROOT;
        children = new ArrayList<SweeperTargetImpl>();
        Set<SweeperTargetImpl> set = new TreeSet<SweeperTargetImpl>();
        for (File file : targetResources) {
            SweeperTargetImpl child = new SweeperTargetImpl(file);
            if (!set.contains(child)) {
                set.add(child);
                children.add(child);
            }
        }
        expanded = true;
    }
    
    public SweeperTargetImpl(File targetResource) throws IOException {
        Preconditions.checkNotNull(targetResource);
        resource = targetResource.getCanonicalFile();
        name = resource.getPath();
        if (resource.isFile()) {
            type = Type.FILE;
            expanded = true;
        } else if (resource.isDirectory()) {
            type = Type.FOLDER;
            children = new ArrayList<SweeperTargetImpl>();
        } else {
            throw new IllegalArgumentException("targetResource = <" + name + "> should be a file or a folder");
        }
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public long getSize() throws IllegalStateException {
        if (!isComputed()) {
            throw new IllegalStateException("not computed");
        }
        return size;
    }

    public DateTime getModificationDate() {
        if (!isComputed()) {
            throw new IllegalStateException("not computed");
        }
        return modificationDate;
    }

    public File getResource() {
        return resource;
    }

    public Mark getMark() {
        return mark;
    }

    public String getHash() {
        if (!isComputed()) {
            throw new IllegalStateException("not computed");
        }
        return hash;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public boolean isComputed() {
        return computed;
    }

    public void setMark(Mark mark) {
        Preconditions.checkNotNull(mark);
        this.mark = mark;
    }
    
    /**
     * Expands the immediate children
     * 
     * @throws IOException
     */
    public void expand() throws IOException {
        if (isExpanded()) {
            return;
        }
        List<File> files = Arrays.asList(resource.listFiles());
        for (File file : files) {
            children.add(new SweeperTargetImpl(file));
        }
        Collections.sort(children);
        expanded = true;
    }
    
    /**
     * Computes the size, the last modified date and the hash.
     * <p>
     * The expand method must be called before this one.
     * 
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public void compute() throws IOException, NoSuchAlgorithmException {
        if (!isExpanded()) {
            throw new IllegalStateException("Should be expanded");
        }
        if (isComputed()) {
            return;
        }
        switch (type) {
        case ROOT:
        case FOLDER:
            computeFolder();
            break;
        case FILE:
            computeFile();
            break;
        }
        computed = true;
    }

    private void computeFile() throws IOException, NoSuchAlgorithmException {
        size = resource.length();
        modificationDate = new DateTime(resource.lastModified());
        InputStream stream = getResourceInputStream();
        try {
            hash = computeSha1(stream);
        } finally {
            stream.close();
        }
    }
    
    private InputStream getResourceInputStream() throws FileNotFoundException {
        return new FileInputStream(resource);
    }

    private String computeSha1(InputStream stream) throws NoSuchAlgorithmException, IOException {
        MessageDigest sha = getSha1Hash();
        if (sha == null) {
            throw new NoSuchAlgorithmException("SHA-1 algorithm provider not found");
        }
        byte[] buf = new byte[BUFFER_SIZE];
        int len;
        while ((len = stream.read(buf)) != -1) {
            sha.update(buf, 0, len);
        }
        byte[] digest = sha.digest();
        sha.reset();
        Formatter formatter = new Formatter();
        for (byte b : digest) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
    
    private MessageDigest getSha1Hash() {
        if (sha1Hash == null) {
            try {
                sha1Hash = MessageDigest.getInstance("SHA-1");
            } catch(NoSuchAlgorithmException e) {
                // ignore
            }
        }
        return sha1Hash;
    }

    private void computeFolder() throws NoSuchAlgorithmException, IOException {
        List<String> hashes = new ArrayList<String>();
        for (SweeperTargetImpl child : getChildren()) {
            if (!child.isComputed()) {
                throw new IllegalStateException("All the children need to be computed");
            }
            size += child.getSize();
            if (modificationDate == null || child.getModificationDate().isAfter(modificationDate)) {
                modificationDate = child.getModificationDate();
            }
            hashes.add(child.getHash());
        }
        Collections.sort(hashes);
        StringBuilder sb = new StringBuilder();
        for (String s : hashes) {
            sb.append(s);
        }
        ByteArrayInputStream stream = new ByteArrayInputStream(sb.toString().getBytes());
        hash = computeSha1(stream);
    }
    
    public List<SweeperTargetImpl> getChildren() {
        return children;
    }

    @Override
    public int hashCode() {
        return name == null ? 0 : name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SweeperTargetImpl other = (SweeperTargetImpl) obj;
        if (name == null ^ other.name == null || name != null && !name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        String printName;
        if (name == null) {
            printName = "/";
        } else if (name.equals("/")) {
            printName = "\\/";
        } else {
            printName = name;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append("[");
        sb.append(printName);
        sb.append(", ");
        sb.append(type);
        sb.append(", ");
        sb.append(mark);
        sb.append(", expanded=");
        sb.append(expanded);
        sb.append(", computed=");
        sb.append(computed);
        sb.append(", size=");
        sb.append(size);
        sb.append(", modificationDate=");
        sb.append(modificationDate);
        sb.append(", hash=");
        sb.append(hash);
        sb.append("]");
        return sb.toString();
    }

    public int compareTo(SweeperTargetImpl other) {
        if (other == null || name != null && other.name == null) {
            return 1;
        }
        if (name == null && other.name != null) {
            return -1;
        }
        if (name == null && other.name == null) {
            return 0;
        }
        return name.compareTo(other.name);
    }

}
