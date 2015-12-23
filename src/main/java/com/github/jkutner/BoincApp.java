package com.github.jkutner;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.xembly.Directives;
import org.xembly.ImpossibleModificationException;
import org.xembly.Xembler;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BoincApp {

  public static final String DEFAULT_WRAPPER_VERSION="26016";

  private static String[] defaultPlatforms = new String[] {
      "x86_64-apple-darwin",
      "i686-apple-darwin",
      "windows_intelx86",
      "windows_x86_64",
      "i686-pc-linux-gnu",
      "x86_64-pc-linux-gnu"
  };

  private Set<String> platforms;

  private File boincDir = new File(System.getProperty("user.dir"), "boinc");

  private File srcUberjar;

  private File srcJobXml;

  private File srcTemplatesDir;

  private String versionKey;

  public BoincApp(
      File uberjar,
      Map<String,Boolean> altPlatforms,
      File jobXml,
      File templatesDir,
      String versionKey
  ) {
    platforms = new HashSet<String>();
    for (String p : altPlatforms.keySet())
      if (altPlatforms.get(p)) platforms.add(p);

    for (String p : defaultPlatforms)
      if (!altPlatforms.containsKey(p) || altPlatforms.get(p)) platforms.add(p);

    this.srcUberjar = uberjar;
    this.srcJobXml = jobXml;
    this.srcTemplatesDir = templatesDir;
    this.versionKey = versionKey;
  }

  public void cleanBoincDir(Boolean keepWrapper) throws IOException {
    if (this.boincDir.exists()) {
      if (keepWrapper) {
        for (File f : FileUtils.listFiles(this.boincDir, new WrapperFilter(), TrueFileFilter.INSTANCE)) {
          if (!f.isDirectory()) {
            FileUtils.forceDelete(f);
          }
        }
      } else {
        FileUtils.deleteDirectory(this.boincDir);
      }
    }
  }

  private static class WrapperFilter implements IOFileFilter {
    public boolean accept(File file) {
      return !"zip".equals(FilenameUtils.getExtension(file.getName()));
    }

    public boolean accept(File file, String s) {
      return !"zip".equals(FilenameUtils.getExtension(s));
    }
  }

  public void packageIntoBoincDir() throws IOException, ImpossibleModificationException, ZipException {
    cleanBoincDir(true);

    FileUtils.forceMkdir(boincDir);

    File appDir = new File(boincDir, "app");
    FileUtils.forceMkdir(appDir);

    File templatesDir = new File(boincDir, "templates");
    FileUtils.copyDirectory(this.srcTemplatesDir, templatesDir);

    File downloadsDir = new File(boincDir, "download");
    FileUtils.forceMkdir(downloadsDir);

    for (String p : platforms) {
      Map<String,File> files = new HashMap<String, File>();

      File platformDir = new File(appDir, p);
      FileUtils.forceMkdir(platformDir);

      File uberjar = new File(platformDir, this.srcUberjar.getName());
      FileUtils.copyFile(this.srcUberjar, uberjar);

      files.put(uberjar.getName(), uberjar);
      files.put("job.xml", copyJobXml(platformDir, p));
      files.put("wrapper", installWrapper(platformDir, p));
      createVersionFile(platformDir, files);
      createComposerJson();
    }
  }

  protected File copyJobXml(File platformDir, String platform) throws IOException {
    String jobFilename = "job_"+platform+"_"+this.versionKey+".xml";
    File jobFile = new File(platformDir, jobFilename);
    FileUtils.copyFile(this.srcJobXml, jobFile);
    return jobFile;
  }

  protected File installWrapper(File platformDir, String platform) throws IOException, ZipException {
    String wrapperZipFilename = wrapperName(platform)+".zip";
    File wrapperZipFile = new File(platformDir, wrapperZipFilename);

    System.out.println("Downloading " + wrapperZipFilename + "...");

    String urlString = System.getProperty(
        "boinc.wrapper." + platform + ".url",
        "http://boinc.berkeley.edu/dl/" + wrapperZipFilename);
    URL wrapperUrl = new URL(urlString);

    // TODO make better
    FileUtils.copyURLToFile(wrapperUrl, wrapperZipFile);

    System.out.println("Extracting " + wrapperZipFilename + "...");
    ZipFile zipFile = new ZipFile(wrapperZipFile);
    zipFile.extractAll(platformDir.toString());

    FileUtils.forceDelete(wrapperZipFile);

    return new File(platformDir, wrapperName(platform)+wrapperExtension(platform));
  }

  protected void createVersionFile(File platformDir, Map<String,File> files)
      throws ImpossibleModificationException, IOException {
    Directives version = new Directives().add("version");

    for (String logicalName : files.keySet()) {
      File physicalFile = files.get(logicalName);
      Directives fileXml = version.add("file")
        .add("physical_name").set(physicalFile.getName()).up()
        .add("copy_file").set("true").up();
      if (logicalName.equals("wrapper")) {
        fileXml.add("main_program").set("true").up();
      } else {
        fileXml.add("logical_name").set(logicalName).up();
      }
      fileXml.up();
    }

    String xml = new Xembler(version).xml();
    File versionFile = new File(platformDir, "version.xml");
    FileUtils.writeStringToFile(versionFile, xml);
  }

  protected void createComposerJson() throws IOException {
    File composerJson = new File(System.getProperty("usr.dir"), "composer.json");
    if (!composerJson.exists())
      FileUtils.writeStringToFile(composerJson, "{}");
  }

  protected String wrapperName(String platform) {
    String wrapperVersion = System.getProperty("boinc.wrapper.version", wrapperVersion(platform));
    return "wrapper_"+wrapperVersion+"_"+platform;
  }

  protected String wrapperVersion(String platform) {
    if (platform.startsWith("windows_"))
      return "26016";
    return "26014";
  }

  protected String wrapperExtension(String platform) {
    if (platform.startsWith("windows_"))
      return ".exe";
    return "";
  }
}
