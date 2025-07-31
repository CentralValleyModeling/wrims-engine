import java

System = java.type("java.lang.System")
MyCustomClass = java.type("graalpy.MyCustomClass")  # Adjust

class JavaTest:
    def __str__(self):
        return "JavaTest instance with custom class"

    def instanceTest(self):
        custom_instance = MyCustomClass()
        print(str(self))
        System.out.println("Hello from Java System.out.println!")
        custom_instance.printTest()
        if (MyCustomClass.isAnInteger(3)):
            System.out.println("Provided value is an integer")

        if not MyCustomClass.isAnInteger("Hello"):
            System.out.println("Provided value is not an integer")

        System.out.println("Number from custom class: " + str(custom_instance.getAbsoluteDifference(2, 3)))

    def getText(self):
        return MyCustomClass().getText()

    def isAnInteger(self, value):
        return MyCustomClass.isAnInteger(value)

    def getAbsoluteDifference(self, a, b):
        return MyCustomClass().getAbsoluteDifference(a, b)
