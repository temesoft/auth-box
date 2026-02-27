CREATE TABLE IF NOT EXISTS users (
  id          VARCHAR(36)   NOT NULL,
  create_time DATETIME      NOT NULL,
  username    VARCHAR(255)  NOT NULL,
  password    VARCHAR(255)  NOT NULL,
  name        VARCHAR(128)  NOT NULL,
  roles_csv   VARCHAR(36)   NOT NULL,
  enabled     TINYINT(1)    NOT NULL DEFAULT FALSE,
  organization_id  VARCHAR(36)   NOT NULL,
  last_updated DATETIME      NOT NULL,
  PRIMARY KEY (id)
);
ALTER TABLE users ADD INDEX idx_users_username (username);
ALTER TABLE users ADD INDEX idx_users_organization_id (organization_id);
ALTER TABLE users ADD INDEX idx_users_create_time (create_time);


CREATE TABLE IF NOT EXISTS organization (
  id            VARCHAR(36)   NOT NULL,
  create_time   DATETIME      NOT NULL,
  name          VARCHAR(255)  NOT NULL,
  domain_prefix VARCHAR(255)  NOT NULL,
  address       VARCHAR(255)  NOT NULL,
  enabled       TINYINT(1)    NOT NULL DEFAULT FALSE,
  last_updated DATETIME      NOT NULL,
  PRIMARY KEY (id)
);
ALTER TABLE organization ADD INDEX idx_organization_domain_prefix (domain_prefix);


CREATE TABLE IF NOT EXISTS oauth_client (
  id                VARCHAR(36)   NOT NULL,
  create_time       DATETIME      NOT NULL,
  description       TEXT          NOT NULL,
  secret            VARCHAR(255)  NOT NULL,
  grant_types_csv   VARCHAR(255)  NOT NULL,
  organization_id        VARCHAR(36)   NOT NULL,
  enabled           TINYINT(1)    NOT NULL DEFAULT FALSE,
  redirect_urls_csv TEXT,
  expiration_seconds INTEGER      NOT NULL,
  refresh_expiration_seconds INTEGER NOT NULL,
  token_type        VARCHAR(36)   NOT NULL,
  private_key       TEXT NOT NULL,
  public_key        TEXT NOT NULL,
  last_updated      DATETIME NOT NULL,
  PRIMARY KEY (id)
);
ALTER TABLE oauth_client ADD INDEX idx_oauth_client_organization_id (organization_id);

CREATE TABLE IF NOT EXISTS oauth_scope (
  id                VARCHAR(36)   NOT NULL,
  create_time       DATETIME      NOT NULL,
  description       TEXT          NOT NULL,
  scope             VARCHAR(255)  NOT NULL,
  organization_id        VARCHAR(36)   NOT NULL,
  PRIMARY KEY (id)
);
ALTER TABLE oauth_scope ADD INDEX idx_oauth_scope_organization_id (organization_id);
ALTER TABLE oauth_scope ADD INDEX idx_oauth_scope_creator_id (scope);

CREATE TABLE IF NOT EXISTS oauth_client_scope (
  id                VARCHAR(36)   NOT NULL,
  create_time       DATETIME      NOT NULL,
  client_id         VARCHAR(255)  NOT NULL,
  scope_id          VARCHAR(255)  NOT NULL,
  PRIMARY KEY (id)
);
ALTER TABLE oauth_client_scope ADD INDEX idx_oauth_client_scope_client_id (client_id);
ALTER TABLE oauth_client_scope ADD INDEX idx_oauth_client_scope_scope_id (scope_id);


CREATE TABLE IF NOT EXISTS oauth_token (
  id                VARCHAR(36)   NOT NULL,
  create_time       DATETIME      NOT NULL,
  hash              VARCHAR(64)   NOT NULL,
  organization_id   VARCHAR(36)   NOT NULL,
  client_id         VARCHAR(36)   NOT NULL,
  expiration        DATETIME      NOT NULL,
  scopes_csv        TEXT          NOT NULL,
  oauth_user_id     VARCHAR(36),
  token_type        VARCHAR(24)   NOT NULL,
  ip                VARCHAR(15)   NOT NULL,
  user_agent        TEXT          NOT NULL,
  request_id        VARCHAR(36)   NOT NULL,
  linked_token_id   VARCHAR(36),
  PRIMARY KEY (id)
);
ALTER TABLE oauth_token ADD INDEX idx_oauth_token_hash (hash);
ALTER TABLE oauth_token ADD INDEX idx_oauth_token_organization_id (organization_id);
ALTER TABLE oauth_token ADD INDEX idx_oauth_token_client_id (client_id);
ALTER TABLE oauth_token ADD INDEX idx_oauth_token_token_type (token_type);
ALTER TABLE oauth_token ADD INDEX idx_oauth_token_request_id (request_id);


CREATE TABLE IF NOT EXISTS oauth_user (
  id          VARCHAR(36)   NOT NULL,
  create_time DATETIME      NOT NULL,
  username    VARCHAR(255)  NOT NULL,
  password    VARCHAR(255)  NOT NULL,
  enabled     TINYINT(1)    NOT NULL DEFAULT FALSE,
  organization_id  VARCHAR(36)   NOT NULL,
  metadata    TEXT          NOT NULL,
  using_2fa   TINYINT(1)    NOT NULL DEFAULT FALSE,
  secret      VARCHAR(64)   NOT NULL,
  last_updated DATETIME      NOT NULL,
  PRIMARY KEY (id)
);
ALTER TABLE oauth_user ADD INDEX idx_oauth_user_username (username);
ALTER TABLE oauth_user ADD INDEX idx_oauth_user_organization_id (organization_id);
ALTER TABLE oauth_user ADD INDEX idx_oauth_user_create_time (create_time);



CREATE TABLE IF NOT EXISTS access_log (
    id                  VARCHAR(36)     NOT NULL, -- UUID type 1
    create_time         DATETIME        NOT NULL,
    organization_id     VARCHAR(36),
    oauth_token_id      VARCHAR(36),
    client_id           VARCHAR(36),
    request_id          VARCHAR(36),
    source              VARCHAR(255)    NOT NULL,
    duration_ms         INTEGER,
    message             TEXT NOT NULL,
    error               VARCHAR(255),
    status_code         INTEGER         NOT NULL,
    ip                  VARCHAR(15),
    user_agent          TEXT,
    PRIMARY KEY (id)
);

ALTER TABLE access_log ADD INDEX idx_access_log_organization_id (organization_id);
ALTER TABLE access_log ADD INDEX idx_access_log_oauth_token_id (oauth_token_id);
ALTER TABLE access_log ADD INDEX idx_access_log_client_id (client_id);
ALTER TABLE access_log ADD INDEX idx_access_log_request_id (request_id);
ALTER TABLE access_log ADD INDEX idx_access_log_source (source);
ALTER TABLE access_log ADD INDEX idx_access_log_status_code (status_code);