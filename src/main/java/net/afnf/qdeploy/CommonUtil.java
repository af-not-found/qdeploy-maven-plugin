package net.afnf.qdeploy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;

public class CommonUtil {

    public static final String CHARSET = "UTF-8";

    public static String CMD_JARLIST = "/jarlist";
    public static String CMD_PUTJAR = "/putjar";
    public static String CMD_PUTWAR = "/putwar";
    public static String[] CMDS = { CMD_JARLIST, CMD_PUTJAR, CMD_PUTWAR };

    public static String getProperty(String key) {
        String value = System.getProperty(key);
        if (StringUtils.isBlank(value)) {
            value = System.getenv(key);
        }
        return value;
    }

    public static String getRelativePath(File base, File file) {
        String basePath = base.getAbsolutePath();
        int baselen = basePath.length();
        String filePath = file.getAbsolutePath();
        return filePath.substring(baselen + 1).replaceAll("\\\\", "/");
    }

    public static String getEntityString(HttpEntity entity) throws Exception {
        InputStream inputStream = entity.getContent();
        try {
            return IOUtils.toString(inputStream, CommonUtil.CHARSET);
        }
        finally {
            inputStream.close();
        }
    }

    public static byte[] getEntityBytes(HttpEntity entity) throws Exception {
        InputStream inputStream = entity.getContent();
        try {
            return IOUtils.toByteArray(inputStream);
        }
        finally {
            inputStream.close();
        }
    }

    public static void extractJarAll(File jarfile, File outdir) throws IOException {

        JarFile jf = null;
        try {

            if (outdir.exists()) {
                FileUtils.deleteDirectory(outdir);
            }
            else {
                outdir.mkdirs();
            }

            jf = new JarFile(jarfile);
            Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) entries.nextElement();
                File file = new File(outdir, jarEntry.getName());

                if (jarEntry.isDirectory()) {
                    file.mkdir();
                }
                else {
                    InputStream is = jf.getInputStream(jarEntry);
                    try {
                        FileUtils.copyInputStreamToFile(is, file);
                        file.setLastModified(jarEntry.getTime());
                    }
                    finally {
                        IOUtils.closeQuietly(is);
                    }
                }
            }
        }
        finally {
            if (jf != null) {
                jf.close();
            }
        }
    }

    public static void createJarFromDirectly(File jarfile, File indir) throws IOException {

        ZipOutputStream zos = null;
        try {
            jarfile.getParentFile().mkdirs();

            zos = new ZipOutputStream(FileUtils.openOutputStream(jarfile));

            Collection<File> files = FileUtils.listFiles(indir, null, true);
            for (File file : files) {
                ZipEntry zipEntry = new ZipEntry(getRelativePath(indir, file));
                zipEntry.setTime(file.lastModified());
                zos.putNextEntry(zipEntry);
                FileUtils.copyFile(file, zos);
                zos.closeEntry();
            }
        }
        finally {
            IOUtils.closeQuietly(zos);
        }
    }
}
