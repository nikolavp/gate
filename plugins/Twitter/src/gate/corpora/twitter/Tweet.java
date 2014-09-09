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

import gate.Factory;
import gate.FeatureMap;
import gate.util.Strings;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringEscapeUtils;
import com.fasterxml.jackson.databind.JsonNode;


public class Tweet {
  private String string;
  private long start;
  private Set<PreAnnotation> annotations;
  
  
  public Set<PreAnnotation> getAnnotations() {
    return this.annotations;
  }
  
  public int getLength() {
    return this.string.length();
  }

  public String getString() {
    return this.string;
  }
  
  public long getStart() {
    return this.start;
  }
  
  public long getEnd() {
    return this.start + this.string.length();
  }
  
  
  public static Tweet readTweet(JsonNode json, List<String> contentKeys, List<String> featureKeys) {
    if ( (contentKeys == null) || (featureKeys == null) ) {
      return new Tweet(json);
    }

    // implied else
    return new Tweet(json, contentKeys, featureKeys);
  }


  /**
   * Used by the JSONTWeetFormat; the DocumentContent contains only the main text;
   * the annotation feature map contains all the other JSON data, recursively.
   */
  private Tweet(JsonNode json) {
    string = "";
    Iterator<String> keys = json.fieldNames();
    FeatureMap features = Factory.newFeatureMap();
    annotations = new HashSet<PreAnnotation>();

    while (keys.hasNext()) {
      String key = keys.next();

      if (key.equals(TweetUtils.DEFAULT_TEXT_ATTRIBUTE)) {
        string = StringEscapeUtils.unescapeHtml(json.get(key).asText());
      }
      else {
        features.put(key.toString(), TweetUtils.process(json.get(key)));
      }
    }
    
    annotations.add(new PreAnnotation(0L, string.length(), TweetUtils.TWEET_ANNOTATION_TYPE, features));
  }
  

  /** Used by the fancier corpus population system to handle options.
   * @param contentKeys JSON paths whose values should be converted to String and
   * added to the DocumentContent
   * @param featureKeys JSON paths whose values should be stored in the main
   * annotation's features
   */
  private Tweet(JsonNode json, List<String> contentKeys, List<String> featureKeys) {
    StringBuilder content = new StringBuilder();
    List<String> keepers = new ArrayList<String>();
    keepers.addAll(contentKeys);
    keepers.addAll(featureKeys);
    this.annotations = new HashSet<PreAnnotation>();

    FeatureMap featuresFound = TweetUtils.process(json, keepers);

    // Put the DocumentContent together from the contentKeys' values found in the JSON.
    for (String cKey : contentKeys) {
      if (featuresFound.containsKey(cKey)) {
        int start = content.length();
        // Use GATE's String conversion in case there are maps or lists.
        content.append(Strings.toString(featuresFound.get(cKey)));
        this.annotations.add(new PreAnnotation(start, content.length(), cKey));
        content.append('\n');
      }
    }
    
    // Get the featureKeys & their values for the main annotation.
    FeatureMap annoFeatures = Factory.newFeatureMap();
    for (String fKey : featureKeys) {
      if (featuresFound.containsKey(fKey)) {
        annoFeatures.put(fKey, featuresFound.get(fKey));
      }
    }
    
    // Create the main annotation and the content.
    this.annotations.add(new PreAnnotation(0, content.length(), TweetUtils.TWEET_ANNOTATION_TYPE, annoFeatures));
    this.string = content.toString();
  }

  
}
