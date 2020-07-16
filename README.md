![DIF Logo](https://raw.githubusercontent.com/decentralized-identity/universal-registrar/master/docs/logo-dif.png)

# Universal Registrar Driver: did:btcr

This is a [Universal Registrar](https://github.com/decentralized-identity/universal-registrar/) driver for **did:btcr** identifiers.

[[_TOC_]]



## Specifications

* [Decentralized Identifiers](https://w3c.github.io/did-core/)
* [DID Method Specification](https://w3c-ccg.github.io/didm-btcr/)



## Build and Run (Docker)

```
docker build -f ./docker/Dockerfile . -t universalregistrar/driver-did-btcr
docker run -p 9080:9080 universalregistrar/driver-did-btcr
```



## Driver Configurations

The BTCR Driver can be configured with environment variables and/or passing application properties. In case no properties file is provided, the driver will try to load [driver.properties](src/main/resources/driver.properties). If a parameter has value as an environment variable, it will be used over its field in the properties file. 

- [Driver Variables Table](docs/driver_variables_table.md)

- [Driver Defaults](docs/driver_defaults.md)

- [Example Application Properties](config_examples/example-application.properties) and its [Corresponding Environment Variables](config_examples/example-env).

  

  **Requirements**:  

  - Values have to be passed to the parameters without default values. 
  - Private Key for the wallet must be dumped for an address of **bech32** format!
  - BTCD or Bitcoin Core full node is required
  - The given bitcoin full node must be configured with txindex=1 (additionally addrindex=1 for BTCD)
  - Only the use of **P2PKH** addresses is supported in this version. 

  

## Operations

Please check:

- [API Requests](docs/API_requests.md) and [API Responses](docs/API_responses.md)

## Implementation Notes

This iteration of the implementation is **highly experimental**. Stability issues are possible. 

- Most importantly, **tests and proper abstraction of the components are lacking**. 
- Transaction fees are not calculated per transaction; instead, default constant fees are deducted. This needs to be adjusted.
- The implementation process started with highly relying on the BitcoinJ library. Initial startup is blocking until BitcoinJ syncs up, it can take a long time. 
- Failure mechanisms need to be refactored, especially rejected transactions.



