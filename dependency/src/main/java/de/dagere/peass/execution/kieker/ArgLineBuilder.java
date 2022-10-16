package de.dagere.peass.execution.kieker;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.execution.maven.pom.MavenPomUtil;
import de.dagere.peass.testtransformation.TestTransformer;

public class ArgLineBuilder {

   private static final Logger LOG = LogManager.getLogger(ArgLineBuilder.class);

   public static final String TEMP_DIR_PURE = "java.io.tmpdir";
   public static final String TEMP_DIR = "-D" + TEMP_DIR_PURE;
   private static final String KIEKER_CONFIGURATION_PURE = "kieker.monitoring.configuration";
   private static final String KIEKER_CONFIGURATION = "-D" + KIEKER_CONFIGURATION_PURE;
   private static final String MONITORING_PROPERTIES_PATH = "/src/main/resources/META-INF/kieker.monitoring.properties";

   public static final String JAVA_AGENT = "-javaagent";

   public static final String RELATIVE_MAVEN_FOLDER = ".m2" + File.separator + "repository" + File.separator + "net" + File.separator
         + "kieker-monitoring" + File.separator + "kieker" + File.separator + MavenPomUtil.KIEKER_VERSION + File.separator + "kieker-"
         + MavenPomUtil.KIEKER_VERSION + "-aspectj.jar";
   public static final String KIEKER_FOLDER_MAVEN = "${user.home}" + File.separator + RELATIVE_MAVEN_FOLDER;
   /**
    * This is added to surefire, assuming that kieker has been downloaded already, so that the aspectj-weaving can take place.
    */
   protected static final String KIEKER_ARG_LINE_MAVEN = JAVA_AGENT + ":" + KIEKER_FOLDER_MAVEN;

   public static final String KIEKER_FOLDER_GRADLE = "${System.properties['user.home']}" + File.separator + RELATIVE_MAVEN_FOLDER;

   protected static final String KIEKER_ARG_LINE_GRADLE = JAVA_AGENT + ":" + KIEKER_FOLDER_GRADLE;

   private final TestTransformer testTransformer;
   private final File modulePath;

   public ArgLineBuilder(final TestTransformer testTransformer, final File modulePath) {
      this.testTransformer = testTransformer;
      this.modulePath = modulePath;
   }

   public String buildArglineMaven(final File tempFolder) {
      final String argLine = buildGenericArgline(tempFolder, "=", " ", KIEKER_ARG_LINE_MAVEN);
      LOG.debug("Created gradle argLine: {}", argLine);
      return argLine;
   }

   // TODO Since Gradle requires different argument specification with systemProperty, this is not realy generic anymore - or maybe in the future again for sbt?
   protected String buildGenericArgline(final File tempFolder, final String valueSeparator, final String entrySeparator, final String kiekerLine) {
      String argline = getTieredCompilationArglinePart(entrySeparator);
      if (testTransformer.getConfig().getKiekerConfig().isUseKieker()) {
         final String tempFolderPath = "'" + tempFolder.getAbsolutePath() + "'";
         if (testTransformer.getConfig().getKiekerConfig().isUseSourceInstrumentation() && !testTransformer.getConfig().getKiekerConfig().isOnlyOneCallRecording()) {
            argline += TEMP_DIR + valueSeparator + tempFolderPath;

         } else {
            argline += kiekerLine + entrySeparator + TEMP_DIR + valueSeparator + tempFolderPath;
         }
         if (!entrySeparator.contains("\"")) {
            argline += " " + KIEKER_CONFIGURATION + valueSeparator + "\"" + modulePath.getAbsolutePath() + MONITORING_PROPERTIES_PATH + "\"";
         } else {
            argline += entrySeparator + KIEKER_CONFIGURATION + valueSeparator + "'" + modulePath.getAbsolutePath()
                  + MONITORING_PROPERTIES_PATH + "'";
         }
      }
      return argline;
   }

