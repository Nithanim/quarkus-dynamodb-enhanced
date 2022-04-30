In this folder is a simple cdk cloudformation template to test the native compilation.

The quarkus extension has to be built first (`mvn install` in the root folder) and
the `TestFunction` ([software/TestFunction](software/TestFunction)) in here has to be built natively too (`package -Pnative`).

Then you can deploy the stack with like `AWS_REGION=eu-central-1 cdk deploy` from the [infrastructure](infrastructure) folder. 
