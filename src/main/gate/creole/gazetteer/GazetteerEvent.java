/*  GazetteerEvent.java
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  borislav popov 08/05/2002
 *
 *  $Id$
 */
package gate.creole.gazetteer;

import gate.event.GateEvent;

/**
 * Gazetteer Event to be used whenever an event needs to be fired and spread
 * among Gazetteer Listeners */
public class GazetteerEvent extends GateEvent {
  
  private static final long serialVersionUID = 1824667976328958501L;

  /**gazetteer reinitialized event*/
  public static final int REINIT = 1;

  /**Creates a gazetteer event
   * @param source the Object that generated the event
   * @param type the tupe of the event
   */
  public GazetteerEvent(Object source,int type) {
    super(source,type);
  }

} // class GazetteerEvent