package test;

import java.util.Map;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

public class InfrastructureStack extends Stack {
  public InfrastructureStack(final Construct parent, final String id) {
    this(parent, id, null);
  }

  public InfrastructureStack(final Construct parent, final String id, final StackProps props) {
    super(parent, id, props);

    var table =
        new Table(
            this,
            "TestTable",
            TableProps.builder()
                .removalPolicy(RemovalPolicy.DESTROY)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .tableName("QuarkusDynamodbEnhancedTest")
                .partitionKey(Attribute.builder().name("pk").type(AttributeType.STRING).build())
                .build());

    Function functionOne =
        new Function(
            this,
            "TestFunction",
            FunctionProps.builder()
                .runtime(Runtime.PROVIDED)
                .architecture(Architecture.X86_64)
                .code(Code.fromAsset("../software/TestFunction/target/function.zip"))
                .handler("io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest")
                .memorySize(128)
                .timeout(Duration.seconds(60))
                .logRetention(RetentionDays.ONE_WEEK)
                .environment(Map.of("DISABLE_SIGNAL_HANDLERS", "true"))
                .build());

    table.grantReadWriteData(functionOne);
  }
}
