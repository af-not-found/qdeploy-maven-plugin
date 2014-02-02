package net.afnf.qdeploy;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * Deploy war file to the webappDir. qdeploy-maven-webapp is required.
 * 
 * @goal deploy
 */
public class QDeployMojo extends AbstractMojo {

    /**
     * @parameter expression="${project.build.finalName}"
     * @required
     */
    protected String finalName;

    /**
     * @parameter expression="${deployUrl}"
     * @required
     */
    protected String deployUrl;

    /**
     * @parameter expression="${webappDir}"
     * @required
     */
    protected File webappDir;

    protected Log logger = getLog();

    protected String key;
    protected CloseableHttpClient client;
    protected File qdeployDir;
    protected File warfile;
    protected File jardir;

    public void execute() throws MojoExecutionException {

        try {

            if (prepare() == false) {
                return;
            }

            String jarlist = generateJarList();

            String[] lackJarlist = getLackJarlist(jarlist);

            putjar(lackJarlist);

            createWarWithoutJar();

            putwar();
        }
        catch (MojoExecutionException e) {
            throw e;
        }
        catch (Exception e) {
            throw new MojoExecutionException("failed", e);
        }
        finally {
            IOUtils.closeQuietly(client);
        }
    }

    protected boolean prepare() {

        key = CommonUtil.getProperty("QDEPLOY_KEY");
        if (StringUtils.isBlank(key)) {
            logger.error("QDEPLOY_KEY is not defined");
            return false;
        }

        File tmpdir = new File(System.getProperty("java.io.tmpdir"));
        if (tmpdir.exists() == false || tmpdir.isDirectory() == false) {
            logger.error("tmpdir does not exist :" + tmpdir.getAbsolutePath());
            return false;
        }

        qdeployDir = new File(tmpdir, "qdeploy");
        warfile = new File(qdeployDir, finalName + "_from.war");

        jardir = new File(webappDir, "/WEB-INF/lib/");
        if (jardir.exists() == false) {
            logger.info("jardir does not exist : " + jardir.getAbsolutePath());
        }

        client = HttpClientBuilder.create().build();

        return true;
    }

    protected String generateJarList() {

        File[] jars = jardir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
        logger.info("jar count : " + (jars == null ? 0 : jars.length));

        StringBuilder sb = new StringBuilder();
        if (jars != null) {
            for (File jar : jars) {
                sb.append(jar.getName());
                sb.append("\t");
                sb.append(jar.length());
                sb.append("\t");
                sb.append(jar.lastModified());
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    protected String[] getLackJarlist(String jarlist) throws Exception {

        String[] lackJarlist = null;

        HttpPost post = new HttpPost(deployUrl + CommonUtil.CMD_JARLIST + "?path=" + finalName + "&key=" + key);
        post.setEntity(new StringEntity(jarlist));

        CloseableHttpResponse response = client.execute(post);
        try {
            if (isSuccess(response)) {
                String lackJarlistStr = CommonUtil.getEntityString(response.getEntity());
                lackJarlist = StringUtils.split(lackJarlistStr, "\n");
                logger.info("jarlist OK, lack=" + lackJarlist.length);
            }
            else {
                error(response, "jarlist");
            }
        }
        finally {
            response.close();
        }

        return lackJarlist;
    }

    protected void putjar(String[] lackJarlist) throws Exception {

        for (String jarname : lackJarlist) {
            File jarfile = new File(jardir, jarname);

            HttpPost post = new HttpPost(deployUrl + CommonUtil.CMD_PUTJAR + "?path=" + finalName + "&jarname=" + jarname
                    + "&lastmod=" + jarfile.lastModified() + "&key=" + key);
            post.setEntity(new FileEntity(jarfile));

            CloseableHttpResponse response = client.execute(post);
            try {
                if (isSuccess(response)) {
                    logger.info("putjar OK, jarname=" + jarname);
                }
                else {
                    error(response, "putwar");
                }
            }
            finally {
                response.close();
            }
        }
    }

    protected void createWarWithoutJar() throws Exception {

        Collection<File> filesWithoutJar = FileUtils.listFiles(webappDir, new IOFileFilter() {
            public boolean accept(File file) {
                String dirname = file.getParentFile().getAbsolutePath().replaceAll("\\\\", "/");
                return file.isFile() && false == (dirname.endsWith("WEB-INF/lib") && file.getAbsolutePath().endsWith(".jar"));
            }

            public boolean accept(File dir, String name) {
                return true;
            }
        }, TrueFileFilter.TRUE);

        File partdir = new File(qdeployDir, finalName + "_from");
        FileUtils.deleteDirectory(partdir);
        for (File file : filesWithoutJar) {
            //logger.debug(file.getAbsolutePath());
            File dest = new File(partdir, CommonUtil.getRelativePath(webappDir, file));
            FileUtils.copyFile(file, dest, true);
        }

        CommonUtil.createJarFromDirectly(warfile, partdir);
    }

    protected void putwar() throws Exception {

        HttpPost post = new HttpPost(deployUrl + CommonUtil.CMD_PUTWAR + "?path=" + finalName + "&key=" + key);
        post.setEntity(new FileEntity(warfile));

        CloseableHttpResponse response = client.execute(post);
        try {
            if (isSuccess(response)) {
                logger.info("putwar OK");
            }
            else {
                error(response, "putwar");
            }
        }
        finally {
            response.close();
        }
    }

    protected boolean isSuccess(CloseableHttpResponse response) {
        boolean success = response.getStatusLine().getStatusCode() == 200;
        return success;
    }

    protected void error(CloseableHttpResponse response, String phase) throws Exception {

        String msg = "unknown";
        try {
            msg = CommonUtil.getEntityString(response.getEntity());
        }
        catch (Exception e) {
            logger.warn(e);
        }

        String str = phase + " failed, " + msg;
        logger.error(str);

        throw new MojoExecutionException(str);
    }

    public static void main(String[] args) throws MojoExecutionException {
        QDeployMojo m = new QDeployMojo();
        m.deployUrl = "http://localhost:8080/qdeploy";
        m.webappDir = new File("D:/Java/git/blog-java1/target/blog-java1");
        m.finalName = "blog";
        m.execute();
    }
}
