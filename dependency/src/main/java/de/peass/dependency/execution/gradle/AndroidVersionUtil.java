package de.peass.dependency.execution.gradle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.execution.GradleParseUtil;

public class AndroidVersionUtil {

   private static final Logger LOG = LogManager.getLogger(AndroidVersionUtil.class);

   private static Map<Integer, String> versions = new LinkedHashMap<>();
   private static Set<String> acceptedVersion = new HashSet<>();

   static {
      final ClassLoader classLoader = GradleParseUtil.class.getClassLoader();
      final File versionFile = new File(classLoader.getResource("versions.txt").getFile());
      if (versionFile.exists()) {
         try {
            final List<String> runningAndroidVersions = Files.readAllLines(Paths.get(versionFile.toURI()));
            for (final String line : runningAndroidVersions) {
               final String version = line.substring(line.indexOf(';') + 1);
               versions.put(getMajorVersion(version), version);
               acceptedVersion.add(version);
            }
         } catch (final IOException e) {
            e.printStackTrace();
         }
      } else {
         LOG.error("No version file existing!");
      }

      final File gradle = new File(System.getenv("user.home"), ".gradle");
      if (!gradle.exists()) {
         gradle.mkdir();
      }
   }

   public static boolean isLegelBuildTools(String versionString) {
      return !acceptedVersion.contains(versionString) &&
            !versionString.equals("rootProject.buildToolsVersion") &&
            !versionString.equals("rootProject.compileSdkVersion") &&
            !versionString.equals("androidCompileSdkVersion.toInteger") &&
            !versionString.equals("buildConfig.buildTools");
   }
   
   public static boolean isLegalBuildToolsVersion(String versionString) {
      return !acceptedVersion.contains(versionString) 
            && !versionString.equals("rootProject.buildToolsVersion") 
            && !versionString.equals("rootProject.compileSdkVersion")
            && !versionString.equals("androidBuildToolsVersion")
            && !versionString.equals("androidCompileSdkVersion.toInteger()")
            && !versionString.equals("buildConfig.buildTools");
   }

   private static int getMajorVersion(final String versionString) {
      final int dotIndex = versionString.indexOf('.');
      if (dotIndex != -1) {
         final String part = versionString.substring(0, dotIndex);
         final int parsed = Integer.parseInt(part);
         return parsed;
      } else {
         return Integer.parseInt(versionString);
      }

   }

   public static String getRunningVersion(String versionString) {
      int majorVersion = getMajorVersion(versionString);
      return versions.get(majorVersion);
   }
}
