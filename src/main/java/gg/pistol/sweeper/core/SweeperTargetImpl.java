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

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;

public class SweeperTargetImpl implements SweeperTarget {
    
    public static final long DEFAULT_SIZE = -1L;
    
    public static final String DEFAULT_HASH = "#";
    
    private static final int COMPUTE_SHA1_BUFFER_SIZE = 8192;
    
    private static final String HASH_SEPARATOR = "-";
    
    private static MessageDigest sha1Algorithm;
    
    private final String name;
    
    private final Type type;
    
    private final File resource;
    
    private final SweeperTargetImpl parent;
    
    private List<SweeperTargetImpl> children;
    
    private long size = DEFAULT_SIZE;
    
    private DateTime modificationDate;
    
    private String hash = DEFAULT_HASH;
    
    private boolean expanded = false;
    
    private boolean sizeComputed = false;
    
    private boolean hashComputed = false;
    
    private Mark mark = Mark.DECIDE_LATER;
    
    private int totalFiles = 0;
    
    public SweeperTargetImpl(List<File> targetResources, SweeperOperationListener listener) {
        Preconditions.checkNotNull(targetResources);
        Preconditions.checkArgument(!targetResources.isEmpty(), "targetResources is empty");
        parent = null;
        name = "";
        resource = null;
        type = Type.ROOT;
        children = new ArrayList<SweeperTargetImpl>();
        doExpand(targetResources, listener);
    }
    
    public SweeperTargetImpl(File targetResource, SweeperTargetImpl parent) throws IOException {
        Preconditions.checkNotNull(targetResource);
        Preconditions.checkNotNull(parent);
        this.parent = parent;
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
        if (!isSizeComputed()) {
            throw new IllegalStateException("not computed");
        }
        return size;
    }

