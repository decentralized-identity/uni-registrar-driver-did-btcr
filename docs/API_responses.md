# BTCR Driver Responses

## Register

### Funding Ticket Response

```json
{
    "methodMetadata": {
        "fundingInfo": {
            "ticket": "77ce6fc3-045d-4628-9085-4ccc7b0cc375", // Ticket to use for the registration operation, after funding the given addresss
            "address": "mxFgR7Ms16SPr9w3k3DFGRgYSyqaUeNnq8" // Address to send coins 
        }
    }
}
```

### Initial Phase (Before TX is con firmed)

```json
{
    "jobId": "f9b1687b-5060-4791-868e-86e09a8c7e80", // UUID to ask for operation states
    "didState": {
        "wait": "confirm",
        "state": "wait",
        "waittime": "600" // approx. time (in seconds) until TX gets confirmed
    },
    "registrarMetadata": {},
    "methodMetadata": {
        "register_init_time": "2020-06-23T14:55:32Z", // Time when tx is broadcast
        "chain": "REGTESTNET",
        "transactionHash": "af067696d242e9886cebf744b22cdeeb5f2ae63358a60e61fe64cabaf0502756",
        "balance": 99000, // change address's UTXO balance
        "changeAddress": "mq8f7FCGCkw6uDSA5TJeZfWVK9yBtm7N3j",
        "publicKeyHex": "036b2d99312abb986656f6baf4b54ae1165c9f4d823a5c2f3ed28517d06569fc50",
        "didContinuationUri": "file://56d8fc08-d15c-44a0-98a7-7629259c5e1a.jsonld" // Null if DID Doc given for the initial registration, continuation doc's link otherwise. Note: file is not supported per BTCR specs, this is used for development purposes
    }
}
```

###  Completed State

```json
{
  "jobId": "f9b1687b-5060-4791-868e-86e09a8c7e80",
  "didState": {
    "did": "did:btcr:xx0p-qqpq-q3l6-gpt",
    "state": "finished",
    "secret": {
      "register_keys": [
        {
          "publicKeyHex": "02b67c6a455891bd8c20262a3f09ec610d68ffd70759ed0f6e68001ad846bf34b7",
          "privateKeyHex": "ec4e39a41eb19b580e5eeaf5865db1c8ddd4195bcf6074c8a543b823c4125826",
          "privateKeyWif": "cVW3nMwBVrCBNi6hk2SZadj52fMAWVuxXs2xEK1R2SzoVxiC3Fai", // Initial key used for DID creation TX
          "privateKeyJwk": {
            "kty": "EC",
            "d": "7E45pB6xm1gOXur1hl2xyN3UGVvPYHTIpUO4I8QSWCY",
            "crv": "secp256k1",
            "x": "tnxqRViRvYwgJio_CexhDWj_1wdZ7Q9uaAAa2Ea_NLc",
            "y": "IZ0uQecwdViZ4oS7qfMI0a5hxY-0iRgfL1BQOcd-NCI"
          },
          "publicKeyDIDURL": "did:btcr:xx0p-qqpq-q3l6-gpt#key-0"
        }
      ],
      "update_keys": [
        {
          "publicKeyHex": "036b2d99312abb986656f6baf4b54ae1165c9f4d823a5c2f3ed28517d06569fc50",
          "privateKeyHex": "f72cc949f1bff0922393e7a65775e9c7d28bea100dc1c4c164cac6ff8b4689a3",
          "privateKeyWif": "cVsBF9iTZaKaPVmjUUgfNVWnNDzynKMWL1EEoSJosgFqrxRikZ1z", // Key for change address, i.e., for update and deactivation operations
          "privateKeyJwk": {
            "kty": "EC",
            "d": "9yzJSfG_8JIjk-emV3Xpx9KL6hANwcTBZMrG_4tGiaM",
            "crv": "secp256k1",
            "x": "ay2ZMSq7mGZW9rr0tUrhFlyfTYI6XC8-0oUX0GVp_FA",
            "y": "c9GnIS3TFHv0kdrA17OWyLWfsl5SyReoIhY2SnzTaBk"
          }
        }
      ]
    }
  },
  "registrarMetadata": {},
  "methodMetadata": {
    "register_completion_time": "2020-06-23T14:55:44Z",
    "chain": "REGTESTNET",
    "transactionHash": "af067696d242e9886cebf744b22cdeeb5f2ae63358a60e61fe64cabaf0502756",
    "blockHeight": 755,
    "transactionPosition": 1,
    "txoIndex": -1,
    "didContinuationUri": "file://56d8fc08-d15c-44a0-98a7-7629259c5e1a.jsonld",
    "operation": "register"
  }
}
```

