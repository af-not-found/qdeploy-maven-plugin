package net.afnf.qdeploy;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.afnf.FileTestUtil;
import net.afnf.MockHttpResponse;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class QDeployMojoTest {

    static File testBaseDir;
    static File fromDir;
    static File toDir;

    static String jarlist = "file1.jar\t54\t1390000000000\n" + "file2.jar\t1395359\t1390000001000\n"
            + "file3.jar\t10485760\t1390000002000\n" + "file4.jar\t10485759\t1390000003000\n";
    static String[] lackJarlist;

    @BeforeClass
    static public void beforeClass() throws Exception {

        FileTestUtil.init();

        testBaseDir = new File(System.getProperty("java.io.tmpdir") + "/qdeploy-mojo-test");
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
    public void test01_GenerateJarList() throws Exception {

        QDeployMojo mj = new QDeployMojo();
        mj.jardir = new File(fromDir, "WEB-INF/lib/");

        {
            String ret = mj.generateJarList();
            assertEquals("", ret);
        }

        {
            File f1 = FileTestUtil.writeFile("WEB-INF/lib/file1.jar", fromDir, 10, 54);
            File f2 = FileTestUtil.writeFile("WEB-INF/lib/file2.jar", fromDir, 1571, 1395359);
            File f3 = FileTestUtil.writeFile("WEB-INF/lib/file3.jar", fromDir, 0, FileTestUtil.LEN);
            File f4 = FileTestUtil.writeFile("WEB-INF/lib/file4.jar", fromDir, 0, FileTestUtil.LEN - 1);
            FileTestUtil.writeFile("WEB-INF/lib/file5.jar.bad", fromDir, 113450, 11);
            FileTestUtil.writeFile("WEB-INF/lib/subdir/file6.jar", fromDir, 113450, 1173);
            FileTestUtil.writeFile("WEB-INF/file7.jar", fromDir, 113451, 11733);
            FileTestUtil.writeFile("file8.jar", fromDir, 113452, 21734);

            f1.setLastModified(1390000000000L);
            f2.setLastModified(1390000001000L);
            f3.setLastModified(1390000002000L);
            f4.setLastModified(1390000003000L);

            String ret = mj.generateJarList();
            assertEquals(jarlist, ret);
        }
    }

    @Test
    public void test02_GetLackJarlist() throws Exception {

        QDeployMojo mj = new QDeployMojo();

        ArgumentCaptor<HttpPost> captor = ArgumentCaptor.forClass(HttpPost.class);
        CloseableHttpResponse mockResponse = new MockHttpResponse(200, "file3.jar\nfile4.jar");
        CloseableHttpClient mockClient = Mockito.mock(CloseableHttpClient.class);
        Mockito.when(mockClient.execute(captor.capture())).thenReturn(mockResponse);
        mj.client = mockClient;

        lackJarlist = mj.getLackJarlist(jarlist);

        assertEquals(2, lackJarlist.length);
        assertEquals("file3.jar", lackJarlist[0]);
        assertEquals("file4.jar", lackJarlist[1]);

        List<HttpPost> allValues = captor.getAllValues();
        assertEquals(1, allValues.size());
        String actual = CommonUtil.getEntityString(allValues.get(0).getEntity());
        assertEquals(jarlist, actual);
    }

    @Test
    public void test03_Putjar() throws Exception {

        QDeployMojo mj = new QDeployMojo();
        mj.jardir = new File(fromDir, "WEB-INF/lib/");

        ArgumentCaptor<HttpPost> captor = ArgumentCaptor.forClass(HttpPost.class);
        CloseableHttpResponse mockResponse = new MockHttpResponse(200, "");
        CloseableHttpClient mockClient = Mockito.mock(CloseableHttpClient.class);
        Mockito.when(mockClient.execute(captor.capture())).thenReturn(mockResponse);
        mj.client = mockClient;

        mj.putjar(lackJarlist);

        List<HttpPost> allValues = captor.getAllValues();
        assertEquals(2, allValues.size());
        {
            FileEntity fe = (FileEntity) allValues.get(0).getEntity();
            assertArrayEquals(FileUtils.readFileToByteArray(new File(fromDir, "WEB-INF/lib/file3.jar")),
                    CommonUtil.getEntityBytes(fe));
        }
        {
            FileEntity fe = (FileEntity) allValues.get(1).getEntity();
            assertArrayEquals(FileUtils.readFileToByteArray(new File(fromDir, "WEB-INF/lib/file4.jar")),
                    CommonUtil.getEntityBytes(fe));
        }
    }

    @Test
    public void test04_CreateWarWithoutJar() throws Exception {

        QDeployMojo mj = new QDeployMojo();
        mj.webappDir = fromDir;
        mj.qdeployDir = new File(testBaseDir, "qdeploy");
        mj.warfile = new File(mj.qdeployDir, "test_from.war");

        mj.createWarWithoutJar();

        File extractdir = new File(testBaseDir, "extractdir1");
        CommonUtil.extractJarAll(mj.warfile, extractdir);

        List<File> files = new ArrayList<File>(FileUtils.listFiles(extractdir, null, true));
        Collections.sort(files);
        assertEquals(4, files.size());
        int i = 0;
        assertEquals("file8.jar", CommonUtil.getRelativePath(extractdir, files.get(i++)));
        assertEquals("WEB-INF/file7.jar", CommonUtil.getRelativePath(extractdir, files.get(i++)));
        assertEquals("WEB-INF/lib/file5.jar.bad", CommonUtil.getRelativePath(extractdir, files.get(i++)));
        assertEquals("WEB-INF/lib/subdir/file6.jar", CommonUtil.getRelativePath(extractdir, files.get(i++)));
    }

    @Test
    public void test05_Putwar() throws Exception {

        QDeployMojo mj = new QDeployMojo();
        mj.finalName = "context-path";
        mj.webappDir = fromDir;
        mj.qdeployDir = new File(testBaseDir, "qdeploy");
        mj.warfile = new File(mj.qdeployDir, "test_from.war");

        ArgumentCaptor<HttpPost> captor = ArgumentCaptor.forClass(HttpPost.class);
        CloseableHttpResponse mockResponse = new MockHttpResponse(200, "");
        CloseableHttpClient mockClient = Mockito.mock(CloseableHttpClient.class);
        Mockito.when(mockClient.execute(captor.capture())).thenReturn(mockResponse);
        mj.client = mockClient;

        mj.putwar();

        List<HttpPost> allValues = captor.getAllValues();
        assertEquals(1, allValues.size());
        {
            FileEntity fe = (FileEntity) allValues.get(0).getEntity();
            assertArrayEquals(FileUtils.readFileToByteArray(mj.warfile), CommonUtil.getEntityBytes(fe));
        }
    }

}
