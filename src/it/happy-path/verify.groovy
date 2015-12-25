import org.codehaus.plexus.util.FileUtils;

assert (new File("${basedir}/boinc").isDirectory())
assert (new File("${basedir}/boinc", "app").isDirectory())
assert (new File("${basedir}/boinc", "templates").isDirectory())
assert (new File("${basedir}/boinc/templates", "app_in").exists())
assert (new File("${basedir}/boinc/templates", "app_out").exists())

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

    def versionXml = FileUtils.fileRead("${basedir}/boinc/app/${platform}/version.xml")
    assert versionXml.contains("<physical_name>helloworld-1.0-SNAPSHOT-jar-with-dependencies_");
    assert versionXml.contains("<logical_name>helloworld-1.0-SNAPSHOT-jar-with-dependencies.jar</logical_name>");
    assert (versionXml.contains("<physical_name>wrapper_26014_"+platform+"</physical_name>") ||
            versionXml.contains("<physical_name>wrapper_26016_"+platform+".exe</physical_name>"));
    assert versionXml.contains("<physical_name>job_"+platform+"_");
    assert versionXml.contains("<logical_name>job.xml</logical_name>");

    def foundJobXml = false
    for (file in (new File("${basedir}/boinc/app/${platform}").listFiles())) {
        if (file.getName().startsWith("job_")) {
            def jobXml = FileUtils.fileRead(file)
            assert jobXml.contains("<job_desc>")
            assert jobXml.contains("<task>")
            assert jobXml.contains("<application>/usr/bin/java</application>")
            assert jobXml.contains("<command_line>-jar helloworld-1.0-SNAPSHOT-jar-with-dependencies.jar</command_line>")
            foundJobXml = true
        }
    }
    assert foundJobXml
}


