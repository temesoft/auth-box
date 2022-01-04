INSERT INTO organization (id, create_time, name, domain_prefix, address, enabled, last_updated)
VALUES
(
    '65eb8936-b6a3-4af9-8391-059dc22982c2',
    '2021-03-16 11:11:11',
    'Test organization 1',
    '${defaultDomainPrefix}',
    '101 California St. San Francisco, CA 94107',
    TRUE,
    '2021-03-16 11:11:11'
);

INSERT INTO users (id, create_time, username, password, name, roles_csv, enabled, organization_id, last_updated)
VALUES
(
    '8841e4ca-fc72-45f9-8af1-528e87b0b3f0',
    '2021-03-16 11:11:11',
    'admin',
    '$2a$10$BUN/uL0ZVcvTclADYjtpMe1God6nzYnm3t7iISfP/i5yP79.sDQLO',
    'Admin',
    'ROLE_ADMIN',
    TRUE,
    '65eb8936-b6a3-4af9-8391-059dc22982c2',
    '2021-03-16 11:11:11'
);

INSERT INTO oauth_scope (id, create_time, description, scope, organization_id)
VALUES
(
    '9a2c53d3-98cc-48d9-b01c-e9bef33082fa',
    '2021-03-16 11:11:11',
    'Some scope functionality description',
    'some/scope',
    '65eb8936-b6a3-4af9-8391-059dc22982c2'
),
(
    '307d77ad-9e31-4d42-8f69-278c3721750c',
    '2021-03-16 11:11:11',
    'Another scope functionality description',
    'another/scope',
    '65eb8936-b6a3-4af9-8391-059dc22982c2'
),
(
    '1270ee1d-cacd-4fe7-9690-8560854f0b5e',
    '2021-03-16 11:11:11',
    'Organization read access functionality',
    'organization/read',
    '65eb8936-b6a3-4af9-8391-059dc22982c2'
),
(
    '02e5656d-9723-4c0a-9898-93e77b2359ea',
    '2021-03-16 11:11:11',
    'Organization write access functionality',
    'organization/write',
    '65eb8936-b6a3-4af9-8391-059dc22982c2'
),
(
    '61e5b343-2a8a-4d1c-8890-db754bd30168',
    '2021-03-16 11:11:11',
    'Oauth2 user read access functionality',
    'oauth2-user/read',
    '65eb8936-b6a3-4af9-8391-059dc22982c2'
),
(
    '248605ee-0c52-4e8f-8def-b32e5fa44bf7',
    '2021-03-16 11:11:11',
    'Oauth2 user write access functionality',
    'oauth2-user/write',
    '65eb8936-b6a3-4af9-8391-059dc22982c2'
),
(
    'aadaf0f7-ba07-4ba2-bf3d-737ba6c71570',
    '2021-03-16 11:11:11',
    'Oauth2 token read access functionality',
    'oauth2-token/read',
    '65eb8936-b6a3-4af9-8391-059dc22982c2'
),
(
    '40c732c3-adeb-4009-a260-1007d5a91c87',
    '2021-03-16 11:11:11',
    'Oauth2 token write access functionality',
    'oauth2-token/write',
    '65eb8936-b6a3-4af9-8391-059dc22982c2'
),
(
    '7099e5fa-9fe6-4dc6-8523-f844943d1d6d',
    '2021-03-16 11:11:11',
    'Oauth2 scope read access functionality',
    'oauth2-scope/read',
    '65eb8936-b6a3-4af9-8391-059dc22982c2'
),
(
    '52f361d8-30af-4b44-9406-5c073f728822',
    '2021-03-16 11:11:11',
    'Oauth2 scope write access functionality',
    'oauth2-scope/write',
    '65eb8936-b6a3-4af9-8391-059dc22982c2'
),
(
    'e7ce6c34-0ef8-44b0-b0e7-7505079dbe32',
    '2021-03-16 11:11:11',
    'Oauth2 client read access functionality',
    'oauth2-client/read',
    '65eb8936-b6a3-4af9-8391-059dc22982c2'
),
(
    'c99d1cd1-e29c-4cee-a31c-3b1e99c1c093',
    '2021-03-16 11:11:11',
    'Oauth2 client write access functionality',
    'oauth2-client/write',
    '65eb8936-b6a3-4af9-8391-059dc22982c2'
);

