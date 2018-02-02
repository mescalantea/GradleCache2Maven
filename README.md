# Gradle Cache to Maven
A command line tool developed in Java to copy the existing content in the Gradle cache to a Maven repository.

# Usage
Run the program by passing the following optional arguments as necessary:

* `-gradle`
Path to the .gradle directory. If it's omitted, the path "%USERPROFILE%\.gradle" will be used by default.

* `-repo`
Path to the Maven repository. If it's omitted, the path "%USERPROFILE%\.m2\repository" will be used by default.

## Example
`java -jar GradleCache2Maven.jar -gradle C:\.gradle -repo C:\repository`
