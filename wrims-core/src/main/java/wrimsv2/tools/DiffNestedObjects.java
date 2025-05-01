package wrimsv2.tools;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import de.danielbechler.diff.node.Visit;

public class DiffNestedObjects {

	public static void compareObjects(Object o1, Object o2) {        
		
		  DiffNode diff = ObjectDifferBuilder.buildDefault().compare(o1, o2);
		  
		  if (diff.hasChanges()) {
		      diff.visit(new DiffNode.Visitor() {
		    	  
		    	  @Override
		          public void node(DiffNode node, Visit visit)
		          {
		              if (!node.hasChildren()) { // Only print if the property has no child
		                  final Object oldValue = node.canonicalGet(o1);
		                  final Object newValue = node.canonicalGet(o2);
		 
		                  final String message = node.getPropertyName() + " changed from " +
		                          oldValue + " to " + newValue;
		                  System.out.println(message);
		              }
		          }
		      });
		  } else {
		      System.out.println("No differences");
		  }
    }
	
}
