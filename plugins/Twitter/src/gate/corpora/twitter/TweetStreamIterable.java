/*
 *  Copyright (c) 1995-2014, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *  
 *  $Id$
 */
package gate.corpora.twitter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;


/**
 * Iterable version, just to make loops easier.
 * @author adam
 *
 */
public class TweetStreamIterable implements Iterable<Tweet> {

  private InputStream input;
  private List<String> contentKeys, featureKeys;
  private boolean gzip;
  private TweetStreamIterator iterator;
  
  private static final Logger logger = Logger.getLogger(TweetStreamIterable.class.getName());
  
  public TweetStreamIterable(InputStream input, List<String> contentKeys,
          List<String> featureKeys, boolean gzip) {
    
    this.input = input;
    this.contentKeys = contentKeys;
    this.featureKeys = featureKeys;
    this.gzip = gzip;
    this.iterator = null;
  }

  
  @Override
  public Iterator<Tweet> iterator() {
    try {
      this.iterator = new TweetStreamIterator(input, contentKeys, featureKeys, gzip);
      return this.iterator;
    } 
    catch(IOException e) {
      logger.warn("Internal error in TweetStreamIterator", e);
      // The Override won't let us throw an exception up.
      return Collections.<Tweet>emptyList().iterator();
    }
  }

  
  public void close() {
    if (this.iterator != null) {
      try {
        this.iterator.close();
      } 
      catch(IOException e) {
        logger.warn("Internal error in TweetStreamIterator", e);
      }
    }
  }
  
}
