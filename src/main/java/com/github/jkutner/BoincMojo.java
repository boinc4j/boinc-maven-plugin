package com.github.jkutner;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @goal package
 * @execute phase="package"
 * @requiresDependencyResolution
 */
public class BoincMojo extends AbstractMojo {

  /**
   * The current Maven session.
   *
   * @parameter property="session"
   * @required
   * @readonly
   */
  protected MavenSession mavenSession;

  /**
   * The Maven BuildPluginManager component.
   *
   * @component
   * @required
   */
  protected BuildPluginManager pluginManager;

  /**
   * The project currently being build.
   *
   * @parameter property="project"
   * @required
   * @readonly
   */
  protected MavenProject mavenProject;

  /**
   * @parameter property="project.build.directory"
   * @readonly
   */
  private File outputPath;

  /**
   * List of platforms to support or not support.
   *
   * @parameter property="boinc.platforms"
   */
  protected Map<String,String> platforms = new HashMap<String, String>();

  /**
   * A the location of the generic job.xml
   *
   * @parameter property="boinc.jobFile"
   */
  protected File jobFile = null;

  /**
   * A the location of the templates dir
   *
   * @parameter property="boinc.templatesDir"
   */
  protected File templatesDir = null;

  /**
   * A unique key for the version (defaults to Git hash)
   *
   * @parameter property="boinc.versionKey"
   */
  protected String versionKey = null;

  /**
   * A standalone JAR file containing your app and all of it's dependencies
   *
   * @parameter property="boinc.uberjar"
   */
  protected File uberjar = null;

  public void execute() throws MojoExecutionException, MojoFailureException {

    // create an uberjar

    if (jobFile == null) jobFile = new File(mavenProject.getBasedir(), "src/main/resources/boinc/app/job.xml");
    if (templatesDir == null) templatesDir = new File(mavenProject.getBasedir(), "src/main/resources/boinc/templates");
    //if (uberjar == null) uberjar = new File(mavenProject.getBuild().getOutputDirectory(), mavenProject.getBuild().getm

    if (uberjar == null) {
//      if (!"war".equals(mavenProject.getPackaging())) {
//        throw new MojoExecutionException("Your packaging must be set to 'jar' or you must define the '<uberjar>' config!");
//      } else {
        File targetDir = new File(mavenProject.getBasedir(), "target");

        File[] files = targetDir.listFiles(new FilenameFilter() {
          public boolean accept(File dir, String name) {
            return name.endsWith(".jar");
          }
        });
        if (files.length == 0) {
          throw new MojoFailureException("Could not find WAR file! Must specify file path in plugin configuration.");
        } else {
          uberjar = files[0];
        }
//      }
    }

    Map<String,Boolean> altPlatforms = new HashMap<String, Boolean>();
    for (String s : platforms.keySet())
      altPlatforms.put(s, "true".equals(platforms.get(s)));

    BoincApp app = new BoincApp(
        uberjar,
        altPlatforms,
        jobFile,
        templatesDir,
        versionKey
    );

    try {
      app.packageIntoBoincDir();
    } catch (Exception e) {
      throw new MojoExecutionException("Error packaging for BOINC", e);
    }
  }

}
