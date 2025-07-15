import java

System = java.type("java.lang.System")
MyCustomClass = java.type("graalpy.MyCustomClass")  # Adjust

class JavaTest:
    def __init__(self):
        self.custom_class = MyCustomClass()

    def __str__(self):
        return "JavaTest instance with custom class"

    def main(self):
        print(str(self))
        System.out.println("Hello from Java System.out.println!")
        self.custom_class.main()
        if (MyCustomClass.isTrue()):
            System.out.println("Custom class is true")

        System.out.println("Number from custom class: " + str(self.custom_class.getNumber(2, 3)))

    def getText(self):
        return str(self)

    def isTrue(self):
        return MyCustomClass.isTrue()

    def getNumber(self, a, b):
        return self.custom_class.getNumber(a, b)
