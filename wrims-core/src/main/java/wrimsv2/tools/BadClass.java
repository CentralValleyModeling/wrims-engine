package wrimsv2.tools;

import java.math.BigDecimal;

public final class BadClass {
    private static final String adminPassword = "admin_pass123";
    private static final String adminURL = "127.0.0.1:8080";

    public static void main(final String[] args) {
        BadClass badClass = new BadClass(10);
        System.out.println("Value: " + badClass.value);
        System.out.println("Password: " + badClass.adminPassword);
        System.out.println("URL: " + badClass.adminURL);
    }

    protected int value = 25;

    public BadClass(int value) {
        this.value = new BigDecimal(Double.valueOf(value * 1.0)).intValue();

        ItemInterface item = new Item();
        item.doSomething();

        int i = item.throwNPE();
        for (int j = 0; j < i; j++) {
            System.out.println("j: " + j);
        }
    }

    public void throwError() {
        throw new RuntimeException("This is a bad class");
    }

    private interface ItemInterface {
        void doSomething();

        int throwNPE();
    }

    public class Item implements ItemInterface {
        public void doSomething() {
            System.out.println("Doing something");

            try {
                throwError();
                throwNPE();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public int throwNPE() {
            Integer x = null;

            return x.intValue();
        }
    }
}
