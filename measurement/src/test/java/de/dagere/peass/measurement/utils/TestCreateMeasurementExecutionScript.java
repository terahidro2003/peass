package de.dagere.peass.measurement.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import de.dagere.nodeDiffDetector.data.MethodCall;
import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.persistence.TestExecutionData;

public class TestCreateMeasurementExecutionScript {
   @Test
   public void testFromDependencies() throws IOException {
      ExecutionData executiondata = buildExecutionDataWithTests();

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (PrintStream ps = new PrintStream(baos)) {
         RunCommandWriter writer = new RunCommandWriter(new MeasurementConfig(30), ps, "experiment-1", executiondata);
         CreateScriptStarter.generateExecuteCommands(executiondata, "experiment-1", writer);
      }

      String result = baos.toString();
      System.out.println(result);
      
      MatcherAssert.assertThat(result, Matchers.containsString("-test Test1#testMe"));
      MatcherAssert.assertThat(result, Matchers.containsString("-test Test5#testMe"));
   }

   private ExecutionData buildExecutionDataWithTests() {
      StaticTestSelection dependencies = TestExecutionData.buildExampleDependencies();

      CommitStaticSelection version2 = dependencies.getCommits().get("000002");
      version2.getChangedClazzes().put(new MethodCall("Test1#testMe"), new TestSet(new TestMethodCall("Test1", "testMe")));
      
      CommitStaticSelection version5 = dependencies.getCommits().get("000005");
      version5.getChangedClazzes().put(new MethodCall("Test5#testMe"), new TestSet(new TestMethodCall("Test5", "testMe")));

      ExecutionData executiondata = new ExecutionData(dependencies);
      return executiondata;
   }
}
