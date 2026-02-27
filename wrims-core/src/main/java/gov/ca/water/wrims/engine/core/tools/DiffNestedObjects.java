package gov.ca.water.wrims.engine.core.tools;

import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import de.danielbechler.diff.node.Visit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiffNestedObjects {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiffNestedObjects.class);

    public static void compareObjects(Object o1, Object o2) {

        DiffNode diff = ObjectDifferBuilder.buildDefault().compare(o1, o2);

        if (diff.hasChanges()) {
            diff.visit(new DiffNode.Visitor() {

                @Override
                public void node(DiffNode node, Visit visit) {
                    if (!node.hasChildren()) { // Only print if the property has no child
                        final Object oldValue = node.canonicalGet(o1);
                        final Object newValue = node.canonicalGet(o2);

                        final String message = node.getPropertyName() + " changed from " + oldValue + " to " + newValue;
                        LOGGER.atInfo().setMessage(message).log();
                    }
                }
            });
        } else {
            LOGGER.atInfo().setMessage("No differences").log();
        }
    }

}
