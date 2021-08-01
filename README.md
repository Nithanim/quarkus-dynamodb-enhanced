This is a quarkus extension that attempts to make the dynamodb enhanced library compatible with native image (and quarkus).

If you do not have quarkus, you can just strip all the quarkus stuff out.
Pretty much just use the classes in the "runtime" module.

This extension specifically fixes the following problems:
* `ClassDefNotFoundError` when running tests (with quarkus)
* GraalVM Native-image runtime/reflection issues

The fix for quarkus-tests uses the same workaround as for the native-image.
There is no other way (from my point of view) for native-image (except full support for dynamically generated lambdas).
But for jvm mode it has a slight performance-hit in contrast to the lambdas.

A detection is in place such that the fix for tests is only applied when in "test"-profile.
For normal builds (like prod) this fix is not applied so you keep the full `LambdaMetafactory` performance.

Everything should just work out-of-the box ❤️

Note: The workaround for tests can be removed when https://github.com/aws/aws-sdk-java-v2/issues/2604 is fixed.

## What does it do technically

In short, the AWS SDK tries to create and load lambdas at runtime but since the native-image is pre-compiled, this is not possible.

Instead, the predecessor, MethodHandles are used instead which are fully supported.

There is also an additional bugfix for quarkus in there that deals with multiple classloaders when running tests. Although this is a separate issue, the same fix described above conveniently fixed this too.

As a bonus, since the `DynamoDbBean`s are accessed by reflection, they are automatically registered for this purpose in the GraalVM native-image generation.
This means that you do not need to add `@RegisterForReflection` to all your beans.

If want to know even more about the technical side, feel free to dive into the code.
You can find extensive information in the javadoc (and of course the code itself)!

## Can I use it in production?

Probably. You certainly have to decide for yourself if you want to take the risk. See the disclaimer in the license!

I just researched and spent weeks trying to fix all problems because I needed it for work. (However, there is no association! I did that in my free time!)

We deployed it in production and has been running happily since!

## Usage

### pom.xml

To use this extension, just import it as a normal dependency:
```xml
<dependency>
    <groupId>me.nithanim.quarkus</groupId>
    <artifactId>quarkus-amazon-dynamodb-enhanced</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

NOTE/WARNING:
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

### Config

Currently, there is only a single config property for `application.properties:
```properties
quarkus.dynamodb-enhanced.jvm-transformation=true
```
It controls whether the fix for the ClassLoader problems for tests should be applied.
It is there just in case that there are any problems with the detection of the current build environment.
IF there are problems you can set it specifically for prod to `false` to not give up any speed at runtime.
