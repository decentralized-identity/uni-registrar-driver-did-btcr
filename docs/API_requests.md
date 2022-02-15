# BTCR Driver Requests 

## Register

### Requesting Funding Ticket

```json
{
    "options": {
        "chain": "TESTNET", // TESTNET | REGTESTNET | MAINNET 
        "fundingRequest": null // Value pair is ignored
    }
}
```

### Register With Server Fundings

```json
{
  "options": {
    "chain": "TESTNET" // TESTNET | REGTESTNET | MAINNET 
  },
  "didDocument": {
    "@context": "https://www.w3.org/2019/did/v1",
    "id": "test2",
    "publicKey": [
      {
        "id": "d452951f-3907-42c7-805b-e02c99beda6f",
        "type": [
          "CryptographicKey",
          "EdDsaSAPublicKey"
        ],
        "publicKeyHex": "024a63c4362772b0fafc51ac02470dae3f8da8a05d90bae9e1ef3f5243180120dd"
      }
    ],
    "service": [
      {
        "type": "agent",
        "serviceEndpoint": "https://azure.microsoft.com/dif/hub/did:btcr:xkrn-xzcr-qqlv-j6sl"
      },
      {
        "type": "xdi",
        "serviceEndpoint": "https://xdi03-at.danubeclouds.com/cl/:did:btcr:xkrn-xzcr-qqlv-j6sl"
      }
    ],
    "authentication": [
      {
        "id": "cn23",
        "type": "EdDsaSAPublicKey",
        "publicKey": "#satoshi"
      }
    ]
  } // Optional
}
```

### Register With Funding Ticket

```json
{
    "options": {
        "chain": "TESTNET" // TESTNET | REGTESTNET | MAINNET 
    },
    "secret": {
        "fundingTicket": "6a24277a-4585-40af-986f-0f3e660db76a" // Funding Ticket given by the server
    },
    "didDocument": null // Optional
}
```

### Register With Private Key

```json
{
    "options": {
        "chain": "REGTESTNET" // *required: TESTNET | REGTESTNET | MAINNET 
    },
    "secret": {
        "privateKeyWiF": "cMqB9UZFNDSXxGADG2YR7kSZbyvHXPktZe22LQJeyEdyxSnkG6yg", // private key to spend given UTXO
        "fundingTX": "4a144c24f49c4678f25f9911a499c610b436db52d746e8e0bc8a68b41bbcc905", // TX ID to locate UTXO
        "outputIndex": 1 // Index of the UTXO
    },
    "didDocument": null // Optional
}
```



## Update

```json
{
    "did": "did:btcr:xqe8-vr9q-qsjh-whp", // DID-BTCR
    "options": {
        "chain": "TESTNET", // TESTNET | REGTESTNET | MAINNET 
        "rotateKey": true // true | false: creates a new random key for change address if true 
    },
    "secret": {
        "privateKeyWiF": "cSCsLmtyTntQSchToNFwiHHeJFMy5jDeBVVVTGRd128gatJhe4sY" // key that can spend current change address's corrseponding UTXO
    },
    "didDocument": null // Optional
}
```



## Deactivate

```json
{
    "did": "did:btcr:xc7q-qqpq-qsjg-4xv", // DID-BTCR
    "options": {
        "chain": "REGTESTNET" // TESTNET | REGTESTNET | MAINNET 
    },
    "secret": {
        "privateKeyWiF": "cQ5nQS142AixLMtmLHDrCmbdeE3T9yWQGjZSFbpJWaZjNKp9Uo6A" // key that can spend current change address's corrseponding UTXO
    }
}
```
## Checking Operation Updates

```json
{
  "jobId" : "60ab273d-942c-43f7-9c8a-389969533535" // Given by the server for multi-stage operations
}
```