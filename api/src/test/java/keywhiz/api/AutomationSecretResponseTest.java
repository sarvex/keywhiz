package keywhiz.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.OffsetDateTime;
import keywhiz.api.model.Group;
import keywhiz.api.model.Secret;
import keywhiz.api.model.VersionGenerator;
import org.junit.Test;

import static keywhiz.api.SecretDeliveryResponse.decodedLength;
import static keywhiz.testing.JsonHelpers.asJson;
import static keywhiz.testing.JsonHelpers.jsonFixture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class AutomationSecretResponseTest {

  private static final ImmutableMap<String, String> metadata =
      ImmutableMap.of("key1", "value1", "key2", "value2");
  private static final OffsetDateTime NOW = OffsetDateTime.now();
  private static final Secret secret = new Secret(0, "name", VersionGenerator.now().toHex(), null,
      "YWJj", NOW, null, NOW, null, metadata, "upload", null);

  @Test
  public void setsLength() {
    AutomationSecretResponse response = AutomationSecretResponse.fromSecret(secret,
        ImmutableList.<Group>of());
    assertThat(response.secretLength()).isEqualTo(decodedLength("YWJj")).isEqualTo(3);
  }

  @Test
  public void hasVersionedName() {
    AutomationSecretResponse response = AutomationSecretResponse.fromSecret(secret,
        ImmutableList.<Group>of());
    assertThat(response.name()).matches("name" + Secret.VERSION_DELIMITER + "[0-9a-f]+");
  }

  @Test
  public void hasMetaData() {
    AutomationSecretResponse response = AutomationSecretResponse.fromSecret(secret,
        ImmutableList.<Group>of());
    assertThat(response.metadata()).contains(entry("key2", "value2"), entry("key1", "value1"));
  }


  @Test
  public void serializesCorrectly() throws Exception {
    String secret = "YXNkZGFz";

    AutomationSecretResponse automationSecretResponse = AutomationSecretResponse.create(
        0,
        "Database_Password",
        secret,
        OffsetDateTime.parse("2011-09-29T15:46:00.232Z"),
        false,
        ImmutableMap.of(),
        ImmutableList.of());
    assertThat(asJson(automationSecretResponse))
        .isEqualTo(jsonFixture("fixtures/automationSecretResponse.json"));

    AutomationSecretResponse automationSecretResponseWithVersion = AutomationSecretResponse.create(
        33,
        "General_Password..0be68f903f8b7d86",
        secret,
        OffsetDateTime.parse("2011-09-29T15:46:00.312Z"),
        true,
        ImmutableMap.of(),
        ImmutableList.of());
    assertThat(asJson(automationSecretResponseWithVersion))
        .isEqualTo(jsonFixture("fixtures/automationSecretResponseWithVersion.json"));

    AutomationSecretResponse automationSecretResponseWithMetadata = AutomationSecretResponse.create(
        66,
        "Nobody_PgPass",
        secret,
        OffsetDateTime.parse("2011-09-29T15:46:00.232Z"),
        false,
        ImmutableMap.of("mode", "0400", "owner", "nobody"),
        ImmutableList.of());
    assertThat(asJson(automationSecretResponseWithMetadata))
        .isEqualTo(jsonFixture("fixtures/automationSecretResponseWithMetadata.json"));
  }
}
