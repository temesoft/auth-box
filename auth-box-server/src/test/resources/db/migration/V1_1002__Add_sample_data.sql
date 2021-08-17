INSERT INTO organization (id, create_time, name, domain_prefix, address, enabled, last_updated)
VALUES
(
    'c1ade6b3-c023-44f4-b3ef-b0f27ba5e6e8',
    '2017-01-31 11:11:11',
    'Test organization',
    'localhost',
    '101 California St. San Francisco, CA 94107',
    TRUE,
    '2017-01-31 11:11:11'
);

INSERT INTO oauth_scope (id, create_time, description, scope, organization_id)
VALUES
(
    '9a2c53d3-98cc-48d9-b01c-e9bef33082fa',
    '2017-01-31 11:11:11',
    'Some scope description, and it is long, and we will be testing the max length that this field can be',
    'some/scope',
    'c1ade6b3-c023-44f4-b3ef-b0f27ba5e6e8'
),
(
    '307d77ad-9e31-4d42-8f69-278c3721750c',
    '2017-01-31 11:11:11',
    'Another scope description',
    'another/scope',
    'c1ade6b3-c023-44f4-b3ef-b0f27ba5e6e8'
);

INSERT INTO oauth_client_scope  (id, create_time, client_id, scope_id)
VALUES
(
    'ea506aab-ced5-4bc9-a675-97f08fdb5cda',
    '2017-01-31 11:11:11',
    '5d94c101-0236-4a4d-b54b-dd8c446c384c',
    '9a2c53d3-98cc-48d9-b01c-e9bef33082fa'
),
(
    '2def3091-e3ee-42e6-a1ad-602204c81c41',
    '2017-01-31 11:11:11',
    '5d94c101-0236-4a4d-b54b-dd8c446c384c',
    '307d77ad-9e31-4d42-8f69-278c3721750c'
);

INSERT INTO oauth_client (id, create_time, description, secret, grant_types_csv, organization_id, enabled, redirect_urls_csv,
    expiration_seconds, refresh_expiration_seconds, token_type, private_key, public_key, last_updated)
VALUES
(
    '5d94c101-0236-4a4d-b54b-dd8c446c384c',
    '2017-01-31 11:11:11',
    'Test Client 1',
    '8f827d865a1ef37f275e27b42cb25d684cccade72086f2da3cc9e34e2f317a9e',
    'client_credentials,password,authorization_code,refresh_token',
    'c1ade6b3-c023-44f4-b3ef-b0f27ba5e6e8',
    TRUE,
    'http://some-domain,http://another-domain',
    3600,
    86400,
    'STANDARD',
    '-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEAz/Rrs07MN32whp9A/pubfwd1XXr+207tx354Xz7uZZ8udW93fK9rCbIJLcLi15YUNrwbbj9KRXhvWEc7mgEPZu+oHE1BtD8IrXuiSYUuRDdRdRgHd9DxOrAs05YvFYrfabQk67XHa7jAnIF5fr1xd6Z992i/hY8b7jdTmaxuUhMOiOZK/wNl6ya/DN588WTQ3gevYziPdfEuj9U44mosUmYQpLgv0eeoAcsz4Tm7Q2pbzqNPVdccO7ArZg52q/1W68oJeHNLQ4myNKeBZdEoTZOIz/RWsu8cwR+7B77ZJMFT42twSJjY2XfxUA8rgbaOcOcnMeAabmgf8d6gXpxh3wIDAQABAoIBAFOjtBmnTL3Y4MIUlXiSL4V1A4B4sr6UTVCajmGIlyvqTS5QiddtWnjI0aZFJyWcqD6ng8Tg0ceFPAtegxta5834AD552D5dx+i2vDwjw8sOEMYuxvoq4ItBRpZHZmKcu2TxXpQrRa0O3vFTpgESwutj3HBBAh7+wzYZrJc3YhvB8eylZ0z55tjsu3xBQssJ2SHLRMa8e5hy1L1YM1idBsTXTfWtdPbX4MR+rIE1024oIcElxlpXGYdJ7+Q6vwyn7NdBkqG7g40/6mV0XqEgf/G1T+ohFgEW8PPEGS/dZBYKF1JibjyFWhRMSofgT1fd0pCbEg2J06+lTgrqKZqVnokCgYEA/oK5ACAP6R5ZWVtf4Rr9He1+rAX7punDq6rgOETQZo6VXygsnQB8NTLglWTWtMDG8RhVH1hDHYMNxI/kjPa5ufuEKDX8t76IjmpcNReG7Mmk8EsnP7wZ7sktkT4euLffEtswFnZSvTXjjJfJ36hoqG1rva9fumdmnfjsFYHz3M0CgYEA0Sv0ILoDo+56ZOCtnVcI9MztS1SZ9NMDat1zQJwy0jAamCDl8phr1L2xVmABRvCzF8ww9YNd8GHpM2jeJ/zQJEE9A49DwpEozqkUyJOQlf2mkynCpeAWbzeKPCnE6pjRxOhSCD2YlittomFICG+Oz0YF3FvM04JAfk8SaY/QeVsCgYAF00AgvJsgns00ul6rbE62zKTFky17WIZd+38+SnTqpADPOMAsp8IwBYYWZUR0xqTHB+OK4B5JIqLCCPkWMN93XkZcFUA/hGDjwZNys6Mm8EaSKWYwk0GUsY4VcPKLD8pPSC0Flpe2NPGMWj0InXYnxLlekhEC1zMGEph0tZC7UQKBgQC2M8JIy5+mKXKF8sxolMrhaqx9BOnCwBhm+xWMgzrb3IJACI1cLjF8mXWZaIz2CT4YdoIWWYlTreqnGnRDnOFaHdW3GGP4DaqeG8SfHT3spwKBgQDA/377NV5T6k1jL6p3c40dXp9kyQkHMHGAlbYI9JfODXbzlFIP00IX8Tn0vKEdEvjROFvE2G2stKbtE+6PQkOyncvw07MMQV3vlSx5wDcwiZSoIFfiG4FO2iIVK00G1NNAIwtBRigLSmUUYkfk9uU8P8g1Wnu8EqmPu+rvzYmhpA==\n-----END RSA PRIVATE KEY-----',
    '-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAz/Rrs07MN32whp9A/pubfwd1XXr+207tx354Xz7uZZ8udW93fK9rCbIJLcLi15YUNrwbbj9KRXhvWEc7mgEPZu+oHE1BtD8IrXuiSYUuRDdRdRgHd9DxOrAs05YvFYrfabQk67XHa7jAnIF5fr1xd6Z992i/hY8b7jdTmaxuUhMOiOZK/wNl6ya/DN588WTQ3gevYziPdfEuj9U44mosUmYQpLgv0eeoAcsz4Tm7Q2pbzqNPVdccO7ArZg52q/1W68oJeHNLQ4myNKeBZdEoTZOIz/RWsu8cwR+7B77ZJMFT42twSJjY2XfxUA8rgbaOcOcnMeAabmgf8d6gXpxh3wIDAQAB\n-----END PUBLIC KEY-----',
    '2017-01-31 11:11:11'
);