   private String getTieredCompilationArglinePart(final String entrySeparator) {
      String argline;
      if (testTransformer.getConfig().getExecutionConfig().isUseTieredCompilation()) {
         argline = "-XX:-TieredCompilation" + entrySeparator;
      } else {
         argline = "";
      }
      return argline;
   }

   public String buildSystemPropertiesGradle(final File tempFolder) {
      // final String argline = buildGenericArgline(tempFolder, ":", "\",\"", KIEKER_ARG_LINE_GRADLE);
      if (testTransformer.getConfig().getKiekerConfig().isUseKieker()) {
         String tempPathNoEscapes = tempFolder.getAbsolutePath().replace('\\', '/');
         String systemProperties = "  systemProperty \"" + TEMP_DIR_PURE + "\", \"" + tempPathNoEscapes + "\"" + System.lineSeparator();
         String configFilePath = modulePath.getAbsolutePath().replace('\\', '/') + MONITORING_PROPERTIES_PATH;
         systemProperties += "  systemProperty \"" + KIEKER_CONFIGURATION_PURE + "\", \"" + configFilePath + "\"" + System.lineSeparator();

         LOG.debug("Created gradle properties: {}", systemProperties);

         return systemProperties;
      } else {
         return "";
      }
   }

   public Map<String, String> getGradleSystemProperties(final File tempFolder) {
      Map<String, String> properties = new LinkedHashMap<>();
      if (testTransformer.getConfig().getKiekerConfig().isUseKieker()) {
         String tempPathNoEscapes = tempFolder.getAbsolutePath().replace('\\', '/');
         properties.put(TEMP_DIR_PURE, tempPathNoEscapes);

         String configFilePath = modulePath.getAbsolutePath().replace('\\', '/') + MONITORING_PROPERTIES_PATH;
         properties.put(KIEKER_CONFIGURATION_PURE, configFilePath);
      } else {
         String tempPathNoEscapes = tempFolder.getAbsolutePath().replace('\\', '/');
         properties.put(TEMP_DIR_PURE, tempPathNoEscapes);
      }
      return properties;
   }

   public String getJVMArgs() {
      String potentialXmxArgLine = getXmxArgLine("");
      if (!testTransformer.getConfig().getKiekerConfig().isUseSourceInstrumentation() || testTransformer.getConfig().getKiekerConfig().isOnlyOneCallRecording()) {
         return "  jvmArgs=[\"" + KIEKER_ARG_LINE_GRADLE + "\"," + potentialXmxArgLine + "]";
      } else if (potentialXmxArgLine.length() > 0) {
         return "  jvmArgs=[" + potentialXmxArgLine + "]";
      } else {
         return null;
      }
   }

   private static final Pattern XMX_PATTERN = Pattern.compile("-Xmx[0-9]*[m,g]");

   public String getJVMArgs(String oldArgLine) {
      String changedArgLine = getXmxArgLine(oldArgLine);

      if (!testTransformer.getConfig().getKiekerConfig().isUseSourceInstrumentation() || testTransformer.getConfig().getKiekerConfig().isOnlyOneCallRecording()) {
         return "  jvmArgs=[\"" + KIEKER_ARG_LINE_GRADLE + "\", " + changedArgLine + "]";
      } else {
         return changedArgLine;
      }
   }

   private String getXmxArgLine(String oldArgLine) {
      if (testTransformer.getConfig().getExecutionConfig().getXmx() != null) {
         Matcher matcher = XMX_PATTERN.matcher(oldArgLine);
         String changedArgLine;
         if (matcher.find()) {
            String xmxString = "-Xmx" + testTransformer.getConfig().getExecutionConfig().getXmx();
            changedArgLine = matcher.replaceFirst(xmxString);
         } else {
            changedArgLine = "\"-Xmx" + testTransformer.getConfig().getExecutionConfig().getXmx()+"\"";
         }
         return changedArgLine;
      } else {
         return oldArgLine;
      }
   }
}
