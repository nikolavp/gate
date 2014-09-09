/*
	Dummy.java

	Hamish Cunningham, 13/06/00

	$Id$
*/


package testpkg;

import java.io.*;

/** A dummy class, used for testing reloading of classes in 
  * TestJDK.
  */
public class Dummy
{
  public static int i = 0;

  static {
    // System.out.println("initialising dummy class, i = " + i++);
  }

} // class Dummy