INSERT INTO oauth_client_scope  (id, create_time, client_id, scope_id)
VALUES
(
    'ea506aab-ced5-4bc9-a675-97f08fdb5cda',
    '2021-03-16 11:11:11',
    '5d94c101-0236-4a4d-b54b-dd8c446c384c',
    '9a2c53d3-98cc-48d9-b01c-e9bef33082fa'
),
(
    'ea89e0fa-5fc0-451b-a6fb-28af475ab036',
    '2021-03-16 11:11:11',
    '5d94c101-0236-4a4d-b54b-dd8c446c384c',
    '307d77ad-9e31-4d42-8f69-278c3721750c'
),
(
    'a0554f93-6ed9-4feb-adf5-ea9c4a7fb8d1',
    '2021-03-16 11:11:11',
    '8c65aa63-bf75-4a68-8b9a-52304623cad1',
    '9a2c53d3-98cc-48d9-b01c-e9bef33082fa'
),
(
    'd09ba13b-a114-4d7b-bcfc-f173c59572ae',
    '2021-03-16 11:11:11',
    '8c65aa63-bf75-4a68-8b9a-52304623cad1',
    '307d77ad-9e31-4d42-8f69-278c3721750c'
),


(
    '6066f466-1235-4331-84bc-aa7b6d909d8d',
    '2021-03-16 11:11:11',
    '5d94c101-0236-4a4d-b54b-dd8c446c384c',
    '1270ee1d-cacd-4fe7-9690-8560854f0b5e'
),
(
    'd2d98598-9c87-4efb-bca6-528e65f3726c',
    '2021-03-16 11:11:11',
    '5d94c101-0236-4a4d-b54b-dd8c446c384c',
    '02e5656d-9723-4c0a-9898-93e77b2359ea'
),
(
    'bdd45ae8-fcfb-4b58-9e62-c27cdd169578',
    '2021-03-16 11:11:11',
    '5d94c101-0236-4a4d-b54b-dd8c446c384c',
    '61e5b343-2a8a-4d1c-8890-db754bd30168'
),
(
    '95105f0c-fddb-4085-8df3-42fc98904aea',
    '2021-03-16 11:11:11',
    '5d94c101-0236-4a4d-b54b-dd8c446c384c',
    '248605ee-0c52-4e8f-8def-b32e5fa44bf7'
),
(
    'f1ed7652-2797-4453-bb56-28110632ea50',
    '2021-03-16 11:11:11',
    '5d94c101-0236-4a4d-b54b-dd8c446c384c',
    'aadaf0f7-ba07-4ba2-bf3d-737ba6c71570'
),
(
    'f50381e2-c4bb-47a7-93c1-7ac5c4782ae9',
    '2021-03-16 11:11:11',
    '5d94c101-0236-4a4d-b54b-dd8c446c384c',
    '40c732c3-adeb-4009-a260-1007d5a91c87'
),
(
    '42ffea04-7b53-4695-971a-137744385648',
    '2021-03-16 11:11:11',
    '5d94c101-0236-4a4d-b54b-dd8c446c384c',
    '7099e5fa-9fe6-4dc6-8523-f844943d1d6d'
),
(
    'a7f014be-5e4d-4173-9c92-c35850754533',
    '2021-03-16 11:11:11',
    '5d94c101-0236-4a4d-b54b-dd8c446c384c',
    '52f361d8-30af-4b44-9406-5c073f728822'
),
(
    '98dfe44b-5188-428f-bf1e-ad026cf2a829',
    '2021-03-16 11:11:11',
    '5d94c101-0236-4a4d-b54b-dd8c446c384c',
    'e7ce6c34-0ef8-44b0-b0e7-7505079dbe32'
),
(
    'f83974c4-ff53-4b3e-96c3-2a845d97055a',
    '2021-03-16 11:11:11',
    '5d94c101-0236-4a4d-b54b-dd8c446c384c',
    'c99d1cd1-e29c-4cee-a31c-3b1e99c1c093'
),




