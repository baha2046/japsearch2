package org.nagoya.io;

import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.vfs2.*;
import org.jetbrains.annotations.NotNull;
import org.nagoya.commons.GUICommon;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;

public class LocalVFS {
    private static Option<FileSystemManager> fsManager = Option.none();

    public static Option<FileSystemManager> get() {
        if (fsManager.isEmpty()) {
            fsManager = Try.of(VFS::getManager).onFailure(GUICommon::errorDialog)
                    .toOption();
        }

        return fsManager;
    }

    public static Option<FileObject> resolveFile(String file) {
        return resolveFile(Path.of(file).toUri());
    }

    public static Option<FileObject> resolveFile(URI uri) {
        return get().flatMap(fs -> Try.of(() -> fs.resolveFile(uri)).onFailure(GUICommon::errorDialog).toOption());
    }

    public static long getSize(FileObject fileObject) {
        return Try.withResources(fileObject::getContent).of(FileContent::getSize).getOrElse(0L);
    }

    public static String fileNameToPath(@NotNull FileName fileName) {

        String root = fileName.getRootURI();
        if (!root.startsWith("file:")) {
            return fileName.getURI(); // nothing we can do about non-normal files. //$NON-NLS-1$
        }
        if (root.startsWith("file:////")) {
            return fileName.getURI();// fileName.getURI(); // we'll see 4 forward slashes for a windows/smb network share
        }
        if (root.endsWith(":/")) // Windows //$NON-NLS-1$
        {
            root = root.substring(8, 10);
        } else // *nix & OSX
        {
            root = ""; //$NON-NLS-1$
        }
        String fileString = root + fileName.getPath();

        if (!"/".equals(File.separator)) //$NON-NLS-1$
        {
            fileString = fileString.replace("/", File.separator); //$NON-NLS-1$
        }
        return fileString;
    }
}
