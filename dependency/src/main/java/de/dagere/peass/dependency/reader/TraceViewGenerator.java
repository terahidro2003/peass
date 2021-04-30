package de.dagere.peass.dependency.reader;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.github.javaparser.ParseException;

import de.dagere.peass.dependency.DependencyManager;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.ResultsFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.persistence.Version;
import de.dagere.peass.dependency.traces.KiekerFolderUtil;
import de.dagere.peass.dependency.traces.OneTraceGenerator;
import de.dagere.peass.dependency.traces.TraceFileMapping;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;

public class TraceViewGenerator {
   
   private final DependencyManager dependencyManager;
   private final PeASSFolders folders;
   private final String version;
   private final TraceFileMapping mapping;
   
   public TraceViewGenerator(final DependencyManager dependencyManager, final PeASSFolders folders, final String version, final TraceFileMapping mapping) {
      this.dependencyManager = dependencyManager;
      this.folders = folders;
      this.version = version;
      this.mapping = mapping;
   }

   public boolean generateViews(final ResultsFolders resultsFolders, final Version newVersionInfo) throws IOException, XmlPullParserException, ParseException, ViewNotFoundException {
      boolean allWorked = true;
      for (TestCase testcase : newVersionInfo.getTests().getTests()) {
         final File moduleFolder = KiekerFolderUtil.getModuleResultFolder(folders, testcase);
         final OneTraceGenerator oneViewGenerator = new OneTraceGenerator(resultsFolders, folders, testcase, mapping, version, moduleFolder,
               dependencyManager.getExecutor().getModules());
          final boolean workedLocal = oneViewGenerator.generateTrace(version);
          allWorked &= workedLocal;
      }
      return allWorked;
   }
}