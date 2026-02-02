# Java SDK for The Open Network (TON)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Based on TON][ton-svg]][ton]
![GitHub last commit](https://img.shields.io/github/last-commit/ton-blockchain/ton4j)
![](https://tokei.rs/b1/github/ton-blockchain/ton4j?category=code)
![](https://tokei.rs/b1/github/ton-blockchain/ton4j?category=files)


Java libraries and wrapper for interacting with TON blockchain. ton4j requires minimum `Java 11`.

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml

<dependency>
    <groupId>org.ton.ton4j</groupId>
    <artifactId>smartcontract</artifactId>
    <version>1.3.5</version>
</dependency>
```

## Jitpack [![JitPack][jitpack-svg]][jitpack]

```xml

<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

```xml

<dependency>
    <groupId>org.ton.ton4j</groupId>
    <artifactId>ton4j</artifactId>
    <version>1.3.5</version>
</dependency>
```

You can use each submodule individually. Click the module below to get more details.

* [Tonlib](tonlib/README.md) - use external Tonlib shared library to communicate with TON blockchain.
* [Adnl](adnl/README.md) - Lite-client based on native ADNL protocol.
* [SmartContract](smartcontract/README.md) - create and deploy custom and predefined smart-contracts.
* [Cell](cell/README.md) - create, read and manipulate Bag of Cells.
* [BitString](bitstring/README.md) - construct bit-strings.
* [Address](address/README.md) - create and parse TON wallet addresses.
* [Mnemonic](mnemonic/README.md) - helpful methods for generating deterministic keys for TON blockchain.
* [Emulator](emulator/README.md) - wrapper for using with external precompiled emulator shared library.
* [Exporter](exporter/README.md) - TON database reader/exporter that uses RocksDB Java JNA libraries.
* [Liteclient](liteclient/README.md) - wrapper for using with external precompiled lite-client binary.
* [TonCenter Client V2](toncenter/README.md) - wrapper used to send REST calls towards [TonCenter API v2](https://toncenter.com/api/v2/) .
* [TonCenter Client V3](toncenter-indexer-v3/README.md) - wrapper used to send REST calls towards [TonCenter Indexer API v3](https://toncenter.com/api/v3/) .
* [Fift](fift/README.md) - wrapper for using external precompiled fift binary.
* [Func](func/README.md) - wrapper for using external precompiled func binary.
* [Tolk](tolk/README.md) - wrapper for using external precompiled tolk binary.
* [TonConnect](tonconnect/README.md) - implementation of Ton Connect standard.
* [Disassembler](disassembler/README.md) - implementation of Ton Connect standard.
* [TL-B](tlb/README.md) - TL-B structures and their de/serialization.
* [TL](tl/README.md) - TL structures and their de/serialization. Used mainly for lite-server queries and responses as well as for RockDB key/values. 
* [Utils](utils/README.md) - create private and public keys, convert data, etc.

## How to use

- [Connection](#connection)
  - [Tonlib shared library](#tonlib)
  - [ADNL lite-client](#adnl-lite-client)
  - [Native lite-client](#native-lite-client)
  - [TonCenter API V2](#toncenter-api-v2)
  - [TonCenter API V3](#toncenter-api-v3)
- [Smart contract address](#smart-contract-address)
- [Wallets](#Wallets)
  - [Create](#create-wallet)
  - [Transfer](#transfer-toncoins-in-testnet)  
  - [Deploy and transfer with externally signed](#deploy-and-transfer-toncoins-signed-externally)  
  - [Transfer to up to 4 recipients](#Transfer-to-up-to-4-recipients)
  - [Transfer to up to 1000 recipients](#Transfer-to-up-to-1000-recipients)
  - [Transfer to up to 1000 recipients using Secp256k1 and externally signed](#Transfer-to-up-to-1000-recipients-using-Secp256k1-and-externally-signed)
  - [Send message to contract](#send-a-message-to-a-contract)
  - [Send message signed externally](#send-message-signed-externally)
- [Accounts](#Accounts)
  - [List transactions](#get-account-transactions)
  - [List messages](#get-account-messages)
  - [Get balance](#get-account-balance)
  - [Get state](#get-account-state)
- [Get block](#get-blockchain-block)
- [Mnemonic](#Generate-mnemonic-and-keypair)
- [NFT](#NFT)
  - [Get info](#Retrieve-nft-information)
  - [Mint](#Mint-nft)
  - [Transfer](#Transfer-nft)
- [Jettons](#Jettons)
  - [Get info](#Retrieve-jetton-info)
  - [Mint](#Mint-jetton)
  - [Transfer](Transfer-jetton)
- [DNS](#DNS)
  - [Resolve](#Resolve-DNS)
  - [Get records](#Get-DNS-records)
  - [Set records](#Set-DNS-records)
- [Contracts](#Contracts)
  - [Retrieve contract's information](#Using-GET-methods)
  - [Compile](#Compile-smart-contract)
- [BitString](#BitString)
- [Cells](#Cells)
  - [Create using CellBuilder](#Cell-Builder)
  - [Parse using CellSlice](#Cell-Slice)
  - [TLB Loader/Serializer](#TLB-Loader)
  - [BoC](#BoC)
  - [Proof creation](#Proofs)
- [Emulator](emulator)
  - [TVM emulator](#tvm-emulator)
  - [TX emulator](#tx-emulator)    
- [TON Connect](#ton-connect)
- [Smart contract disassembler](#disassembler)
- [Notes](#notes)

## Connection
In the TON ecosystem you can interact with a TON blockchain in 4 ways:
  - tonlib shared library — connect to lite-server via tonlibjson.so/dll/dylib shared library;
  - ADNL lite-client - used to connect to lite-server using native Java ADNL protocol implementation; In the current implementation it does not download proofs on start and thus is much faster than tonlibjson.  
  - Native lite-client - a java wrapper to compile lite-client executable. Handles and parses responses returned by lite-client. Obsolete way of connecting to TON blockchain and should not be used.
  - TonCenter API - a java wrapper to interact with a [TonCenter HTTP API](https://toncenter.com/) service. For production usage consider obtaining API key.  

### Tonlib

Connect to the TON Mainnet with the latest tonlibjson downloaded from the TON github release

```java
Tonlib tonlib =
    Tonlib.builder()
        .pathToTonlibSharedLib(Utils.getTonlibGithubUrl())
        .testnet(false)
        .build();
BlockIdExt block = tonlib.getLast().getLast();
log.info("block {}", block);
```
More examples in [tests](tonlib/src/test/java/org/ton/ton4j/tonlib/TestTonlibJson.java).

### ADNL lite-client

Connect to the TON Mainnet

```java
TonGlobalConfig tonGlobalConfig = TonGlobalConfig.loadFromUrl(Utils.getGlobalConfigUrlMainnetGithub());
AdnlLiteClient client = AdnlLiteClient.builder().globalConfig(tonGlobalConfig).build();
MasterchainInfo info = client.getMasterchainInfo();
```
Connect to the TON Testnet
```java
TonGlobalConfig tonGlobalConfig = TonGlobalConfig.loadFromUrl(Utils.getGlobalConfigUrlTestnetGithub());
AdnlLiteClient client = AdnlLiteClient.builder().globalConfig(tonGlobalConfig).build();
MasterchainInfo info = client.getMasterchainInfo();
```

Connect to MyLocalTon
```java
TonGlobalConfig tonGlobalConfig = TonGlobalConfig.loadFromUrl(Utils.getGlobalConfigUrlMyLocalTon());
AdnlLiteClient client = AdnlLiteClient.builder().globalConfig(tonGlobalConfig).build();
MasterchainInfo info = client.getMasterchainInfo();
```
More examples in [tests](adnl/src/test/java/org/ton/ton4j/adnl/AdnlLiteClientTest.java).

### Native lite-client
Download lite-client executable and run its methods
```java
LiteClient liteClient =
  LiteClient.builder()
    .testnet(false)
    .pathToLiteClientBinary(Utils.getLiteClientGithubUrl())
  .build();
liteClient.executeLast();
liteClient.executeRunMethod(
    "EQDCJVrezD71y-KPcTIG-YeKNj4naeiR7odpQgVA1uDsZqPC",
            "(-1,8000000000000000,20301499):070D07EB64D36CCA2D8D20AA644489637059C150E2CD466247C25B4997FB8CD9:D7D7271D466D52D0A98771F9E8DCAA06E43FCE01C977AACD9DE9DAD9A9F9A424",
            "seqno",
            "");
```
Parse result if required
```java
LiteClient liteClient =
        LiteClient.builder()
                .testnet(false)
                .pathToLiteClientBinary(Utils.getLiteClientGithubUrl())
                .build();
String resultLast = liteClient.executeLast();
ResultLastBlock blockIdLast = LiteClientParser.parseLast(resultLast);
String stdout =
    liteClient.executeBySeqno(
        blockIdLast.getWc(), blockIdLast.getShard(), blockIdLast.getSeqno());
ResultLastBlock blockId = LiteClientParser.parseBySeqno(stdout);
```
Download block's dump and parse it
```java
LiteClient liteClient =
        LiteClient.builder()
                .testnet(false)
                .pathToLiteClientBinary(Utils.getLiteClientGithubUrl())
                .build();
String stdoutLast = liteClient.executeLast();
ResultLastBlock blockIdLast = LiteClientParser.parseLast(stdoutLast);
String stdoutDumpblock = liteClient.executeDumpblock(blockIdLast);
Block block = LiteClientParser.parseDumpblock(stdoutDumpblock, false, false);
```
More examples in [tests](liteclient/src/test/java/org/ton/ton4j/liteclient/LiteClientTest.java).

### TonCenter API V2

Run get method on smart contract
```java
TonCenter client = TonCenter.builder().apiKey("your-toncenter-api-key").network(Network.MAINNET).build();
try {
  TonResponse<RunGetMethodResponse> response =  client.runGetMethod(tonWallet, "seqno", new ArrayList<>());
  log.info("response {}", response.getResult());
  log.info("Get method 'seqno' executed successfully");
} finally {
  client.close();
}
```

Get seqno of a contract
```java
TonCenter client = TonCenter.builder().apiKey("your-toncenter-api-key").network(Network.MAINNET).build();
try {
  long seqno = client.getSeqno(tonWallet);      
  log.info("Get method 'seqno' executed successfully, seqno {}", seqno);
} finally {
  client.close();
}
```
More TonCenter V2 examples in [tests](toncenter/src/test/java/org/ton/ton4j/toncenter/TonCenterTest.java).

### TonCenter API V3

Get account states

```java
TonCenterV3 client =
  TonCenterV3.builder()
    .mainnet()
    .connectTimeout(Duration.ofSeconds(15))
    .readTimeout(Duration.ofSeconds(30))
    .apiKey("your-toncenter-api-key")
    .build();

List<String> addresses = Collections.singletonList("0:a44757069a7b04e393782b4a2d3e5e449f19d16a4986a9e25436e6b97e45a16a");
AccountStatesResponse response = client.getAccountStates(addresses, true);
log.info("Retrieved {} account states", response.getAccounts().size());
```

Get top accounts by balance
```java
TonCenterV3 client =
  TonCenterV3.builder()
    .mainnet()
    .connectTimeout(Duration.ofSeconds(15))
    .readTimeout(Duration.ofSeconds(30))
    .apiKey("your-toncenter-api-key")
    .build();
try {
  List<AccountBalance> response = client.getTopAccountsByBalance(10, 0);
  assertNotNull(response);
  log.info("Retrieved {} top accounts", response.size());
  if (!response.isEmpty()) {
    log.info("Top account balance: {}", response.get(0).getBalance());
  }
  log.info(response.toString());
} finally {
  client.close();
}
```

Get traces
```java
TonCenterV3 client =
  TonCenterV3.builder()
    .mainnet()
    .connectTimeout(Duration.ofSeconds(15))
    .readTimeout(Duration.ofSeconds(30))
    .apiKey("your-toncenter-api-key")
    .build();
try {
  TracesResponse response = client.getTraces(TEST_ADDRESS, null,null, null, null, null, null, null, null, null, null, 10, 0, "desc");
  assertNotNull(response);
  log.info("Retrieved traces");
} finally {
  client.close();
}
```
More TonCenter V3 examples in [tests](toncenter-indexer-v3/src/test/java/org/ton/ton4j/toncenterv3/TonCenterV3Test.java).

## Smart contract address
In TON smart contract address have various [formats](https://docs.ton.org/foundations/addresses/formats).

```java
TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
WalletV3R2 wallet = WalletV3R2.builder().keyPair(keyPair).walletId(42).build();

String raw = wallet.getAddress().toRaw();
String bounceableTestnet = wallet.getAddress().toBounceableTestnet();
String nonBounceableTestnet = wallet.getAddress().toNonBounceableTestnet();

String bounceableMainnet = wallet.getAddress().toBounceable();
String nonBounceableMainnet = wallet.getAddress().toNonBounceable();
```
Parse and convert base64 address to raw format
```java
Address address = Address.of("EQDKbjIcfM6ezt8KjKJJLshZJJSqX7XOA4ff-W72r5gqPrHF");
String rawAddress = address.toRaw();
```
More examples in [tests](address/src/test/java/org/ton/ton4j/address/TestAddress.java).

## Wallets
In TON there are [many types of wallets](https://docs.ton.org/standard/wallets/history), i.e., smart contracts. 
The most popular ones are V3R2 and V4R2 and V5R1. 
Some of them are advanced version of the previous ones and some have specific purpose, like vesting and multisig. 

### Create wallet
Create a simple wallet V3R2 in the Mainnet
```java
// prepare 
TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
AdnlLiteClient adnlLiteClient =
    AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlMainnetGithub()).build();
WalletV3R2 contract =
    WalletV3R2.builder().adnlLiteClient(adnlLiteClient).keyPair(keyPair).walletId(42).build();

// to deploy a wallet, you have to top it up with some toncoins first
String nonBounceableAddress = contract.getAddress().toNonBounceable();
String bounceableAddress = contract.getAddress().toBounceable();
String rawAddress = contract.getAddress().toRaw();
log.info("non-bounceable address: {}", nonBounceableAddress);
log.info("    bounceable address: {}", bounceableAddress);
log.info("           raw address: {}", rawAddress);

// retrieve the address and send some toncoins to it, normally up to 0.1 toncoins are more than enough
// then deploy the wallet
contract.deploy();
```

### Transfer toncoins in Testnet
```java
// prepare 
TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
AdnlLiteClient adnlLiteClient =
    AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlMainnetGithub()).build();
WalletV3R2 contract =
    WalletV3R2.builder().adnlLiteClient(adnlLiteClient).keyPair(keyPair).walletId(42).build();

// to deploy a wallet, you have to top it up with some toncoins first
String nonBounceableAddress = contract.getAddress().toNonBounceable();
String bounceableAddress = contract.getAddress().toBounceable();
String rawAddress = contract.getAddress().toRaw();
log.info("non-bounceable address: {}", nonBounceableAddress);
log.info("    bounceable address: {}", bounceableAddress);
log.info("           raw address: {}", rawAddress);

// in testnet you can use a helper method that uses Testnet Faucet to top up the address with test toncoins
TestnetFaucet.topUpContract(adnlLiteClient, Address.of(nonBounceableAddress), Utils.toNano(1));

// deploy the wallet
contract.deploy();

// check if wallet is deployed
contract.isDeployed();

//send toncoins
WalletV3Config config =
  WalletV3Config.builder()
    .walletId(42)
    .seqno(contract.getSeqno())
    .destination(Address.of(TestnetFaucet.BOUNCEABLE))
    .amount(Utils.toNano(0.8))
    .comment("testWalletV3R2-42")
    .build();

// transfer coins from a new wallet (back to faucet)
contract.send(config);
```

### Deploy and transfer toncoins signed externally
```java
TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
byte[] publicKey = keyPair.getPublicKey();
WalletV3R2 contract = WalletV3R2.builder().tonlib(tonlib).publicKey(publicKey).walletId(42).build();

BigInteger balance = TestnetFaucet.topUpContract(tonlib, contract.getAddress(), Utils.toNano(0.1));
log.info("walletId {} new wallet {} balance: {}",
        contract.getWalletId(),
        contract.getName(),
        Utils.formatNanoValue(balance));

// deploy using an externally signed body
Cell deployBody = contract.createDeployMessage();

byte[] signedDeployBodyHash = Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), deployBody.hash());

contract.deploy(signedDeployBodyHash);
contract.waitForDeployment();

// send toncoins
WalletV3Config config =
  WalletV3Config.builder()
    .walletId(42)
    .seqno(contract.getSeqno())
    .destination(Address.of(TestnetFaucet.BOUNCEABLE))
    .amount(Utils.toNano(0.08))
    .comment("testWalletV3R2-signed-externally")
    .build();

// transfer coins from a new wallet (back to faucet) using an externally signed body
Cell transferBody = contract.createTransferBody(config);
byte[] signedTransferBodyHash = Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), transferBody.hash());
SendResponse sendResponse = contract.send(config, signedTransferBodyHash);
log.info("sendResponse: {}", sendResponse);
contract.waitForBalanceChange();
```

### Transfer to up to 4 recipients
In TON there are [several ways](smartcontract/README-WALLETS.md) how to transfer toncoins to multiple users.

You can use WalletV2R2 to send toncoins to up to four recipients 

```java
// generate keypair
TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
// create wallet
WalletV2R2 contract = WalletV2R2.builder().tonlib(tonlib).keyPair(keyPair).build();

// deploy wallet as per the above examples and then define recipients
config = WalletV2R2Config.builder()
        .seqno(contract.getSeqno())
        .destination1(Address.of("recipient-1"))
        .destination1(Address.of("recipient-2"))
        .destination1(Address.of("recipient-3"))
        .destination1(Address.of("recipient-3"))
        .amount1(Utils.toNano(0.15))
        .amount2(Utils.toNano(0.15))
        .amount3(Utils.toNano(0.15))
        .amount4(Utils.toNano(0.15))
        .build();

// send
contract.send(config);
```

Or you can use WalletV3R2 and construct a body with up to 4 recipients yourself.
Refer to [this](smartcontract/src/main/java/org/ton/ton4j/smartcontract/wallet/v2/WalletV2R2.java) example, method `createTransferBody`, that contract cell with 4 references. 

### Transfer to up to 1000 recipients
To send toncoins or custom payloads to more than 4 recipients, use Highload Wallet V3.  
```java
TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

HighloadWalletV3 contract = HighloadWalletV3.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();

String nonBounceableAddress = contract.getAddress().toNonBounceable();
String bounceableAddress = contract.getAddress().toBounceable();
String rawAddress = contract.getAddress().toRaw();

log.info("non-bounceable address {}", nonBounceableAddress);
log.info("    bounceable address {}", bounceableAddress);
log.info("           raw address {}", rawAddress);
log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

// top up new wallet using test-faucet-wallet
BigInteger balance =  TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(2));
Utils.sleep(30, "topping up...");
log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

HighloadV3Config config =
        HighloadV3Config.builder()
                .walletId(42)
                .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
                .build();

SendResponse sendResponse = contract.deploy(config);
assertThat(sendResponse.getCode()).isZero();

contract.waitForDeployment();

config = HighloadV3Config.builder()
        .walletId(42)
        .queryId(HighloadQueryId.fromSeqno(1).getQueryId())
        .body(contract.createBulkTransfer(createDummyDestinations(1000), BigInteger.valueOf(HighloadQueryId.fromSeqno(1).getQueryId())))
        .build();

sendResponse = contract.send(config);
assertThat(sendResponse.getCode()).isZero();
log.info("sent to 1000 recipients");
```

In the example above we used method `createDummyDestinations()`, replace it with your logic defining recipients 
```java
  List<Destination> createDummyDestinations(int count) {
    List<Destination> result = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String dstDummyAddress = Utils.generateRandomAddress(0);

      result.add(
          Destination.builder()
              .bounce(false)
              .address(dstDummyAddress)
              .amount(Utils.toNano(0.001))
              // .comment("comment-" + i)
              .build());
    }
    return result;
  }
```

### Transfer to up to 1000 recipients using Secp256k1 and externally signed

```java

Secp256k1KeyPair keyPair = Utils.generateSecp256k1SignatureKeyPair();
byte[] pubKey = keyPair.getPublicKey();

HighloadWalletV3S contract =
    HighloadWalletV3S.builder()
        .tonlib(tonlib)
        .publicKey(pubKey) // no private key is used
        .walletId(42)
        .build();

String nonBounceableAddress = contract.getAddress().toNonBounceable();
String bounceableAddress = contract.getAddress().toBounceable();
String rawAddress = contract.getAddress().toRaw();

log.info("non-bounceable address {}", nonBounceableAddress);
log.info("    bounceable address {}", bounceableAddress);
log.info("           raw address {}", rawAddress);

// top up a new wallet using test-faucet-wallet
BigInteger balance =
    TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.5));
Utils.sleep(30, "topping up...");
log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

HighloadV3Config config =
    HighloadV3Config.builder()
        .walletId(42)
        .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
        .build();

Cell deployBody = contract.createDeployMessage(config);

// sign deployBody without exposing the private key and come back with a signature
byte[] signedDeployBody = Utils.signDataSecp256k1(deployBody.hash(), keyPair.getPrivateKey(), pubKey).getSignature();

SendResponse sendResponse = contract.deploy(config, signedDeployBody);
assertThat(sendResponse.getCode()).isZero();

contract.waitForDeployment();

int queryId = HighloadQueryId.fromSeqno(1).getQueryId();
config = HighloadV3Config.builder()
        .walletId(42)
        .queryId(queryId)
        .body(contract.createBulkTransfer(createDummyDestinations(1000), BigInteger.valueOf(HighloadQueryId.fromSeqno(1).getQueryId())))
        .build();

Cell transferBody = contract.createTransferMessage(config);

// sign transferBody without exposing private key and come back with a signature
byte[] signedTransferBody = Utils.signDataSecp256k1(transferBody.hash(), keyPair.getPrivateKey(), keyPair.getPublicKey()).getSignature();

sendResponse = contract.send(config, signedTransferBody);
```

### Send a message to a contract

You can also send a custom payload to a smart contract

```java
// prepare 
TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
AdnlLiteClient adnlLiteClient =  AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlMainnetGithub()).build();
WalletV3R2 contract =  WalletV3R2.builder().adnlLiteClient(adnlLiteClient).keyPair(keyPair).walletId(42).build();

// to deploy a wallet, you have to top it up with some toncoins first
String nonBounceableAddress = contract.getAddress().toNonBounceable();
log.info("non-bounceable address: {}", nonBounceableAddress);

// in testnet you can use a helper method that uses Testnet Faucet to top up the address with test toncoins
TestnetFaucet.topUpContract(adnlLiteClient, Address.of(nonBounceableAddress), Utils.toNano(1));

// deploy the wallet
contract.deploy();

// check if wallet is deployed
contract.isDeployed();

//send toncoins
WalletV3Config config =
  WalletV3Config.builder()
    .walletId(42)
    .seqno(contract.getSeqno())
    .destination(Address.of("destination-wallet-address"))
    .amount(Utils.toNano(0.8))
    .body(CellBuilder.beginCell().storeUint(2, 2).endCell()) // custom payload
    .build();

// transfer toncoins from a new wallet (back to faucet)
contract.send(config);
```

### Send a message signed externally
In a critical infrastructure you store customers' private keys in a secure place, like HSM or cold storage.
In that case you sign data outside the main application and use already signed data within an application.

```java
AdnlLiteClient adnlLiteClient = AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlTestnetGithub()).build();

TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

WalletV3R2 wallet =
    WalletV3R2.builder()
        .adnlLiteClient(adnlLiteClient)
        .publicKey(keyPair.getPublicKey()) // no private key in app
        .walletId(42)
        .build();

// Create a payload, sign elsewhere (HSM), then deploy
Cell deployBody = wallet.createDeployMessage();
byte[] signature = Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), deployBody.hash());

wallet.signature(signedData);
```
More examples on how to work with TON wallets in [tests](smartcontract/src/test/java/org/ton/ton4j/smartcontract).

## Accounts

### Get account transactions

With the help of an ADNL lite-client limited by 10 transactions

```java
AdnlLiteClient adnlLiteClient = AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlMainnetGithub()).build();
TransactionList transactionList = client.getTransactions(Address.of("wallet-address"), 0, null, 10);
for (Transaction tx : transactionList.getTransactionsParsed()) {
    log.info("tx {}", tx);
}
```

With help of Tonlibjson shared library
```java
Tonlib tonlib = Tonlib.builder().pathToTonlibSharedLib(Utils.getTonlibGithubUrl()).build();
tonlib.printAccountTransactions(Address.of("wallet-address"), 20, true);
```

### Get account messages
With help of Tonlibjson shared library
```java
Tonlib tonlib = Tonlib.builder().pathToTonlibSharedLib(Utils.getTonlibGithubUrl()).build();
tonlib.printAccountMessages(Address.of("wallet-address"), 20, true);
```

### Get account balance
With help of Tonlibjson shared library
```java
Tonlib tonlib =
    Tonlib.builder()
        .pathToTonlibSharedLib(Utils.getTonlibGithubUrl())
        .testnet(false)
        .build();
BigInteger balance = tonlib.getAccountBalance(Address.of("wallet-address"));
```

With help of ADNL lite-client
```java
AdnlLiteClient adnlLiteClient = AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlMainnetGithub()).build();
log.info("balance {}", adnlLiteClient.getBalance(Address.of("wallet-address")));
```

### Get account state
- Using TonCenter V3 client
```java
  TonCenterV3 client =
      TonCenterV3.builder()
          .mainnet()
          .connectTimeout(Duration.ofSeconds(15))
          .readTimeout(Duration.ofSeconds(30))
          .apiKey(API_KEY);
          .build(); 
try {
  List<String> addresses = Collections.singletonList(TEST_ADDRESS);
  AccountStatesResponse response = client.getAccountStates(addresses, true);
  assertNotNull(response);
  assertNotNull(response.getAccounts());
  log.info("Retrieved {} account states", response.getAccounts().size());
  log.info(response.toString());
} finally {
  client.close();
}
```

Using an ADNL lite-client
```java
  AdnlLiteClient adnlLiteClient = AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlMainnetGithub()).build();
  MasterchainInfo info = client.getMasterchainInfo();
  AccountState accountState = client.getAccountState(info.getLast(), Address.of(getAddress()));
```
Many more examples in [tests](smartcontract/src/test/java/org/ton/ton4j/smartcontract).

## Get blockchain block
Get the most recent block using an ADNL lite-client, parsed as per TL-B schema
```java
MasterchainInfo masterchainInfo = client.getMasterchainInfo();
BlockData blockData = client.getBlock(masterchainInfo.getLast());
log.info("Block  {}", blockData.getBlock());
```

## Generate mnemonic and keypair

```java
// generate 24-word mnemonic
String mnemonic = Mnemonic.generateString(24);
// convert it to Pair
Pair keyPair = Mnemonic.toKeyPair(mnemonic);
// use generated mnemonic as a key pair for TON smart contracts
// 64-byte signature keypair (32 pub + 32 priv)
TweetNaclFast.Signature.KeyPair sigKeyPair = TweetNaclFast.Signature.keyPair_fromSeed(keyPair.getSecretKey());

// Quick random signature keypair (no mnemonic)
TweetNaclFast.Signature.KeyPair quickKeyPair = Utils.generateSignatureKeyPair();
```

## NFT
### Retrieve NFT information
### Mint NFT
### Transfer NFT

## Jettons
### Retrieve jetton info
### Mint jetton
### Transfer jetton

## DNS
### Resolve-DNS
### Get DNS records
### Set DNS records

## Contracts
### Using GET methods
### Compile-smart contract

## BitString 
## Cells
### Cell Builder
### Cell Slice
### TLBLoader

## Notes

- Testnet faucet only works on testnet. On mainnet, top up the wallet address externally before deploying.
- Prefer public-key-only flows and external signing when private keys must not be exposed.
- More wallet and smart-contract examples live in `smartcontract/src/test/java/org/ton/ton4j/smartcontract`.

## Features

* ✅ BitString manipulations
* ✅ Cells serialization / deserialization
* ✅ TL-B serialization / deserialization
* ✅ TL serialization / deserialization
* ✅ Cell builder and cell slicer (reader)
* ✅ Tonlib, Lite-client, TVM/TX, Fift, Func and Tolk wrappers
* ✅ ADNL Lite-client
* ✅ TON RocksDB direct access
* ✅ TonConnect
* ✅ TonCenter V2 wrapper
* ✅ TonCenter Indexer V3 wrapper
* ✅ Fift, Func, Tolk wrappers
* ✅ BoC disassembler
* ✅ Extra-currency support and examples
* ✅ Support num, cell and slice as arguments for runMethod
* ✅ Render List, Tuple, Slice, Cell and Number results from runMethod
* ✅ Generate or import private key, sign, encrypt and decrypt using Tonlib
* ✅ Encrypt/decrypt with mnemonic
* ✅ Deploy contracts and send external messages using Tonlib
* ✅ Wallets - Simple (V1), V2, V3, V4 (plugins), V5, Lockup, ~~Highload~~/Highload-V3, Highload-V3S (Secp256k1), DNS,
  Jetton/Jetton V2, StableCoin, NFT,
  Payment-channels, ~~Multisig V1~~, Multisig V2
* ✅ HashMap, HashMapE, PfxHashMap, PfxHashMapE, HashMapAug, HashMapAugE serialization / deserialization

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=ton-blockchain/ton4j&type=Date)](https://www.star-history.com/#ton-blockchain/ton4j&Date)

<!-- Badges -->

[maven-central-svg]: https://img.shields.io/maven-central/v/org.ton.ton4j/smartcontract

[maven-central]: https://mvnrepository.com/artifact/org.ton.ton4j/smartcontract

[jitpack-svg]: https://jitpack.io/v/ton-blockchain/ton4j.svg

[jitpack]: https://jitpack.io/#ton-blockchain/ton4j

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org
