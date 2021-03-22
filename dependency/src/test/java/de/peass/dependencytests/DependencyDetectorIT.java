package de.peass.dependencytests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import de.peass.config.ExecutionConfig;
import de.peass.dependency.ChangeManager;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.changesreading.ClazzChangeData;
import de.peass.dependency.execution.EnvironmentVariables;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.reader.DependencyReader;
import de.peass.dependencytests.helper.FakeFileIterator;
import de.peass.vcs.VersionIterator;

public class DependencyDetectorIT {

   @Before
   public void initialize() throws IOException, InterruptedException {
      Assert.assertTrue(DependencyTestConstants.VERSIONS_FOLDER.exists());

      FileUtils.deleteDirectory(DependencyTestConstants.CURRENT);
      FileUtils.copyDirectory(DependencyTestConstants.BASIC_STATE, DependencyTestConstants.CURRENT);
      
   }

   @Test
   public void testNormalChange() throws IOException, InterruptedException, XmlPullParserException {
      final File secondVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "normal_change");

      final ChangeManager changeManager = DependencyDetectorTestUtil.defaultChangeManager();

      final VersionIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(secondVersion));

      final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator);

      System.out.println(reader.getDependencies());

      final TestSet testMe = DependencyDetectorTestUtil.findDependency(reader.getDependencies(), "defaultpackage.NormalDependency#executeThing", DependencyTestConstants.VERSION_1);
      final TestCase testcase = testMe.getTests().iterator().next();
      Assert.assertEquals("defaultpackage.TestMe", testcase.getClazz());
      Assert.assertEquals("testMe", testcase.getMethod());
   }

   @Test
   public void testTestChange() throws IOException, InterruptedException, XmlPullParserException {
      final File secondVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "changed_test");

      final Map<ChangedEntity, ClazzChangeData> changes = new TreeMap<>();
      DependencyDetectorTestUtil.addChange(changes, "", "defaultpackage.TestMe", "testMe");

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

      final VersionIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(secondVersion));

      final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator);

      System.out.println(reader.getDependencies().getVersions().get(DependencyTestConstants.VERSION_1));

      final TestSet testMe = DependencyDetectorTestUtil.findDependency(reader.getDependencies(), "defaultpackage.TestMe#testMe", DependencyTestConstants.VERSION_1);
      System.out.println(testMe);
      final TestCase testcase = testMe.getTests().iterator().next();
      Assert.assertEquals("defaultpackage.TestMe", testcase.getClazz());
      Assert.assertEquals("testMe", testcase.getMethod());
   }

   

   @Test
   public void testAddedClass() throws IOException, InterruptedException, XmlPullParserException {
      final File secondVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "added_class");

      final ChangeManager changeManager = DependencyDetectorTestUtil.mockAddedChangeManager();

      final VersionIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(secondVersion));

      final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator);

      System.out.println(reader.getDependencies());

      final TestSet testMeAlso = DependencyDetectorTestUtil.findDependency(reader.getDependencies(), "defaultpackage.TestMeAlso", DependencyTestConstants.VERSION_1);
      final TestCase testcase = testMeAlso.getTests().iterator().next();

      System.out.println(testMeAlso);
      Assert.assertEquals("defaultpackage.TestMeAlso", testcase.getClazz());
      Assert.assertEquals("testMe", testcase.getMethod());
   }

   @Test
   public void testClassChange() throws IOException, InterruptedException, XmlPullParserException {
      final File secondVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "changed_class");

      final Map<ChangedEntity, ClazzChangeData> changes = new TreeMap<>();
      changes.put(new ChangedEntity("defaultpackage.NormalDependency", ""), new ClazzChangeData("defaultpackage.NormalDependency", false));

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

      final VersionIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(secondVersion));

      final DependencyReader reader = new DependencyReader(new PeASSFolders(DependencyTestConstants.CURRENT), new File("/dev/null"), null, fakeIterator, changeManager, new ExecutionConfig(5), new EnvironmentVariables());
      final boolean success = reader.readInitialVersion();
      Assert.assertTrue(success);

      final Dependencies dependencies = reader.getDependencies();
      System.out.println(dependencies);

      fakeIterator.goToNextCommit();

      reader.analyseVersion(changeManager);

      System.out.println(dependencies);

      final TestSet testMe = DependencyDetectorTestUtil.findDependency(dependencies, "defaultpackage.NormalDependency", DependencyTestConstants.VERSION_1);

      System.out.println(testMe);
      final ChangedEntity change = dependencies.getVersions().get(DependencyTestConstants.VERSION_1).getChangedClazzes().keySet().iterator().next();
      Assert.assertEquals("defaultpackage.NormalDependency", change.toString());
      Assert.assertEquals("defaultpackage.TestMe#testMe", testMe.getTests().iterator().next().getExecutable());
   }

   /**
    * Tests removal of a method. In the first version, the method should not be called (but the other method of TestMe should be called, since the class interface changed). In the
    * second version, the changes should only influence TestMe.testMe, not TestMe.removeMe.
    * 
    * @throws IOException
    * @throws InterruptedException
    * @throws XmlPullParserException
    */
   @Test
   public void testRemoval() throws IOException, InterruptedException, XmlPullParserException {
      final File secondVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "removed_method");
      final File thirdVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "removed_method_change");

      final Map<ChangedEntity, ClazzChangeData> changes = new TreeMap<>();
      changes.put(new ChangedEntity("defaultpackage.TestMe", ""), new ClazzChangeData("defaultpackage.TestMe", false));

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

      final VersionIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(secondVersion, thirdVersion));

      final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator);

      Assert.assertEquals(1, reader.getDependencies().getVersions().get("000001").getChangedClazzes().size());

      fakeIterator.goToNextCommit();
      reader.analyseVersion(changeManager);

      System.out.println(reader.getDependencies());

      final TestSet testMe = DependencyDetectorTestUtil.findDependency(reader.getDependencies(), "defaultpackage.TestMe", DependencyTestConstants.VERSION_2);

      final TestCase test = testMe.getTests().iterator().next();
      Assert.assertEquals(1, testMe.getTests().size());
      Assert.assertEquals("defaultpackage.TestMe", test.getClazz());
      Assert.assertEquals("testMe", test.getMethod());
   }
   
   @Test
   public void testClassRemoval() throws IOException, InterruptedException, XmlPullParserException {
      final File secondVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "removed_class");

      final Map<ChangedEntity, ClazzChangeData> changes = new TreeMap<>();
      final ChangedEntity changedEntity = new ChangedEntity("src/test/java/defaultpackage/TestMe.java", "");
      changes.put(changedEntity, new ClazzChangeData(changedEntity, false));

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

      final VersionIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(secondVersion));

      final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator);

      final Map<ChangedEntity, TestSet> changedClazzes = reader.getDependencies().getVersions().get(DependencyTestConstants.VERSION_1).getChangedClazzes();
      System.out.println("Ergebnis: " + changedClazzes);
      final ChangedEntity key = new ChangedEntity("defaultpackage.TestMe", "");
      System.out.println("Hash: " + key.hashCode());
      final TestSet testSet = changedClazzes.get(key);
      System.out.println("Testset: " + testSet);
      Assert.assertThat(testSet.getTests(), Matchers.empty());
   }

   @Test
   public void testPackageChange() {

   }
}
