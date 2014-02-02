package net.afnf;

import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.afnf.qdeploy.CommonUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class FileTestUtil {

    public static final int LEN = 1024 * 1024 * 10;
    public static byte[] data;

    public static void init() {
        data = new byte[LEN];
        for (int i = 0; i < LEN; i++) {
            data[i] = (byte) (Math.random() * 256);
        }
    }

    public static File writeFile(String path, File fromDir, int offset, int len) throws Exception {
        return writeFile(path, fromDir, offset, len, null);
    }

    public static File writeFile(String path, File fromDir, int offset, int len, Map<String, File> files) throws Exception {
        File f = new File(fromDir, path);
        f.getParentFile().mkdirs();
        OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
        os.write(data, offset, len);
        os.flush();
        IOUtils.closeQuietly(os);
        if (files != null) {
            files.put(path, f);
        }
        return f;
    }

    public static void assertDirEquals(File fromDir, File toDir) throws Exception {
        assertDirEquals(fromDir, toDir, true);
    }

    public static void assertDirEquals(File fromDir, File toDir, boolean exactlyLastmod) throws Exception {

        List<File> fromFiles = new ArrayList<File>(FileUtils.listFiles(fromDir, null, true));
        Collections.sort(fromFiles);
        List<File> toFiles = new ArrayList<File>(FileUtils.listFiles(toDir, null, true));
        Collections.sort(toFiles);

        int size = fromFiles.size();
        assertEquals(size, toFiles.size());
        for (int i = 0; i < size; i++) {
            File f1 = fromFiles.get(i);
            File f2 = toFiles.get(i);
            assertEquals(CommonUtil.getRelativePath(fromDir, f1), CommonUtil.getRelativePath(toDir, f2));
            assertEquals(f1.length(), f2.length());
            assertArrayEquals(FileUtils.readFileToByteArray(f1), FileUtils.readFileToByteArray(f2));

            if (exactlyLastmod) {
                assertEquals(f1.lastModified(), f2.lastModified());
            }
            else {
                assertThat(Math.abs(f1.lastModified() - f2.lastModified()), lessThan(2000L));
            }
        }
    }
}
