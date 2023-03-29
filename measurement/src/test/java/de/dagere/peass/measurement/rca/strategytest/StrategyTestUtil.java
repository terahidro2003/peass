package de.dagere.peass.measurement.rca.strategytest;

import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;

import de.dagere.nodeDiffDetector.data.MethodCall;

public class StrategyTestUtil {
   public static void checkChanges(final Set<MethodCall> changes) {
      System.out.println(changes);
      Assert.assertEquals(3, changes.size());
      MatcherAssert.assertThat(changes.toString(), Matchers.containsString("ClassB#methodB"));
      MatcherAssert.assertThat(changes.toString(), Matchers.containsString("Test#test"));
      MatcherAssert.assertThat(changes.toString(), Matchers.containsString("ClassA#methodA"));
   }
}
