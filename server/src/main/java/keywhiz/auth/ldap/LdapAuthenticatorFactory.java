package keywhiz.auth.ldap;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.service.AutoService;
import io.dropwizard.auth.basic.BasicCredentials;
import io.dropwizard.java8.auth.Authenticator;
import java.io.IOException;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import keywhiz.api.validation.ValidX500Name;
import keywhiz.auth.User;
import keywhiz.auth.UserAuthenticatorFactory;
import keywhiz.service.config.Templates;
import org.hibernate.validator.constraints.NotEmpty;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Configuration parameters for using an LDAP connection. */
@AutoService(UserAuthenticatorFactory.class)
@JsonTypeName("ldap")
@SuppressWarnings("unused")
public class LdapAuthenticatorFactory implements UserAuthenticatorFactory {
  private final Logger logger = LoggerFactory.getLogger(LdapAuthenticatorFactory.class);

  @NotEmpty
  private String server;

  @Min(value = 1) @Max(value = 65535)
  private int port = 636;

  /**
   * LDAP uses X.500 names, so the nomenclature is a bit odd. This is essentially the username to
   * use but should be a fully-qualified X.500 name.
   */
  @ValidX500Name
  private String userDN;

  private String password;

  /**
   * LDAP parameters to lookup authenticated users and their roles.
   */
  @NotNull @Valid
  private LdapLookupConfig lookup;

  public String getServer() {
    return server;
  }

  public int getPort() {
    return port;
  }

  public String getUserDN() {
    return userDN;
  }

  @NotEmpty
  public String getPassword() {
    try {
      return Templates.evaluateTemplate(password);
    } catch (IOException e) {
      throw new RuntimeException("Failure resolving ldap password template", e);
    }
  }

  public LdapLookupConfig getLookup() {
    return lookup;
  }

  @Override public Authenticator<BasicCredentials, User> build(DBI dbi) {
    logger.debug("Creating LDAP authenticator");
    LdapConnectionFactory connectionFactory =
        new LdapConnectionFactory(getServer(), getPort(), getUserDN(), getPassword());
    return new LdapAuthenticator(connectionFactory, getLookup());
  }
}
