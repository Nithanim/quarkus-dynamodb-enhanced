This is a quarkus extension that attempts to make the dynamodb enhanced library compatible with native image (and quarkus).

If you do not have quarkus, you can just strip all the quarkus stuff out. Pretty much just use the classen in the "runtime" module.

To use this extension, make sure you match the quarkus version is the parent pom with your quarkus version.
Import the fix to your project:
```xml
        <dependency>
            <groupId>org.acme</groupId>
            <artifactId>aws-dynamodb-fix</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
```
