import java

System = java.type("java.lang.System")

class HelloWorld:
    def __init__(self):
        pass

    def __str__(self):
        return "Hello, World!"

    def printTest(self):
        print(str(self))
        System.out.println("Hello from Java System.out.println!")

    def getText(self):
        return str(self)


if __name__ == "__main__":
    HelloWorld().printTest()