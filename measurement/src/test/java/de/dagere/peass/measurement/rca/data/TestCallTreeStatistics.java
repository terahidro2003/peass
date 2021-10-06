package de.dagere.peass.measurement.rca.data;

import org.hamcrest.number.IsNaN;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.MeasurementConfiguration;

public class TestCallTreeStatistics {
   
   public static final MeasurementConfiguration CONFIG = new MeasurementConfiguration(10);
   
   @BeforeEach
   public void init() {
      CONFIG.getExecutionConfig().setVersionOld("B");
      CONFIG.getExecutionConfig().setVersion("A");
   }
   
   @Test
   public void testStatistics() {
      final CallTreeNode node = new CallTreeNode("de.mypackage.Test#callMethod", "public void de.mypackage.Test.callMethod()", "public void de.mypackage.Test.callMethod()", CONFIG);
      final CallTreeNode otherVersionNode = new CallTreeNode("de.mypackage.Test#callMethod", "public void de.mypackage.Test.callMethod()", "public void de.mypackage.Test.callMethod()", CONFIG);
      node.setOtherVersionNode(otherVersionNode);

      node.initVersions();
      for (int vm = 0; vm < CONFIG.getVms(); vm++) {
         addVMMeasurements("A", node);
         addVMMeasurements("B", node);
      }
      node.createStatistics("A");
      node.createStatistics("B");

      Assert.assertEquals(15, node.getStatistics("A").getMean(), 0.01);
      Assert.assertEquals(15, node.getStatistics("B").getMean(), 0.01);

      Assert.assertEquals(10, node.getTestcaseStatistic().getVMs());
      Assert.assertEquals(150, node.getTestcaseStatistic().getCallsOld());
      Assert.assertEquals(150, node.getTestcaseStatistic().getCalls());
   }

   @Test
   public void testStatisticsADDED() {
      final CallTreeNode node = new CallTreeNode(CauseSearchData.ADDED, CauseSearchData.ADDED, "public void de.mypackage.Test.callMethod()", CONFIG);
      final CallTreeNode otherVersionNode = new CallTreeNode("de.mypackage.Test#callMethod", "public void de.mypackage.Test.callMethod()", CauseSearchData.ADDED, CONFIG);
      node.setOtherVersionNode(otherVersionNode);

      node.initVersions();
      for (int vm = 0; vm < CONFIG.getVms(); vm++) {
         addVMMeasurements("A", node);
      }
      node.createStatistics("A");
      node.createStatistics("B");

      Assert.assertEquals(15, node.getStatistics("A").getMean(), 0.01);
      Assert.assertThat(node.getStatistics("B").getMean(), IsNaN.notANumber());

      Assert.assertEquals(10, node.getStatistics("A").getN());
      Assert.assertEquals(0, node.getStatistics("B").getN());

      Assert.assertEquals(10, node.getPartialTestcaseStatistic().getVMs());
      Assert.assertEquals(0, node.getPartialTestcaseStatistic().getCallsOld());
      Assert.assertEquals(150, node.getPartialTestcaseStatistic().getCalls());
   }

   @Test
   public void testStatisticsADDEDNew() {
      final CallTreeNode node = new CallTreeNode("de.mypackage.Test#callMethod", "public void de.mypackage.Test.callMethod()", CauseSearchData.ADDED, CONFIG);
      final CallTreeNode otherVersionNode = new CallTreeNode(CauseSearchData.ADDED, CauseSearchData.ADDED, "public void de.mypackage.Test.callMethod()", CONFIG);
      node.setOtherVersionNode(otherVersionNode);

      node.initVersions();
      for (int vm = 0; vm < CONFIG.getVms(); vm++) {
         addVMMeasurements("B", node);
      }
      node.createStatistics("A");
      node.createStatistics("B");

      Assert.assertEquals(15, node.getStatistics("B").getMean(), 0.01);
      Assert.assertThat(node.getStatistics("A").getMean(), IsNaN.notANumber());

      Assert.assertEquals(10, node.getStatistics("B").getN());
      Assert.assertEquals(0, node.getStatistics("A").getN());

      Assert.assertEquals(10, node.getPartialTestcaseStatistic().getVMs());
      Assert.assertEquals(150, node.getPartialTestcaseStatistic().getCallsOld());
      Assert.assertEquals(0, node.getPartialTestcaseStatistic().getCalls());
   }

   private void addVMMeasurements(final String version, final CallTreeNode node) {
      node.newVM(version);
      for (int i = 0; i < 15; i++) {
         node.addMeasurement(version, 15L);
      }
   }
}
