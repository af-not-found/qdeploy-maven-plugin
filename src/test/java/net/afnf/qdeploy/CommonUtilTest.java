package net.afnf.qdeploy;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.afnf.FileTestUtil;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CommonUtilTest {

    static File testBaseDir;
    static File fromDir;
    static File toDir;
    static Map<String, File> files = new HashMap<String, File>();

    @BeforeClass
    static public void beforeClass() throws Exception {

        FileTestUtil.init();

        testBaseDir = new File(System.getProperty("java.io.tmpdir") + "/qdeploy-test");
        System.out.println(testBaseDir.getAbsolutePath());
        FileUtils.deleteDirectory(testBaseDir);

        fromDir = new File(testBaseDir, "from");
        toDir = new File(testBaseDir, "to");

        fromDir.mkdirs();
        toDir.mkdirs();
    }

    @AfterClass
    static public void afterClass() throws Exception {
        FileUtils.deleteDirectory(testBaseDir);
    }

    @Test
    public void test01_jar() throws Exception {

        FileTestUtil.writeFile("file1.bin", fromDir, 0, 1, files);
        FileTestUtil.writeFile("file2.bin", fromDir, 1000, 1000, files);
        FileTestUtil.writeFile("dir1/file1.bin", fromDir, 10, 54, files);
        FileTestUtil.writeFile("dir1/file2.bin", fromDir, 1571, 1395359, files);
        FileTestUtil.writeFile("dir1/sub1/file1.bin", fromDir, 0, FileTestUtil.LEN, files);
        FileTestUtil.writeFile("dir1/sub1/file2.bin", fromDir, 0, FileTestUtil.LEN - 1, files);
        FileTestUtil.writeFile("dir2/ファイル1.bin", fromDir, 113450, 11, files);
        FileTestUtil.writeFile("dir2/file2.bin", fromDir, 113450, 1173, files);
        FileTestUtil.writeFile("dir2/sub1/ディレクトリ1/file1.bin", fromDir, 113450, 11853, files);

        File jarfile = new File(testBaseDir, "a.jar");
        CommonUtil.createJarFromDirectly(jarfile, fromDir);

        CommonUtil.extractJarAll(jarfile, toDir);

        FileTestUtil.assertDirEquals(fromDir, toDir, false);
    }

    @Test
    public void test02_getRelativePath() throws Exception {

        assertEquals("file1.bin", CommonUtil.getRelativePath(fromDir, files.get("file1.bin")));
        assertEquals("dir1/file1.bin", CommonUtil.getRelativePath(fromDir, files.get("dir1/file1.bin")));
        assertEquals("dir2/ファイル1.bin", CommonUtil.getRelativePath(fromDir, files.get("dir2/ファイル1.bin")));
        assertEquals("dir2/sub1/ディレクトリ1/file1.bin", CommonUtil.getRelativePath(fromDir, files.get("dir2/sub1/ディレクトリ1/file1.bin")));

        {
            File file = files.get("dir2/ファイル1.bin");
            String s1 = fromDir.getAbsolutePath().replaceAll("/", "\\");
            String s2 = file.getAbsolutePath().replaceAll("/", "\\");
            assertEquals("dir2/ファイル1.bin", CommonUtil.getRelativePath(new File(s1), new File(s2)));
        }

        {
            File file = files.get("dir2/ファイル1.bin");
            String s1 = fromDir.getAbsolutePath();
            String s2 = file.getAbsolutePath().replaceAll("/", "\\");
            assertEquals("dir2/ファイル1.bin", CommonUtil.getRelativePath(new File(s1), new File(s2)));
        }

        {
            File file = files.get("dir2/ファイル1.bin");
            String s1 = fromDir.getAbsolutePath().replaceAll("/", "\\");
            String s2 = file.getAbsolutePath();
            assertEquals("dir2/ファイル1.bin", CommonUtil.getRelativePath(new File(s1), new File(s2)));
        }
    }
}
