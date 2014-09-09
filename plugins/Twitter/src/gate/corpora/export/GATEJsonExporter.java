package gate.corpora.export;

/*
 *  Copyright (c) 1995-2014, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Mark A. Greenwood 17/07/2014
 *
 */

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.DocumentExporter;
import gate.FeatureMap;
import gate.corpora.DocumentJsonUtils;
import gate.creole.metadata.AutoInstance;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@CreoleResource(name = "GATE JSON Exporter", tool = true, autoinstances = @AutoInstance, icon = "GATEJSON")
public class GATEJsonExporter extends DocumentExporter {

  private static final long serialVersionUID = -8087536348560365618L;

  private String annotationSetName;
  
  private Set<String> annotationTypes;

  public String getAnnotationSetName() {
    return annotationSetName;
  }

  @RunTime
  @Optional
  @CreoleParameter
  public void setAnnotationSetName(String annotationSetName) {
    this.annotationSetName = annotationSetName;
  }

  public Set<String> getAnnotationTypes() {
    return annotationTypes;
  }

  @RunTime
  @CreoleParameter
  public void setAnnotationTypes(Set<String> annotationTypes) {
    this.annotationTypes = annotationTypes;
  }

  public GATEJsonExporter() {
    super("GATE JSON", "json","application/json");
  }

  @SuppressWarnings("unchecked")
  @Override
  public void export(Document doc, OutputStream out, FeatureMap options)
    throws IOException {

    AnnotationSet annots =
      doc.getAnnotations((String)options.get("annotationSetName"));
    
    Collection<String> types = (Collection<String>)options.get("annotationTypes");
    
    Map<String,Collection<Annotation>> annotationsMap = new HashMap<String,Collection<Annotation>>();
    
    for (String type : types) {
      annotationsMap.put(type, annots.get(type));
    }

    DocumentJsonUtils.writeDocument(doc, annotationsMap, out);
  }
}