(
    'adaceed5-887d-4ee8-a6fb-3c3454b23bba',
    '2021-03-16 11:11:11',
    '8c65aa63-bf75-4a68-8b9a-52304623cad1',
    '1270ee1d-cacd-4fe7-9690-8560854f0b5e'
),
(
    '2674c35a-3966-4d70-a4d6-1b3abb3dcc7f',
    '2021-03-16 11:11:11',
    '8c65aa63-bf75-4a68-8b9a-52304623cad1',
    '02e5656d-9723-4c0a-9898-93e77b2359ea'
),
(
    'd2ba9980-efba-416c-9015-c8589535f831',
    '2021-03-16 11:11:11',
    '8c65aa63-bf75-4a68-8b9a-52304623cad1',
    '61e5b343-2a8a-4d1c-8890-db754bd30168'
),
(
    '76ef28b8-c2c8-46d3-b86d-f68518065d20',
    '2021-03-16 11:11:11',
    '8c65aa63-bf75-4a68-8b9a-52304623cad1',
    '248605ee-0c52-4e8f-8def-b32e5fa44bf7'
),
(
    '91120010-9bef-4089-9445-c96add279024',
    '2021-03-16 11:11:11',
    '8c65aa63-bf75-4a68-8b9a-52304623cad1',
    'aadaf0f7-ba07-4ba2-bf3d-737ba6c71570'
),
(
    '13988c0d-708e-49cd-9884-87124a8cc2f4',
    '2021-03-16 11:11:11',
    '8c65aa63-bf75-4a68-8b9a-52304623cad1',
    '40c732c3-adeb-4009-a260-1007d5a91c87'
),
(
    '4ba0a2ce-afc5-4aad-b800-9b125f017abd',
    '2021-03-16 11:11:11',
    '8c65aa63-bf75-4a68-8b9a-52304623cad1',
    '7099e5fa-9fe6-4dc6-8523-f844943d1d6d'
),
(
    '018c88ce-5be4-41ad-9a7a-b0999f38f149',
    '2021-03-16 11:11:11',
    '8c65aa63-bf75-4a68-8b9a-52304623cad1',
    '52f361d8-30af-4b44-9406-5c073f728822'
),
(
    '9dfa9490-c2b1-4d62-a89e-eb80226ac0e5',
    '2021-03-16 11:11:11',
    '8c65aa63-bf75-4a68-8b9a-52304623cad1',
    'e7ce6c34-0ef8-44b0-b0e7-7505079dbe32'
),
(
    '82af458f-d080-415b-9ded-9b604a7774d3',
    '2021-03-16 11:11:11',
    '8c65aa63-bf75-4a68-8b9a-52304623cad1',
    'c99d1cd1-e29c-4cee-a31c-3b1e99c1c093'
);

