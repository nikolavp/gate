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

import gate.AnnotationSet;
import gate.Corpus;
import gate.Document;
import gate.DocumentContent;
import gate.Factory;
import gate.Gate;
import gate.corpora.DocumentContentImpl;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.AutoInstance;
import gate.creole.metadata.CreoleResource;
import gate.gui.NameBearerHandle;
import gate.gui.ResourceHelper;
import gate.util.InvalidOffsetException;
import java.awt.event.ActionEvent;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;


@CreoleResource(name = "Twitter Corpus Populator", tool = true, autoinstances = @AutoInstance,
    comment = "Populate a corpus from Twitter JSON containing multiple Tweets",
    helpURL = "http://gate.ac.uk/userguide/sec:social:twitter:format")
public class Population extends ResourceHelper  {

  private static final long serialVersionUID = 1443073039199794668L;

  
  public static void populateCorpus(final Corpus corpus, URL inputUrl, PopulationConfig config) 
      throws ResourceInstantiationException {
    populateCorpus(corpus, inputUrl, config.getEncoding(), config.getContentKeys(), 
        config.getFeatureKeys(), config.getTweetsPerDoc());
  }
  
  /**
   * 
   * @param corpus
   * @param inputUrl
   * @param encoding
   * @param contentKeys
   * @param featureKeys
   * @param tweetsPerDoc 0 = put them all in one document; otherwise the number per document
   * @throws ResourceInstantiationException
   */
  public static void populateCorpus(final Corpus corpus, URL inputUrl, String encoding, List<String> contentKeys,
      List<String> featureKeys, int tweetsPerDoc) throws ResourceInstantiationException {
    try {
      InputStream input = inputUrl.openStream();
      List<String> lines = IOUtils.readLines(input, encoding);
      IOUtils.closeQuietly(input);
      
      // TODO: sort this out so it processes one at a time instead of reading the
      // whole hog into memory
      
      // For now, we assume the streaming API format (concatenated maps, not in a list)
      List<Tweet> tweets = TweetUtils.readTweetStrings(lines, contentKeys, featureKeys);
      
      int digits = (int) Math.ceil(Math.log10(tweets.size()));
      int tweetCounter = 0;
      Document document = newDocument(inputUrl, tweetCounter, digits);
      StringBuilder content = new StringBuilder();
      Map<PreAnnotation, Integer> annotandaOffsets = new HashMap<PreAnnotation, Integer>();
      
      for (Tweet tweet : tweets) {
        if ( (tweetsPerDoc > 0) && (tweetCounter > 0) && ((tweetCounter % tweetsPerDoc) == 0) ) {
          closeDocument(document, content, annotandaOffsets, corpus);
          document = newDocument(inputUrl, tweetCounter, digits);
          content = new StringBuilder();
          annotandaOffsets = new HashMap<PreAnnotation, Integer>();
        }

        int startOffset = content.length();
        content.append(tweet.getString());
        for (PreAnnotation preAnn : tweet.getAnnotations()) {
          annotandaOffsets.put(preAnn, startOffset);
        }

        content.append('\n');
        tweetCounter++;
      } // end of Tweet loop
      
      if (content.length() > 0) {
        closeDocument(document, content, annotandaOffsets, corpus);
      }
      else {
        Factory.deleteResource(document);
      }
      
      if(corpus.getDataStore() != null) {
        corpus.getDataStore().sync(corpus);
      }
      
    }
    catch (Exception e) {
      throw new ResourceInstantiationException(e);
    }
  }


  private static Document newDocument(URL url, int counter, int digits) throws ResourceInstantiationException {
    Document document = Factory.newDocument("");
    String code = StringUtils.leftPad(Integer.toString(counter), digits, '0');
    String name = StringUtils.stripToEmpty(StringUtils.substring(url.getPath(), 1)) + "_" + code;
    document.setName(name);
    document.setSourceUrl(url);
    document.getFeatures().put(Document.DOCUMENT_MIME_TYPE_PARAMETER_NAME, TweetUtils.MIME_TYPE);
    document.getFeatures().put("gate.SourceURL", url.toString());
    return document;
  }
  
  
  private static void closeDocument(Document document, StringBuilder content, Map<PreAnnotation, Integer> annotandaOffsets, Corpus corpus) throws InvalidOffsetException {
    DocumentContent contentImpl = new DocumentContentImpl(content.toString());
    document.setContent(contentImpl);
    AnnotationSet originalMarkups = document.getAnnotations(Gate.ORIGINAL_MARKUPS_ANNOT_SET_NAME);
    for (PreAnnotation preAnn : annotandaOffsets.keySet()) {
      preAnn.toAnnotation(originalMarkups, annotandaOffsets.get(preAnn));
    }
    corpus.add(document);
    
    if (corpus.getLRPersistenceId() != null) {
      corpus.unloadDocument(document);
      Factory.deleteResource(document);
    }
  }

  
  @Override
  protected List<Action> buildActions(final NameBearerHandle handle) {
    List<Action> actions = new ArrayList<Action>();

    if(!(handle.getTarget() instanceof Corpus)) return actions;

    actions.add(new AbstractAction("Populate from Twitter JSON files") {
      private static final long serialVersionUID = -8511779592856786327L;

      @Override
      public void actionPerformed(ActionEvent e)  {
        final PopulationDialogWrapper dialog = new PopulationDialogWrapper();

        // If no files were selected then just stop
        try {
          final List<URL> fileUrls = dialog.getFileUrls();
          if ( (fileUrls == null) || fileUrls.isEmpty() ) {
            return;
          }
          
          // Run the population in a separate thread so we don't lock up the GUI
          Thread thread =
              new Thread(Thread.currentThread().getThreadGroup(),
                  "Twitter JSON Corpus Populator") {
                public void run() {
                  try {
                    for (URL fileUrl : fileUrls) {
                      populateCorpus((Corpus) handle.getTarget(), fileUrl, dialog.getEncoding(), 
                          dialog.getContentKeys(), dialog.getFeatureKeys(), dialog.getTweetsPerDoc());
                    } 
                  }
                  catch(ResourceInstantiationException e) {
                    e.printStackTrace();
                  }
                }
              };
          thread.setPriority(Thread.MIN_PRIORITY);
          thread.start();
        }
        catch(MalformedURLException e0) {
          e0.printStackTrace();
        }
      }
    });

    return actions;
  }

}
