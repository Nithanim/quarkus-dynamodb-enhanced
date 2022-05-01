package test;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class TestRecord {
  String pk;
  String random;

  public TestRecord() {}

  public TestRecord(String pk, String random) {
    this.pk = pk;
    this.random = random;
  }

  @DynamoDbPartitionKey
  public String getPk() {
    return pk;
  }

  public void setPk(String pk) {
    this.pk = pk;
  }

  public String getRandom() {
    return random;
  }

  public void setRandom(String random) {
    this.random = random;
  }
}
