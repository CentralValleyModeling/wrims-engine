# buildSrc

The `buildSrc` directory contains a set of template Gradle files that can be pulled in to other builds like plugins.
This allows autoconfiguring many components of the build with a single line in the `build.gradle` file, and avoids 
copying and pasting a lot of boilerplate code. The single location of all of the logic also makes it much easier to
update for the entire library, such as updating the version of Java.