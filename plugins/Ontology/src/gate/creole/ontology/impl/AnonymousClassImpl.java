/*
 *  AnnonymousClassImpl.java
 *
 *  Niraj Aswani, 09/March/07
 *
 *  $Id$
 */
package gate.creole.ontology.impl;

import gate.creole.ontology.AnonymousClass;
import gate.creole.ontology.ONodeID;
import gate.creole.ontology.Ontology;

/**
 * Implementation of the AnonymousClass
 * @author niraj
 */
public class AnonymousClassImpl extends OClassImpl implements AnonymousClass {
  /**
   * Constructor
   * @param aURI
   * @param ontology
   * @param repositoryID
   * @param owlimPort
   */
  public AnonymousClassImpl(ONodeID aURI, Ontology ontology,
          OntologyService owlimPort) {
    super(aURI, ontology, owlimPort);
  }
}
