package de.dagere.peass.measurement.rca.searcher;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.nodeDiffDetector.data.MethodCall;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.measurement.organize.FolderDeterminer;
import de.dagere.peass.measurement.rca.CausePersistenceManager;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.CauseTester;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.kieker.BothTreeReader;
import de.dagere.peass.measurement.rca.treeanalysis.LevelDifferentNodeDeterminer;

/**
 * Searches for root causes of performance change by levelwise measurement (like described in Heger et al.: Automated root cause isolation of performance regressions during software development)
 *
 */
public class LevelCauseSearcher extends CauseSearcher {

   private static final Logger LOG = LogManager.getLogger(LevelCauseSearcher.class);

   /**
    * Continues existing run
    * 
    * @param measurer
    * @param finishedData
    * @param folders
    * @throws InterruptedException
    * @throws IOException
    */
   public LevelCauseSearcher(final CauseTester measurer, final CausePersistenceManager persistenceManager, final EnvironmentVariables env) {
      super(null, persistenceManager.getRCAData().getCauseConfig(), measurer, persistenceManager.getRCAData().getMeasurementConfig(), persistenceManager.getFolders(), env);
      this.persistenceManager = persistenceManager;
   }

   public LevelCauseSearcher(final BothTreeReader reader, final CauseSearcherConfig causeSearchConfig, final CauseTester measurer, final MeasurementConfig measurementConfig,
         final CauseSearchFolders folders, final EnvironmentVariables env) {
      super(reader, causeSearchConfig, measurer, measurementConfig, folders, env);
      persistenceManager = new CausePersistenceManager(causeSearchConfig, measurementConfig, folders);

      final File potentialOldFolder = new File(folders.getArchiveResultFolder(measurementConfig.getFixedCommitConfig().getCommit(), causeSearchConfig.getTestCase()), "0");
      if (potentialOldFolder.exists()) {
         throw new RuntimeException("Old measurement folder " + potentialOldFolder.getAbsolutePath() + " exists - please cleanup!");
      }
      new FolderDeterminer(folders).testResultFolders(measurementConfig.getFixedCommitConfig().getCommit(), measurementConfig.getFixedCommitConfig().getCommitOld(),
            causeSearchConfig.getTestCase());
   }

   @Override
   protected Set<MethodCall> searchCause() {
      reader.getRootPredecessor().setOtherCommitNode(reader.getRootCurrent());
      reader.getRootCurrent().setOtherCommitNode(reader.getRootPredecessor());
      reader.getRootPredecessor().setOtherKiekerPattern(reader.getRootCurrent().getKiekerPattern());

      LevelChildDeterminer predecessorDeterminer = new LevelChildDeterminer(Arrays.asList(new CallTreeNode[] { reader.getRootPredecessor() }), causeSearchConfig.getLevels() - 1);
      List<CallTreeNode> initialNodesPredecessor = predecessorDeterminer.getSelectedIncludingParentNodes();
      LevelChildDeterminer currentDeterminer = new LevelChildDeterminer(Arrays.asList(new CallTreeNode[] { reader.getRootCurrent() }), causeSearchConfig.getLevels() - 1);
      List<CallTreeNode> initialNodesCurrent = currentDeterminer.getSelectedIncludingParentNodes();

      isLevelDifferent(initialNodesPredecessor, initialNodesCurrent);

      return convertToChangedEntitites();
   }

   public void isLevelDifferent(final List<CallTreeNode> currentPredecessorNodeList, final List<CallTreeNode> currentVersionNodeList) {
      final LevelDifferentNodeDeterminer levelDifferentNodeDeterminer = new LevelDifferentNodeDeterminer(currentPredecessorNodeList, currentVersionNodeList, causeSearchConfig,
            measurementConfig);

      final List<CallTreeNode> measurePredecessor = levelDifferentNodeDeterminer.getMeasurePredecessor();

      LOG.info("Measure next level: {}", measurePredecessor);
      if (measurePredecessor.size() > 0) {
         measureLevel(levelDifferentNodeDeterminer, measurePredecessor);
         writeTreeState();

         List<CallTreeNode> differentNodesPredecessor = LevelChildDeterminer.filterDeepestLevel(levelDifferentNodeDeterminer.getLevelDifferentPredecessor());
         List<CallTreeNode> measureChildsPredecessor = LevelChildDeterminer.getLevelChilds(differentNodesPredecessor, causeSearchConfig.getLevels());
         List<CallTreeNode> differentNodesCurrent = LevelChildDeterminer.filterDeepestLevel(levelDifferentNodeDeterminer.getLevelDifferentCurrent());
         List<CallTreeNode> measureChildsCurrent = LevelChildDeterminer.getLevelChilds(differentNodesCurrent, causeSearchConfig.getLevels());
         LOG.debug("Different nodes: {} {}", differentNodesPredecessor, differentNodesCurrent);
         LOG.debug("Nodes for remeasurement: {} {}", measureChildsPredecessor, measureChildsCurrent);

         isLevelDifferent(measureChildsPredecessor, measureChildsCurrent);
      }
   }

   private void measureLevel(final LevelDifferentNodeDeterminer levelDifferentNodeDeterminer, final List<CallTreeNode> measuredPredecessor) {
      measurer.measureCommit(measuredPredecessor);
      levelDifferentNodeDeterminer.calculateDiffering();

      for (final CallTreeNode predecessorNode : measuredPredecessor) {
         persistenceManager.addMeasurement(predecessorNode);
      }

      differingNodes.addAll(levelDifferentNodeDeterminer.getTreeStructureDifferingNodes());
      differingNodes.addAll(levelDifferentNodeDeterminer.getLevelDifferentPredecessor());
   }
}