INSERT INTO oauth_token (id, create_time, hash, organization_id, client_id, expiration, scopes_csv, oauth_user_id, token_type, ip, user_agent, request_id)
VALUES
(
    'c4870e7d-9a4b-46c1-bcad-56550e0508be',
    '2017-01-31 11:11:11',
    '9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08',
    'c1ade6b3-c023-44f4-b3ef-b0f27ba5e6e8',
    '5d94c101-0236-4a4d-b54b-dd8c446c384c',
    '2017-01-31 12:12:12',
    'some/scope,another/scope',
    '6c580763-c0c1-4f26-92c6-ffeba50dc4d5',
    'ACCESS_TOKEN',
    '1.2.3.4',
    'Some User Agent',
    '371817a9-17bb-4ef6-806b-2c0c71e44462'
),
(
    '5dd46292-b28c-4c05-83b2-641d6c2456cf',
    '2017-01-31 11:11:11',
    '60303ae22b998861bce3b28f33eec1be758a213c86c93c076dbe9f558c11c752',
    'c1ade6b3-c023-44f4-b3ef-b0f27ba5e6e8',
    '5d94c101-0236-4a4d-b54b-dd8c446c384c',
    '2099-01-31 12:12:12',
    'some/scope,another/scope',
    '6c580763-c0c1-4f26-92c6-ffeba50dc4d5',
    'ACCESS_TOKEN',
    '1.2.3.4',
    'Some User Agent',
    'cddca780-6cee-4d7f-89a1-a457bfa5504a'
),
(
    '8aef48b4-f508-4322-b1f4-43db9d605a81',
    '2017-01-31 11:11:11',
    'fd61a03af4f77d870fc21e05e7e80678095c92d808cfb3b5c279ee04c74aca13',
    'c1ade6b3-c023-44f4-b3ef-b0f27ba5e6e8',
    '5d94c101-0236-4a4d-b54b-dd8c446c384c',
    '2017-01-31 12:12:12',
    'some/scope,another/scope',
    '6c580763-c0c1-4f26-92c6-ffeba50dc4d5',
    'REFRESH_TOKEN',
    '1.2.3.4',
    'Some User Agent',
    '08db0f8c-aac8-4ca8-95da-cb0a9a2bb35d'
),
(
    '5c6200b3-bb60-4d0c-b481-67c9a720d2a0',
    '2017-01-31 11:11:11',
    'a4e624d686e03ed2767c0abd85c14426b0b1157d2ce81d27bb4fe4f6f01d688a',
    'c1ade6b3-c023-44f4-b3ef-b0f27ba5e6e8',
    '5d94c101-0236-4a4d-b54b-dd8c446c384c',
    '2099-01-31 12:12:12',
    'some/scope,another/scope',
    '6c580763-c0c1-4f26-92c6-ffeba50dc4d5',
    'REFRESH_TOKEN',
    '1.2.3.4',
    'Some User Agent',
    '4c293e1c-6911-46f5-a6c3-534ce8b20cd1'
);


INSERT INTO oauth_user (id, create_time, username, password, enabled, organization_id, metadata, using_2fa, secret, last_updated)
VALUES
(
    '6c580763-c0c1-4f26-92c6-ffeba50dc4d5',
    '2017-01-31 11:11:11',
    'user1',
    '$2a$10$OqmIe./UUWJrWdCvXmAah.O/xap1wWM3QzbsxLMPCk0ndsSQ9a0OS',
    TRUE,
    'c1ade6b3-c023-44f4-b3ef-b0f27ba5e6e8',
    '{}',
    false,
    'ERN7C3DG3GWBKDF6JXRCQIAF4M24GMQ7NZXL5JF4XPQU45N3R642VTCKHTIRU72W',
    '2017-01-31 11:11:11'
);