## Update

### Initial Phase

```json
{
    "jobId": "d484d49e-5c0a-4593-aaf7-13a0d6a783ff",
    "didState": {
        "wait": "confirm",
        "state": "wait",
        "waittime": "600"
    },
    "methodMetadata": {
        "update_init_time": "2020-06-23T14:58:33Z",
        "did": "did:btcr:xx0p-qqpq-q3l6-gpt",
        "chain": "REGTESTNET",
        "transactionHash": "f8ff723a65d8a260e7203fb25061f01f6228e730244a6bd0197ba4fc6c498fc9",
        "balance": 98000,
        "changeAddress": "mq8f7FCGCkw6uDSA5TJeZfWVK9yBtm7N3j", // Will be same if key rotation is not requested
        "publicKeyHex": "03bfcf19a97f6c79b81a03166cf70280423168d09d5f8e5ff5b882262ac77b1f3f",
        "didContinuationUri": "file://433a50d3-8aed-4b77-add4-ce89f727d37e.jsonld"
    },
    "registrarMetadata": {}
}
```

### Completed State

```json
{
    "jobId": "d484d49e-5c0a-4593-aaf7-13a0d6a783ff",
    "didState": {
        "did": "did:btcr:xx0p-qqpq-q3l6-gpt",
        "state": "finished",
        "secret": { // New secret is returned if key-rotation was requested
            "keys": [{
                "publicKeyHex": "03bfcf19a97f6c79b81a03166cf70280423168d09d5f8e5ff5b882262ac77b1f3f",
                "privateKeyHex": "7e8e4adfa7b57fd69ef532e15cce3dfabcf5facf51784d93dd6a6197c267530f",
                "privateKeyWif": "cRpi7UDSv21LV3XNSyYYY3ga9VhnuVPDHZeTguFu5gmUibiQfPJZ",
                "privateKeyJwk": {
                    "kty": "EC",
                    "d": "fo5K36e1f9ae9TLhXM49-rz1-s9ReE2T3Wphl8JnUw8",
                    "crv": "secp256k1",
                    "x": "v88ZqX9sebgaAxZs9wKAQjFo0J1fjl_1uIImKsd7Hz8",
                    "y": "5bGcK_YVV4Wol0tZ4FMebgEUH_Hnh5Dshk1kxeRx7wU"
                },
                "publicKeyDIDURL": "did:btcr:xx0p-qqpq-q3l6-gpt#key-0"
            }]
        }
    },
    "methodMetadata": {
        "update_completion_time": "2020-06-23T14:59:04Z",
        "chain": "REGTESTNET",
        "transactionHash": "f8ff723a65d8a260e7203fb25061f01f6228e730244a6bd0197ba4fc6c498fc9",
        "blockHeight": 767,
        "transactionPosition": 1,
        "txoIndex": 0,
        "didContinuationUri": "file://433a50d3-8aed-4b77-add4-ce89f727d37e.jsonld",
        "operation": "update"
    },
    "registrarMetadata": {}
}
```

## Deactivation

**Note:** If driver is not configured for tracking the deactivation confirmations, server will directly yield the state as finished

```json
{
    "jobId": "f401a032-4e1a-4775-957c-274d0d00cc10",
    "didState": {
        "did": "did:btcr:xs2p-qqzq-qpd4-55s",
        "state": "finished",
        "secret": null 
    },
    "methodMetadata": {
        "deactivate_init_time": "2020-06-23T14:28:51Z",
        "chain": "REGTESTNET",
        "transactionHash": "255b3b674738af38ad1956f2b30d419ac614ded7026ddfe680ee8366d3f0d0bd",
        "changeAddress": "n3e6oTjLAa8yWN9iEQVjW8ngL5J8DS8M66",
        "balance": "95000",
        "old_did_continual_uri": "file://c217d665-4d74-4e3b-ad95-6b16b5008049.jsonld", // most recent cont. uri before deactivation
        "operation": "deactivation"
    },
    "registrarMetadata": {}
}
```

