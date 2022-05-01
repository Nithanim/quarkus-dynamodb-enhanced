package test;

import java.util.UUID;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class TestLambda implements RequestHandler<String, String> {

  private final DynamoDbTable<TestRecord> table;

  @Inject
  public TestLambda(DynamoDbClient basicClient) {
    DynamoDbEnhancedClient client =
        DynamoDbEnhancedClient.builder().dynamoDbClient(basicClient).build();
    TableSchema<TestRecord> tableSchema = TableSchema.fromBean(TestRecord.class);
    table = client.table("QuarkusDynamodbEnhancedTest", tableSchema);
  }

  @Override
  public String handleRequest(String input, Context context) {
    String uuid = UUID.randomUUID().toString();
    System.out.println("UUID: " + uuid);

    TestRecord original = new TestRecord(uuid, input);

    table.putItem(original);
    TestRecord item = table.getItem(Key.builder().partitionValue(uuid).build());

    Assertions.assertThat(item).usingRecursiveComparison().isEqualTo(original);

    return "Id: " + uuid;
  }
}
