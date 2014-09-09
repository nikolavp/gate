/*
 * Copyright (c) 2011--2014, The University of Sheffield.
 *
 * This file is part of GATE (see http://gate.ac.uk/), and is free
 * software, licenced under the GNU Library General Public License,
 * Version 2, June1991.
 *
 * A copy of this licence is included in the distribution in the file
 * licence.html, and is also available at http://gate.ac.uk/gate/licence.html.
 *
 *  $Id$
 */

package gate.corpora.twitter;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Factory;
import gate.FeatureMap;
import gate.util.InvalidOffsetException;


/**
 * This is a wrapper around the data that we will want to
 * turn into an Annotation later, but here it's a floater, not attached
 * to any Document or AnnotationSet.  Used to hold data in the Tweet
 * model.
 */
public class PreAnnotation  {
  private FeatureMap features;
  private String type;
  private long start, end;
  
  
  public PreAnnotation(long start, long end, String type, FeatureMap features) {
    if (features == null) {
      this.features = Factory.newFeatureMap();
    }
    else {
      this.features = features;
    }
    
    this.type = type;
    this.setStart(start);
    this.setEnd(end);
  }
  
  
  public PreAnnotation(long start, long end, String type) {
    this.features = Factory.newFeatureMap();
    this.type = type;
    this.setStart(start);
    this.setEnd(end);
  }
  
  
  public Annotation toAnnotation(AnnotationSet outputAS, long startOffset) throws InvalidOffsetException {
    long outputStart = this.start + startOffset;
    long outputEnd   = this.end + startOffset; 
    Integer id = outputAS.add(outputStart, outputEnd, type, features);
    return outputAS.get(id);
  }
  
  
  public void setStart(long start) {
    this.start = start;
  }

  public void setEnd(long end) {
    this.end = end;
  }
  
  public FeatureMap getFeatures() {
    return this.features;
  }

  public void setFeatures(FeatureMap features) {
    this.features = features;
  }

  public String getType() {
    return this.type;
  }

  public long getStart() {
    return this.start;
  }
  
  public long getEnd() {
    return this.end;
  }

}

