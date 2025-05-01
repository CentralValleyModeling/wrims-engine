package wrimsv2.tools;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DiffObjects {

	public static boolean compareObjects(Object o1, Object o2) {        
        try {
            List<Field> fields1 = getFields(o1);
            List<Field> fields2 = getFields(o2);
            boolean found;
            Field field2Temp = null;
            for (Field field1 : fields1) {
                found = false;
                for (Field field2 : fields2) {
                    if (field1.getName().equals(field2.getName())) {
                    	if (field1.get(o1)==null && field2.get(o2)==null){
                    		
                    	}else if (field1.get(o1)==null && field2.get(o2)!=null){
                    		System.out.println("Value of sds1 for field " + field1 + " is null but the value " + field2.get(o2) + " for field " + field2);
                            return false;
                    	}else if (field1.get(o1)==null && field2.get(o2)!=null){
                    		System.out.println("Value " + field1.get(o1) + " for field " + field1 + " but value of sds2 for field " + field2+" is null");
                            return false;
                    	}else{
                    		if (!field1.get(o1).equals(field2.get(o2))) {
                    			System.out.println("Value " + field1.get(o1) + " for field " + field1 + " does not match the value " + field2.get(o2) + " for field " + field2);
                    			return false;
                    		}
                    	}
                        field2Temp = field2;
                        found = true;
                    }
                }
                if (!found) {
                    System.out.println("Field " + field1 + " has not been found in the object " + o2.getClass());
                    return false;
                } else {
                    fields2.remove(field2Temp);
                }
            }
            if (fields2.size() > 0) {
                for (Field field : fields2) {
                    System.out.println("Field " + field + " has not been found in the object " + o1.getClass());
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static List<Field> getFields(Object o) {
        Field[] fields = o.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
        }
        return new ArrayList<>(Arrays.asList(fields));
    }
	
}
