/*
 *  DynamicRegistrationTest.java
 *
 *  Copyright (c) 2010, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 */

package gate.creole.test;

import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.Gate;
import gate.ProcessingResource;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.SerialAnalyserController;
import gate.creole.metadata.CreoleResource;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test case for the dynamic registration system. Intended for use inside of TestGate.
 *
 */
public class DynamicRegistrationTest extends TestCase {

  @CreoleResource(name = "This is a test")
  public static class TestResource extends AbstractLanguageAnalyser {
    private static final long serialVersionUID = 1L;

    @Override
    public void execute() throws ExecutionException {

    }
  }


  public void testDynamicRegistration() throws Exception {
    Gate.getCreoleRegister().registerComponent(TestResource.class);

    SerialAnalyserController controller = (SerialAnalyserController)Factory.createResource("gate.creole.SerialAnalyserController",
            Factory.newFeatureMap(),
            Factory.newFeatureMap(),
            "basicRun");
    ProcessingResource testResource = (ProcessingResource)
      Factory.createResource(TestResource.class.getName());
    controller.add(testResource);
    Corpus corpus = Factory.newCorpus("basicTestCorpus");
    String engText = "This is the cereal shot from gnus.";
    Document doc = Factory.newDocument(engText);
    corpus.add(doc);
    controller.setCorpus(corpus);
    controller.setDocument(doc);
    controller.execute();
  }

  /** Test suite routine for the test runner */
  public static Test suite() {
    return new TestSuite(DynamicRegistrationTest.class);
  } // suite

}
