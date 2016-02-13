import org.codehaus.plexus.util.FileUtils;

assert (new File("${basedir}/boinc").isDirectory())
assert (new File("${basedir}/boinc", "app").isDirectory())
assert (new File("${basedir}/boinc", "templates").isDirectory())
assert (new File("${basedir}/boinc/templates", "app_in").exists())
assert (new File("${basedir}/boinc/templates", "app_out").exists())

assert (new File("${basedir}/boinc", "bin").isDirectory())
assert (new File("${basedir}/boinc/bin", "java_assimilator").exists())

def javaAssimilator = FileUtils.fileRead("${basedir}/boinc/bin/java_assimilator")
assert javaAssimilator.contains("java -Dboinc.assimilator.class=com.github.jkutner.boinc.BoincAssimilator -cp /app/boinc-project/download/helloworld-1.0-SNAPSHOT-jar-with-dependencies")
assert javaAssimilator.contains(".jar com.github.jkutner.boinc.BoincAssimilator \$@")

def defaultPlatforms = [
    "x86_64-apple-darwin",
    "i686-apple-darwin",
    "windows_intelx86",
    "windows_x86_64",
    "i686-pc-linux-gnu",
    "x86_64-pc-linux-gnu"
];

for (platform in defaultPlatforms) {
    assert (new File("${basedir}/boinc/app", platform).isDirectory())

    assert (new File("${basedir}/boinc/app/${platform}", "version.xml").exists())
    assert ((new File("${basedir}/boinc/app/${platform}", "wrapper_26014_${platform}").exists()) ||
            (new File("${basedir}/boinc/app/${platform}", "wrapper_26016_${platform}.exe").exists()))
    assert ((new File("${basedir}/boinc/app/${platform}", "mjava_v0.2_${platform}").exists()) ||
            (new File("${basedir}/boinc/app/${platform}", "mjava_v0.2_${platform}.exe").exists()))

    def versionXml = FileUtils.fileRead("${basedir}/boinc/app/${platform}/version.xml")
    assert versionXml.contains("<physical_name>helloworld-1.0-SNAPSHOT-jar-with-dependencies_");
    assert versionXml.contains("<logical_name>helloworld-1.0-SNAPSHOT-jar-with-dependencies.jar</logical_name>");
    assert (versionXml.contains("<physical_name>wrapper_26014_"+platform+"</physical_name>") ||
            versionXml.contains("<physical_name>wrapper_26016_"+platform+".exe</physical_name>"));
    assert versionXml.contains("<physical_name>job_"+platform+"_");
    assert versionXml.contains("<logical_name>job.xml</logical_name>");
    assert versionXml.contains("<logical_name>jdk.zip</logical_name>");
    assert versionXml.contains("<url>https://s3.amazonaws.com/boinc4j/openjdk-1.7.0-u80-unofficial-");

    def foundJobXml = false
    for (file in (new File("${basedir}/boinc/app/${platform}").listFiles())) {
        if (file.getName().startsWith("job_")) {
            def jobXml = FileUtils.fileRead(file)
            assert jobXml.contains("<job_desc>")
            assert jobXml.contains("<task>")
            assert jobXml.contains("<application>mjava")
            assert jobXml.contains("<command_line>--mjava-zip=jdk.zip --mjava-home=openjdk-1.7.0-u80-unofficial-")
            assert jobXml.contains("-jar helloworld-1.0-SNAPSHOT-jar-with-dependencies.jar</command_line>")
            foundJobXml = true
        }
    }
    assert foundJobXml
}