INSERT INTO oauth_client (id, create_time, description, secret, grant_types_csv, organization_id, enabled, redirect_urls_csv, expiration_seconds, refresh_expiration_seconds, token_type, private_key, public_key, last_updated)
VALUES
(
    '5d94c101-0236-4a4d-b54b-dd8c446c384c',
    '2021-03-16 11:11:11',
    'Test service client',
    '8f827d865a1ef37f275e27b42cb25d684cccade72086f2da3cc9e34e2f317a9e',
    'client_credentials',
    '65eb8936-b6a3-4af9-8391-059dc22982c2',
    TRUE,
    '',
    3600,
    86400,
    'STANDARD',
    '-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEAz/Rrs07MN32whp9A/pubfwd1XXr+207tx354Xz7uZZ8udW93\nfK9rCbIJLcLi15YUNrwbbj9KRXhvWEc7mgEPZu+oHE1BtD8IrXuiSYUuRDdRdRgH\nd9DxOrAs05YvFYrfabQk67XHa7jAnIF5fr1xd6Z992i/hY8b7jdTmaxuUhMOiOZK\n/wNl6ya/DN588WTQ3gevYziPdfEuj9U44mosUmYQpLgv0eeoAcsz4Tm7Q2pbzqNP\nVdccO7ArZg52q/1W68oJeHNLQ4myNKeBZdEoTZOIz/RWsu8cwR+7B77ZJMFT42tw\nSJjY2XfxUA8rgbaOcOcnMeAabmgf8d6gXpxh3wIDAQABAoIBAFOjtBmnTL3Y4MIU\nlXiSL4V1A4B4sr6UTVCajmGIlyvqTS5QiddtWnjI0aZFJyWcqD6ng8Tg0ceFPAte\ngxta5834AD552D5dx+i2vDwjw8sOEMYuxvoq4ItBRpZHZmKcu2TxXpQrRa0O3vFT\npgESwutj3HBBAh7+wzYZrJc3YhvB8eylZ0z55tjsu3xBQssJ2SHLRMa8e5hy1L1Y\nM1idBsTXTfWtdPbX4MR+rIE1024oIcElxlpXGYdJ7+Q6vwyn7NdBkqG7g40/6mV0\nXqEgf/G1T+ohFgEW8PPEGS/dZBYKF1JibjyFWhRMSofgT1fd0pCbEg2J06+lTgrq\nKZqVnokCgYEA/oK5ACAP6R5ZWVtf4Rr9He1+rAX7punDq6rgOETQZo6VXygsnQB8\nNTLglWTWtMDG8RhVH1hDHYMNxI/kjPa5ufuEKDX8t76IjmpcNReG7Mmk8EsnP7wZ\n7sktkT4euLffEtswFnZSvTXjjJfJ36hoqG1rva9fumdmnfjsFYHz3M0CgYEA0Sv0\nILoDo+56ZOCtnVcI9MztS1SZ9NMDat1zQJwy0jAamCDl8phr1L2xVmABRvCzF8ww\n9YNd8GHpM2jeJ/zQJEE9A49DwpEozqkUyJOQlf2mkynCpeAWbzeKPCnE6pjRxOhS\nCD2YlittomFICG+Oz0YF3FvM04JAfk8SaY/QeVsCgYAF00AgvJsgns00ul6rbE62\nzKTFky17WIZd+38+SnTqpADPOMAsp8IwBYYWZUR0xqTHB+OK4B5JIqLCCPkWMN93\nXkZcFUA/hGDjwZNys6Mm8EaSKWYwk0GUsY4VcPKLD8pPSC0Flpe2NPGMWj0InXYn\nxLlekhEC1zMGEph0tZC7UQKBgQC2M8JIy5+mKXKF8sxolMrhaqx9BOnCwBhm+xWM\nv0LpoitYPrfutOShAMe7xI74O2zVmvE+uzqVzMP1GzEAxhCMtCMwuZe9oPJZ6iCH\ngzrb3IJACI1cLjF8mXWZaIz2CT4YdoIWWYlTreqnGnRDnOFaHdW3GGP4DaqeG8Sf\nHT3spwKBgQDA/377NV5T6k1jL6p3c40dXp9kyQkHMHGAlbYI9JfODXbzlFIP00IX\n8Tn0vKEdEvjROFvE2G2stKbtE+6PQkOyncvw07MMQV3vlSx5wDcwiZSoIFfiG4FO\n2iIVK00G1NNAIwtBRigLSmUUYkfk9uU8P8g1Wnu8EqmPu+rvzYmhpA==\n-----END RSA PRIVATE KEY-----',
    '-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAz/Rrs07MN32whp9A/pub\nfwd1XXr+207tx354Xz7uZZ8udW93fK9rCbIJLcLi15YUNrwbbj9KRXhvWEc7mgEP\nZu+oHE1BtD8IrXuiSYUuRDdRdRgHd9DxOrAs05YvFYrfabQk67XHa7jAnIF5fr1x\nd6Z992i/hY8b7jdTmaxuUhMOiOZK/wNl6ya/DN588WTQ3gevYziPdfEuj9U44mos\nUmYQpLgv0eeoAcsz4Tm7Q2pbzqNPVdccO7ArZg52q/1W68oJeHNLQ4myNKeBZdEo\nTZOIz/RWsu8cwR+7B77ZJMFT42twSJjY2XfxUA8rgbaOcOcnMeAabmgf8d6gXpxh\n3wIDAQAB\n-----END PUBLIC KEY-----',
    '2021-03-16 11:11:11'
),
(
    '8c65aa63-bf75-4a68-8b9a-52304623cad1',
    '2021-03-16 11:11:11',
    'Test web app client',
    '8f827d865a1ef37f275e27b42cb25d684cccade72086f2da3cc9e34e2f317a9e',
    'authorization_code,password,refresh_token',
    '65eb8936-b6a3-4af9-8391-059dc22982c2',
    TRUE,
    'http://some-domain,http://another-domain',
    3600,
    86400,
    'JWT',
    '-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEAz/Rrs07MN32whp9A/pubfwd1XXr+207tx354Xz7uZZ8udW93\nfK9rCbIJLcLi15YUNrwbbj9KRXhvWEc7mgEPZu+oHE1BtD8IrXuiSYUuRDdRdRgH\nd9DxOrAs05YvFYrfabQk67XHa7jAnIF5fr1xd6Z992i/hY8b7jdTmaxuUhMOiOZK\n/wNl6ya/DN588WTQ3gevYziPdfEuj9U44mosUmYQpLgv0eeoAcsz4Tm7Q2pbzqNP\nVdccO7ArZg52q/1W68oJeHNLQ4myNKeBZdEoTZOIz/RWsu8cwR+7B77ZJMFT42tw\nSJjY2XfxUA8rgbaOcOcnMeAabmgf8d6gXpxh3wIDAQABAoIBAFOjtBmnTL3Y4MIU\nlXiSL4V1A4B4sr6UTVCajmGIlyvqTS5QiddtWnjI0aZFJyWcqD6ng8Tg0ceFPAte\ngxta5834AD552D5dx+i2vDwjw8sOEMYuxvoq4ItBRpZHZmKcu2TxXpQrRa0O3vFT\npgESwutj3HBBAh7+wzYZrJc3YhvB8eylZ0z55tjsu3xBQssJ2SHLRMa8e5hy1L1Y\nM1idBsTXTfWtdPbX4MR+rIE1024oIcElxlpXGYdJ7+Q6vwyn7NdBkqG7g40/6mV0\nXqEgf/G1T+ohFgEW8PPEGS/dZBYKF1JibjyFWhRMSofgT1fd0pCbEg2J06+lTgrq\nKZqVnokCgYEA/oK5ACAP6R5ZWVtf4Rr9He1+rAX7punDq6rgOETQZo6VXygsnQB8\nNTLglWTWtMDG8RhVH1hDHYMNxI/kjPa5ufuEKDX8t76IjmpcNReG7Mmk8EsnP7wZ\n7sktkT4euLffEtswFnZSvTXjjJfJ36hoqG1rva9fumdmnfjsFYHz3M0CgYEA0Sv0\nILoDo+56ZOCtnVcI9MztS1SZ9NMDat1zQJwy0jAamCDl8phr1L2xVmABRvCzF8ww\n9YNd8GHpM2jeJ/zQJEE9A49DwpEozqkUyJOQlf2mkynCpeAWbzeKPCnE6pjRxOhS\nCD2YlittomFICG+Oz0YF3FvM04JAfk8SaY/QeVsCgYAF00AgvJsgns00ul6rbE62\nzKTFky17WIZd+38+SnTqpADPOMAsp8IwBYYWZUR0xqTHB+OK4B5JIqLCCPkWMN93\nXkZcFUA/hGDjwZNys6Mm8EaSKWYwk0GUsY4VcPKLD8pPSC0Flpe2NPGMWj0InXYn\nxLlekhEC1zMGEph0tZC7UQKBgQC2M8JIy5+mKXKF8sxolMrhaqx9BOnCwBhm+xWM\nv0LpoitYPrfutOShAMe7xI74O2zVmvE+uzqVzMP1GzEAxhCMtCMwuZe9oPJZ6iCH\ngzrb3IJACI1cLjF8mXWZaIz2CT4YdoIWWYlTreqnGnRDnOFaHdW3GGP4DaqeG8Sf\nHT3spwKBgQDA/377NV5T6k1jL6p3c40dXp9kyQkHMHGAlbYI9JfODXbzlFIP00IX\n8Tn0vKEdEvjROFvE2G2stKbtE+6PQkOyncvw07MMQV3vlSx5wDcwiZSoIFfiG4FO\n2iIVK00G1NNAIwtBRigLSmUUYkfk9uU8P8g1Wnu8EqmPu+rvzYmhpA==\n-----END RSA PRIVATE KEY-----',
    '-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAz/Rrs07MN32whp9A/pub\nfwd1XXr+207tx354Xz7uZZ8udW93fK9rCbIJLcLi15YUNrwbbj9KRXhvWEc7mgEP\nZu+oHE1BtD8IrXuiSYUuRDdRdRgHd9DxOrAs05YvFYrfabQk67XHa7jAnIF5fr1x\nd6Z992i/hY8b7jdTmaxuUhMOiOZK/wNl6ya/DN588WTQ3gevYziPdfEuj9U44mos\nUmYQpLgv0eeoAcsz4Tm7Q2pbzqNPVdccO7ArZg52q/1W68oJeHNLQ4myNKeBZdEo\nTZOIz/RWsu8cwR+7B77ZJMFT42twSJjY2XfxUA8rgbaOcOcnMeAabmgf8d6gXpxh\n3wIDAQAB\n-----END PUBLIC KEY-----',
    '2021-03-16 11:11:11'
);


INSERT INTO oauth_user (id, create_time, username, password, enabled, organization_id, metadata, using_2fa, secret, last_updated)
VALUES
(
    '6c580763-c0c1-4f26-92c6-ffeba50dc4d5',
    '2021-03-16 11:11:11',
    'test',
    '$2a$10$wA81VFAYEhmT2zRzu0sfZ.iK.CHu6ZQ8UZkFFc0rBAyiPTj82zE0a',
    TRUE,
    '65eb8936-b6a3-4af9-8391-059dc22982c2',
    '{}',
    true,
    'ERN7C3DG3GWBKDF6JXRCQIAF4M24GMQ7NZXL5JF4XPQU45N3R642VTCKHTIRU72W',
    '2021-03-16 11:11:11'
);