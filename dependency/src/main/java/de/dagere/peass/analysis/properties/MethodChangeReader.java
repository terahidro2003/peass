package de.dagere.peass.analysis.properties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;

import de.dagere.nodeDiffDetector.data.MethodCall;
import de.dagere.nodeDiffDetector.diffDetection.FileComparisonUtil;
import de.dagere.nodeDiffDetector.sourceReading.MethodReader;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.traces.diff.DiffUtilJava;

public class MethodChangeReader {

   private final ChangedMethodManager manager;
   private final MethodCall clazz;

   private final String commit;

   private final String method, methodOld;

   public MethodChangeReader(final File methodSourceFolder, final File sourceFolder, final File oldSourceFolder, final MethodCall clazz, 
         final String commit, final ExecutionConfig config)
         throws FileNotFoundException {
      this.manager = new ChangedMethodManager(methodSourceFolder);
      this.clazz = clazz;
      this.commit = commit;
      
      method = MethodReader.getMethodSource(sourceFolder, clazz, config);
      methodOld = MethodReader.getMethodSource(oldSourceFolder, clazz, config);
   }

   public void readMethodChangeData() throws IOException {
      final File goalFile = manager.getMethodDiffFile(commit, clazz);
      if (!method.equals(methodOld)) {

         final File main = manager.getMethodMainFile(commit, clazz);
         final File old = manager.getMethodOldFile(commit, clazz);

         FileUtils.writeStringToFile(main, method, Charset.defaultCharset());
         FileUtils.writeStringToFile(old, methodOld, Charset.defaultCharset());
         DiffUtilJava.generateDiffFile(goalFile, Arrays.asList(new File[] { old, main }), "");
      } else {
         FileUtils.writeStringToFile(goalFile, method, Charset.defaultCharset());
      }
   }

   public Patch<String> getKeywordChanges(final MethodCall clazz) throws FileNotFoundException {
      final Patch<String> patch = DiffUtils.diff(Arrays.asList(method.split("\n")), Arrays.asList(methodOld.split("\n")));
      return patch;
   }
   
}
