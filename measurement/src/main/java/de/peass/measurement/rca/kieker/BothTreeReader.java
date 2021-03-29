package de.peass.measurement.rca.kieker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.execution.EnvironmentVariables;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.rca.CauseSearcherConfig;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.treeanalysis.TreeUtil;
import de.peass.utils.Constants;
import kieker.analysis.exception.AnalysisConfigurationException;

public class BothTreeReader {

   private static final Logger LOG = LogManager.getLogger(BothTreeReader.class);

   private CallTreeNode rootPredecessor;
   private CallTreeNode rootVersion;

   private final CauseSearcherConfig causeSearchConfig;
   private final MeasurementConfiguration config;
   private final CauseSearchFolders folders;
   private final EnvironmentVariables env;
   
   final File potentialCacheFileOld;
   final File potentialCacheFile;

   public BothTreeReader(final CauseSearcherConfig causeSearchConfig, final MeasurementConfiguration config, final CauseSearchFolders folders, final EnvironmentVariables env) {
      this.causeSearchConfig = causeSearchConfig;
      this.config = config;
      this.folders = folders;
      this.env = env;
      
      potentialCacheFileOld = new File(folders.getTreeCacheFolder(config.getVersion(), causeSearchConfig.getTestCase()), config.getVersionOld());
      potentialCacheFile = new File(folders.getTreeCacheFolder(config.getVersion(), causeSearchConfig.getTestCase()), config.getVersion());
   }
   
   public void readCachedTrees() throws JsonParseException, JsonMappingException, IOException {
      if (!potentialCacheFile.exists() || !potentialCacheFileOld.exists()) {
         throw new RuntimeException("Cache not existing! " + potentialCacheFile.getAbsolutePath());
      }
      rootPredecessor = Constants.OBJECTMAPPER.readValue(potentialCacheFileOld, CallTreeNode.class);
      rootVersion = Constants.OBJECTMAPPER.readValue(potentialCacheFile, CallTreeNode.class);
      
      rootPredecessor.setConfig(config);
      setConfig(rootPredecessor);
      rootVersion.setConfig(config);
      setConfig(rootVersion);
   }

   private void setConfig(final CallTreeNode node) {
      for (CallTreeNode child : node.getChildren()) {
         child.setConfig(config);
         setConfig(child);
      }
   }
   
   public void readTrees() throws InterruptedException, IOException, XmlPullParserException, ViewNotFoundException, AnalysisConfigurationException {
      if (potentialCacheFile.exists() && potentialCacheFileOld.exists()) {
         LOG.info("Using cache!");
         readCachedTrees();
      } else {
         determineTrees();
         LOG.info("Writing to cache");
         Constants.OBJECTMAPPER.writeValue(potentialCacheFileOld, rootPredecessor);
         Constants.OBJECTMAPPER.writeValue(potentialCacheFile, rootVersion);
      }
   }

   private void determineTrees() throws InterruptedException, IOException, FileNotFoundException, XmlPullParserException, ViewNotFoundException, AnalysisConfigurationException {
      final TreeReader resultsManager = TreeReaderFactory.createTreeReader(folders, config.getVersionOld(), config, causeSearchConfig.isIgnoreEOIs(), env);
      rootPredecessor = resultsManager.getTree(causeSearchConfig.getTestCase(), config.getVersionOld());

      final TreeReader resultsManagerPrevious = TreeReaderFactory.createTreeReader(folders, config.getVersion(), config, causeSearchConfig.isIgnoreEOIs(), env);
      rootVersion = resultsManagerPrevious.getTree(causeSearchConfig.getTestCase(), config.getVersion());
      LOG.info("Traces equal: {}", TreeUtil.areTracesEqual(rootPredecessor, rootVersion));
   }

   public CallTreeNode getRootPredecessor() {
      return rootPredecessor;
   }

   public CallTreeNode getRootVersion() {
      return rootVersion;
   }

   public EnvironmentVariables getEnv() {
      return env;
   }
}
