/*
 *  SetParameterBeanDefinitionParser.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Ian Roberts, 22/Jan/2008
 *
 *  $Id$
 */

package gate.util.spring.xml;

import gate.util.spring.AddPRResourceCustomiser;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * BeanDefinitionParser for <code>&lt;gate:add-pr&gt;</code>,
 * producing a definition of a {@link AddPRResourceCustomiser}
 * object.
 */
public class AddPRBeanDefinitionParser
                                             extends
                                               AbstractSingleBeanDefinitionParser {

  @Override
  protected void doParse(Element element, ParserContext parserContext,
          BeanDefinitionBuilder builder) {
    if(element.hasAttribute("add-before")) {
      builder.addPropertyValue("addBefore", element.getAttribute("add-before"));
    }
    if(element.hasAttribute("add-after")) {
      builder.addPropertyValue("addAfter", element.getAttribute("add-after"));
    }
    if(element.hasAttribute("index")) {
      builder.addPropertyValue("index", element.getAttribute("index"));
    }
    
    builder.addPropertyValue("pr", parserContext.getDelegate()
            .parsePropertyValue(element, builder.getRawBeanDefinition(),
                    "pr"));
  }

  @Override
  protected Class<?> getBeanClass(Element element) {
    return AddPRResourceCustomiser.class;
  }

}
