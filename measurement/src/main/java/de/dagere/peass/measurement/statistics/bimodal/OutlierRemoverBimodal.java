package de.dagere.peass.measurement.statistics.bimodal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.measurement.dataloading.MultipleVMTestUtil;
import de.dagere.peass.measurement.statistics.data.OutlierRemover;

public class OutlierRemoverBimodal {
   public OutlierRemoverBimodal(final List<VMResult> results) {
      final IsBimodal isBimodal = new IsBimodal(results);
      if (isBimodal.isBimodal()) {
         removeFromLeftDistribution(results, isBimodal);
         removeFromRightDistribution(results, isBimodal);
      } else {
         removeUnimodal(results);
      }
   }

   private void removeUnimodal(final List<VMResult> results) {
      SummaryStatistics statistics = MultipleVMTestUtil.getStatistic(results);
      for (Iterator<VMResult> iterator = results.iterator(); iterator.hasNext();) {
         VMResult r = iterator.next();
         double zscore = Math.abs(r.getValue() - statistics.getMean()) / statistics.getStandardDeviation();
         if (zscore > OutlierRemover.Z_SCORE) {
            iterator.remove();
         }
      }
   }

   private void removeFromRightDistribution(final List<VMResult> results, final IsBimodal isBimodal) {
      SummaryStatistics stat2 = isBimodal.getStat2();
      for (Iterator<VMResult> iterator = results.iterator(); iterator.hasNext();) {
         VMResult r = iterator.next();
         if (r.getValue() >= isBimodal.getAvgValue()) {
            double zscore = Math.abs(r.getValue() - stat2.getMean()) / stat2.getStandardDeviation();
            if (zscore > OutlierRemover.Z_SCORE) {
               iterator.remove();
            }
         }
      }
   }

   private void removeFromLeftDistribution(final List<VMResult> results, final IsBimodal isBimodal) {
      SummaryStatistics stat1 = isBimodal.getStat1();
      for (Iterator<VMResult> iterator = results.iterator(); iterator.hasNext();) {
         VMResult r = iterator.next();
         if (r.getValue() < isBimodal.getAvgValue()) {
            double zscore = Math.abs(r.getValue() - stat1.getMean()) / stat1.getStandardDeviation();
            if (zscore > OutlierRemover.Z_SCORE) {
               iterator.remove();
            }
         }
      }
   }

   public static CompareData removeOutliers(final CompareData data, final double outlierFactor) {
      final BimodalityTester isBismodal = new BimodalityTester(data);
      if (isBismodal.isBimodal()) {
         return removeOutlierBimodal(data, outlierFactor, isBismodal);
      } else {
         return removeOutliersSimple(data, outlierFactor);
      }
   }
   
   public static CompareData removeOutliersSimple(final CompareData data, final double outlierFactor) {
      CompareData result;
      double[] valuesPredecessor = removeOutliers(data.getPredecessor(), data.getPredecessorStat(), outlierFactor);
      double[] valuesCurrent = removeOutliers(data.getCurrent(), data.getCurrentStat(), outlierFactor);
      result = new CompareData(valuesPredecessor, valuesCurrent);
      return result;
   }

   private static CompareData removeOutlierBimodal(final CompareData data, final double outlierFactor, final BimodalityTester isBismodal) {
      double[] valuesPredecessor = removeOutliersBimodal(data.getPredecessor(), isBismodal.getDataPredecessor(), outlierFactor);
      double[] valuesCurrent = removeOutliersBimodal(data.getCurrent(), isBismodal.getDataAfter(), outlierFactor);
      CompareData result = new CompareData(valuesPredecessor, valuesCurrent);
      return result;
   }

   private static double[] removeOutliersBimodal(final double[] values, final IsBimodal data, final double outlierFactor) {
      List<Double> containedValues = new ArrayList<>(values.length);
      for (double value : values) {
         if (value < data.getAvgValue()) {
            double zscore = Math.abs(value - data.getStat1().getMean()) / data.getStat1().getStandardDeviation();
            if (!(zscore > outlierFactor)) {
               containedValues.add(value);
            }
         } else {
            double zscore = Math.abs(value - data.getStat2().getMean()) / data.getStat2().getStandardDeviation();
            if (!(zscore > outlierFactor)) {
               containedValues.add(value);
            }
         }
      }
      double[] containedResults = ArrayUtils.toPrimitive(containedValues.toArray(new Double[0]));
      return containedResults;
   }

   private static double[] removeOutliers(final double[] original, final StatisticalSummary statistics, final double outlierFactor) {
      List<Double> containedValues = new ArrayList<>(original.length);
      for (double value : original) {
         double zscore = Math.abs(value - statistics.getMean()) / statistics.getStandardDeviation();
         if (!(zscore > outlierFactor)) {
            containedValues.add(value);
         }
      }
      double[] containedResults = ArrayUtils.toPrimitive(containedValues.toArray(new Double[0]));
      return containedResults;
   }
}