    public DateTime getModificationDate() {
        if (!isHashComputed()) {
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
        if (!isHashComputed()) {
            throw new IllegalStateException("not computed");
        }
        return hash;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public boolean isSizeComputed() {
        return sizeComputed;
    }
    
    public boolean isHashComputed() {
        return hashComputed;
    }

    public void setMark(Mark mark) {
        Preconditions.checkNotNull(mark);
        this.mark = mark;
    }
    
    /**
     * Expands the immediate children
     */
    public void expand(SweeperOperationListener listener) {
        if (isExpanded()) {
            return;
        }
        File[] files;
        try {
            files = resource.listFiles();
        } catch(SecurityException e) {
            listener.updateTargetException(this, SweeperTargetAction.EXPAND, new SweeperException(e));
            return;
        }
        if (files == null) {
            listener.updateTargetException(this, SweeperTargetAction.EXPAND, new SweeperException("I/O error while listing files"));
            return;
        }
        listener.updateTargetAction(this, SweeperTargetAction.EXPAND);
        doExpand(Arrays.asList(files), listener);
    }
    
    private void doExpand(List<File> targetResources, SweeperOperationListener listener) {
        Set<SweeperTargetImpl> set = new TreeSet<SweeperTargetImpl>();
        for (File file : targetResources) {
            SweeperTargetImpl child;
            try {
                child = new SweeperTargetImpl(file, this);
            } catch(Exception e) {
                listener.updateTargetException(new MockTarget(file), SweeperTargetAction.OPEN, new SweeperException(e));
                continue;
            }
            if (!set.contains(child)) {
                set.add(child);
                children.add(child);
                listener.updateTargetAction(child, SweeperTargetAction.OPEN);
            }
        }
        Collections.sort(children);
        expanded = true;
    }
    
    /**
     * Computes the size.
     * <p>
     * The expand method must be called before this one.
     */
    public void computeSize(SweeperOperationListener listener) {
        if (!isExpanded()) {
            throw new IllegalStateException("Should be expanded");
        }
        if (isSizeComputed()) {
            return;
        }
        try {
            if (type == Type.FILE) {
                size = resource.length();
                totalFiles = 1;
            } else {
                computeFolderSize();
            }
        } catch (Exception e) {
            listener.updateTargetException(this, SweeperTargetAction.COMPUTE_SIZE, new SweeperException(e));
        }
        sizeComputed = true;
        listener.updateTargetAction(this, SweeperTargetAction.COMPUTE_SIZE);
    }
    
    /**
     * Computes the hash and the last modified date.
     * <p>
     * The {@link #expand()} and {@link #computeSize()} methods must be called before this one.
     */
    public void computeHash(SweeperOperationListener listener) {
        if (!isExpanded()) {
            throw new IllegalStateException("Should be expanded");
        }
        if (!isSizeComputed()) {
            throw new IllegalStateException("Size not computed");
        }
        if (isHashComputed()) {
            return;
        }
        try {
            if (type == Type.FILE) {
                computeFileHash();
            } else {
                computeFolderHash();
            }
        } catch (Exception e) {
            listener.updateTargetException(this, SweeperTargetAction.COMPUTE_HASH, new SweeperException(e));
        }
        hashComputed = true;
        listener.updateTargetAction(this, SweeperTargetAction.COMPUTE_HASH);
    }

    private void computeFileHash() throws IOException, NoSuchAlgorithmException {
        modificationDate = new DateTime(resource.lastModified());
        InputStream stream = getResourceInputStream();
        try {
            hash = getTotalFiles() + HASH_SEPARATOR + getSize() + HASH_SEPARATOR + computeSha1(stream);
        } finally {
            stream.close();
        }
    }
    
    private InputStream getResourceInputStream() throws FileNotFoundException {
        return new FileInputStream(resource);
    }

    private String computeSha1(InputStream stream) throws NoSuchAlgorithmException, IOException {
        MessageDigest sha1 = getSha1Algorithm();
        if (sha1 == null) {
            throw new NoSuchAlgorithmException("SHA-1 algorithm provider not found");
        }
        byte[] buf = new byte[COMPUTE_SHA1_BUFFER_SIZE];
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
    
    private MessageDigest getSha1Algorithm() {
        if (sha1Algorithm == null) {
            try {
                sha1Algorithm = MessageDigest.getInstance("SHA-1");
            } catch(NoSuchAlgorithmException e) {
                // ignore
            }
        }
        return sha1Algorithm;
    }

    private void computeFolderSize() {
        size = 0;
        for (SweeperTargetImpl child : getChildren()) {
            if (!child.isSizeComputed()) {
                size = -1;
                throw new IllegalStateException("All the children need to be size computed");
            }
            size += child.getSize();
            totalFiles += child.getTotalFiles();
        }
    }
    
    private void computeFolderHash() throws NoSuchAlgorithmException, IOException {
        List<String> hashes = new ArrayList<String>();
        for (SweeperTargetImpl child : getChildren()) {
            if (!child.isHashComputed()) {
                throw new IllegalStateException("All the children need to be hash computed");
            }
            if (modificationDate == null || child.getModificationDate().isAfter(modificationDate)) {
                modificationDate = child.getModificationDate();
            }
            hashes.add(child.getHash());
        }
        if (hashes.size() == 1) {
            hash = hashes.get(0);
        } else {
            Collections.sort(hashes);
            StringBuilder sb = new StringBuilder();
            for (String s : hashes) {
                sb.append(s);
            }
            ByteArrayInputStream stream = new ByteArrayInputStream(sb.toString().getBytes());
            hash = getTotalFiles() + HASH_SEPARATOR + getSize() + HASH_SEPARATOR + computeSha1(stream);
        }
    }
    
    public List<SweeperTargetImpl> getChildren() {
        return children;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SweeperTargetImpl other = (SweeperTargetImpl) obj;
        return name.equals(other.name);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("name", name).add("type", type).add("mark", mark)
                .add("expanded", expanded).add("sizeComputed", sizeComputed).add("hashComputed", hashComputed)
                .add("size", size).add("modificationDate", modificationDate).add("hash", hash)
                .add("totalFiles", totalFiles).toString();
    }

    public int compareTo(SweeperTarget other) {
        Preconditions.checkNotNull(other);
        return ComparisonChain.start().compare(name, other.getName()).result();
    }

    public SweeperTargetImpl getParent() {
        return parent;
    }

    public int getTotalFiles() {
        if (!isSizeComputed()) {
            throw new IllegalStateException("not computed");
        }
        return totalFiles;
    }

}
