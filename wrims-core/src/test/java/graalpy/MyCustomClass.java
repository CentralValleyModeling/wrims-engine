package graalpy;

public final class MyCustomClass {

    public void printTest() {
        System.out.println("Hello from MyCustomClass!");
    }

    public static boolean isAnInteger(Object value) {
        return (value instanceof Integer);
    }

    public int getAbsoluteDifference(int x, int y) {
        return Math.abs(x - y);
    }

    public String getText() {
        return "JavaTest instance with custom class";
    }
}
