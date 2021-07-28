This is a quarkus extension that attempts to make the dynamodb enhanced library compatible with native image (and quarkus).

If you do not have quarkus, you can just strip all the quarkus stuff out.
Pretty much just use the classes in the "runtime" module.

This extension specifically fixes the following problems:
* `ClassDefNotFoundError` when running tests (with quarkus)
* GraalVM Native-image runtime/reflection issues

Warning: The fix for tests has a slight performance-hit in production when using normal jvm mode. It can be removed when https://github.com/aws/aws-sdk-java-v2/issues/2604 is fixed. (Info: The "fix" is the same as the fix for GraalVM Native-image.) 

## What does it do technically

In short, the AWS SDK tries to create and load lambdas at runtime but since the native-image is pre-compiled, this is not possible.

Instead, the predecessor, MethodHandles are used instead which are fully supported.

There is also an additional bugfix for quarkus in there that deals with multiple classloaders when running tests. Although this is a separate issue, the same fix described above conveniently fixed this too.

As a bonus, since the `DynamoDbBean`s are accessed by reflection, they are automatically registered for this purpose in the GraalVM native-image generation.
This means that you do not need to add `@RegisterForReflection` to all your beans.

Everything should work out-of-the box ❤️

If want to know even more about the technical side, feel free to dive into the code.
You can find extensive information in the javadoc (and of course the code itself)!

## Usage

To use this extension, make sure you match the quarkus version is the parent pom with your quarkus version.
Import the fix to your project:
```xml
<dependency>
    <groupId>me.nithanim.quarkus</groupId>
    <artifactId>quarkus-amazon-dynamodb-enhanced</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```
WARNING:
This extension is built against a specific quarkus version (see pom.xml properties).
Since you are probably using the quarkus version bom in your project like so:
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>${quarkus.platform.group-id}</groupId>
            <artifactId>${quarkus.platform.artifact-id}</artifactId>
            <version>${quarkus.platform.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```
the versions defined there take precedence over the ones defined in this extension.

In any case, you can always exclude the dependencies and add them to your project directly (like every other quarkus dependency you have added).
It might look something like this:
```xml
<dependencies>
    <dependency>
        <groupId>me.nithanim.quarkus</groupId>
        <artifactId>quarkus-amazon-dynamodb-enhanced</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <exclusions>
            <exclusion>
                <groupId>io.quarkus</groupId>
                <artifactId>quarkus-arc</artifactId>
            </exclusion>
            <exclusion>
                <groupId>io.quarkus</groupId>
                <artifactId>quarkus-amazon-dynamodb</artifactId>
            </exclusion>
            <exclusion>
                <groupId>software.amazon.awssdk</groupId>
                <artifactId>dynamodb-enhanced</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-amazon-dynamodb</artifactId>
    </dependency>
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>dynamodb-enhanced</artifactId>
    </dependency>
</dependencies>
```
Then maven will pull the versions that are defined via your quarkus import.
