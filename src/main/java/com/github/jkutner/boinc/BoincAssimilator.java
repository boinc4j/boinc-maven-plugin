package com.github.jkutner.boinc;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class BoincAssimilator {

  public static final String ASSIMILATOR_CLASS_KEY="boinc.assimilator.class";

  // make package private once this is in the same package as BoincApp
  public static String buildJavaOpts(String assimilatorClassName) {
    if (assimilatorClassName != null) {
      return "-D"+ASSIMILATOR_CLASS_KEY+"=" + assimilatorClassName;
    } else {
      return "";
    }
  }

  public abstract void assimilate(String workUnitId, List<File> files) throws Exception;

  public abstract void assimilateError(String errorCode, String workUnitId) throws Exception;

  public Connection getConnection() throws SQLException {
    return DriverManager.getConnection(getJdbcUrl());
  }

  public String getJdbcUrl() {
    return System.getProperty("boinc.jdbc.database.url", System.getenv("JDBC_DATABASE_URL"));
  }

  public static void main(String[] args) throws Exception {
    String className = System.getProperty(ASSIMILATOR_CLASS_KEY);
    Class<? extends BoincAssimilator> assimilator = Class.forName(className).asSubclass(BoincAssimilator.class);

    if ("--error".equals(args[0])) {
      String code = args[1];
      String wuid = args[2];
      (assimilator.newInstance()).assimilateError(code, wuid);
    } else {
      String wuid = args[0];
      List<File> files = new ArrayList<>();
      for (int i = 1; i < args.length; i++) {
        File f = new File(args[i]);
        files.add(f);
      }
      (assimilator.newInstance()).assimilate(wuid, files);

    }
  }
}
