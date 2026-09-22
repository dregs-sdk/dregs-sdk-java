# Dregs Java SDK

[![Maven Central](https://img.shields.io/maven-central/v/com.dregs/dregs-sdk.svg)](https://central.sonatype.com/artifact/com.dregs/dregs-sdk)
[![Java](https://img.shields.io/badge/java-17%2B-blue.svg)](https://whichjdk.com/)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

The official Java client for [Dregs](https://dregs.com), which scores the users of your application
for fraud and abuse across four categories: humanity, authenticity, uniqueness, and behavior.

Send events from your backend, read back the scores and the observations behind them.

```groovy
implementation 'com.dregs:dregs-sdk:0.1.0'
```

```xml
<dependency>
    <groupId>com.dregs</groupId>
    <artifactId>dregs-sdk</artifactId>
    <version>0.1.0</version>
</dependency>
```

Java 17 or newer. The only dependency is [Gson](https://github.com/google/gson).

## Getting started

You need the **secret key** from an API credential, which you will find under **Settings → Credentials**
in the Dregs dashboard. It starts with `sk_`. The `pk_` public key is for the browser tracker and cannot
read identities or scores.

```java
import com.dregs.Dregs;

Dregs client = Dregs.builder().secretKey(System.getenv("DREGS_SECRET_KEY")).build();
```

The key is read from `DREGS_SECRET_KEY` when you do not pass one, so `Dregs.builder().build()` is
usually enough. The client holds a connection pool: build one at startup and keep it, rather than
making a new one per request. It is immutable and safe to share across threads.

## Tracking events

```java
import com.dregs.TrackRequest;

client.track(TrackRequest.builder("user.signup", "user_12345")
        .data(Map.of("plan", "pro", "referrer", "partner-x"))
        .identityData(Map.of("email", "ada@example.com", "name", "Ada Lovelace"))
        .build());
```

The second argument is your own id for the user — the same one you pass to `dregs.identify()` in the
browser tracker, and the one you look scores up by. It is required: a server-side event carries no
device signature, so the identity is the only thing tying the event to a user.

`identityData` carries attributes of the *user* rather than the event. The analyzers lean on these
heavily, so send them whenever you have them. Name the keys the way your application already does and
map them to Dregs's canonical fields under **Settings → Mappings**; the same goes for event names.

For an event with nothing but a type and an identity, there is a short form:

```java
client.track("user.signup", "user_12345");
```

### Idempotency

Every event is sent with an `id`, which makes ingestion idempotent: reposting the same id returns the
original event instead of recording a second one. Pass the id your application already has, and a retry
after a timeout can never double-count.

```java
client.track(TrackRequest.builder("purchase", "user_12345")
        .eventId("order-" + order.getId())
        .build());
```

When you omit it the SDK generates one, which is what makes its own retries safe.

### What comes back

```java
TrackResult result = client.track("user.signup", "user_12345");

result.accepted();  // true when Dregs recorded the event
result.id();        // the event's id
```

`accepted()` is `false` for the handful of rejections Dregs answers quietly rather than naming the check
that failed. Failures that are yours to act on throw instead — see [Errors](#errors).

## Reading scores

```java
Scores scores = client.identities().scores("user_12345");

scores.humanity();      // 85
scores.authenticity();  // 72
scores.uniqueness();    // 91
scores.behavior();      // 68
```

This is the cheap read and the one most integrations want. A category Dregs has not scored yet reads as
`null`, and a brand-new identity comes back empty. `Scores` is also `Iterable<Score>`, so you can loop
over the ones that exist.

Scoring is **asynchronous**. Scores appear moments after the events that move them, not in the same
breath, so read them at a decision point rather than immediately after a `track()` call.

```java
Integer authenticity = scores.authenticity();

if (authenticity != null && authenticity < 40) {
    holdForReview("user_12345");
}
```

### Seeing exactly why

The scores are the summary; the observations are the evidence. When you need to show or log *why* an
identity scored the way it did, ask for the analysis.

```java
Analysis analysis = client.identities().analysis("user_12345");

for (Observation observation : analysis.observations()) {
    log.info("{}: {} (value {})",
            observation.label(), observation.explanation(), observation.value());
}
```

Each observation carries the analyzer that produced it, a `value` from 0.0 (suspicious) to 1.0
(legitimate), a `confidence`, a `weight`, and the counts behind the finding in `metadata()`.
`analysis()` throws `NotFoundException` until the identity has been analyzed at least once.

### The whole identity

```java
Identity identity = client.identities().get("user_12345");

identity.displayEmail();   // "ada@example.com"
identity.humanityScore();  // 85
identity.badges();         // [Badge[name=Account Takeover Suspected, ...]]
identity.data();           // every attribute you have sent
```

### Forcing a rescore

```java
client.identities().analyze("user_12345");
```

This queues the work and returns; it does not wait for the cycle to finish. Dregs rescores on its own
as events arrive, so you rarely need this outside of a support or backfill flow.

## Errors

Every exception this library throws derives from `DregsException`, and all of them are unchecked: a
fraud score is an input to a decision rather than a step that must succeed, so where the failure is
worth handling is yours to decide.

```java
try {
    client.track("user.signup", "user_12345");
} catch (QuotaExceededException exceeded) {
    // over the monthly event limit; the event was not queued
} catch (RateLimitException limited) {
    // ingesting too fast; limited.retryAfter() when the server said how long
} catch (DregsException failed) {
    // anything else this library throws
}
```

| Exception | When |
| --- | --- |
| `BadRequestException` | 400, the event was malformed |
| `AuthenticationException` | 401, the secret key was not recognized |
| `QuotaExceededException` | 402, the account is over its monthly event limit |
| `PermissionDeniedException` | 403, the credential may not do this |
| `NotFoundException` | 404, no such identity, or it has not been analyzed |
| `RateLimitException` | 429, too many requests |
| `ServerException` | 5xx |
| `DregsTimeoutException` | the request timed out |
| `DregsConnectionException` | the request never reached Dregs |

Those that reached the API also carry `statusCode()`, `body()`, and `requestId()`. Quote the request id
when you report a problem: it is what lets support find the request in the logs.

### Retries

Connection failures, timeouts, 429s, and 5xx are retried automatically with exponential backoff and
jitter, honouring `Retry-After` when the server sends one. Two retries by default:

```java
Dregs client = Dregs.builder().maxRetries(5).build();  // or 0 to handle it yourself
```

## Async

Every method has an async twin with the same name, returning a `CompletableFuture`.

```java
AsyncDregs client = AsyncDregs.builder().build();

client.track("user.signup", "user_12345")
        .thenCompose(result -> client.identities().scores("user_12345"))
        .thenAccept(scores -> log.info("humanity {}", scores.humanity()));
```

Failures arrive the way `CompletableFuture` delivers every failure: the future completes exceptionally,
and `join()` wraps the cause in a `CompletionException`. The cause is the same exception the synchronous
client would have thrown. Retries wait on the JDK's delayed executor rather than by blocking a thread.

## Webhooks

Dregs signs every webhook with the channel's signing secret. Verify it against the **raw request body**
before acting on the payload — a re-serialized object will not match, because key order and whitespace
change.

```java
@PostMapping("/webhooks/dregs")
ResponseEntity<Void> receive(
        @RequestBody byte[] body,
        @RequestHeader(Webhooks.SIGNATURE_HEADER) String signature) {

    Map<String, Object> event;

    try {
        event = Webhooks.verify(body, signature, System.getenv("DREGS_WEBHOOK_SECRET"));
    } catch (WebhookVerificationException rejected) {
        return ResponseEntity.badRequest().build();
    }

    handle(event);

    return ResponseEntity.ok().build();
}
```

In Spring MVC that means a `byte[]` or `String` request body rather than a mapped type; in a servlet,
the raw input stream, read once. `verify` also rejects payloads older than five minutes as replays; pass
a `null` tolerance to skip that if you are deduplicating on the event id yourself. The signing secret is
shown once, when you create the webhook channel, and is not your API secret key.

## Configuration

```java
Dregs client = Dregs.builder()
        .secretKey(null)                     // defaults to $DREGS_SECRET_KEY
        .baseUrl(null)                       // defaults to $DREGS_BASE_URL, then https://dregs.com/api
        .timeout(Duration.ofSeconds(10))
        .maxRetries(2)
        .httpClient(null)                    // bring your own HttpClient for proxies or custom TLS
        .build();
```

An `HttpClient` you supply is yours to close; the one the SDK builds is released by `close()`, which on
Java 17 is a no-op because `HttpClient` only became closeable in Java 21.

## Types and forward compatibility

Responses are records with accessors rather than maps, so an IDE can complete them and the compiler can
check them. Parsing is deliberately lenient: a missing field is `null`, a score category this release
predates does not break the response around it, and every model keeps the body it was built from in
`raw()`, so a field Dregs adds after this release is reachable without waiting for an SDK upgrade.

Gson stays an implementation detail. No type it defines appears in a public signature, so the SDK will
not argue with whatever JSON library your application has already chosen.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). The short version:

```bash
./gradlew build      # compile, lint, javadoc, and test
./gradlew test       # tests alone
./gradlew javadoc    # the documentation check
```

The Gradle wrapper pins the build, `gradle.lockfile` pins the dependency versions, and
`gradle/verification-metadata.xml` pins their checksums, so the same commands produce the same
artifacts locally and in CI.

## Links

- [Dregs manual](https://dregs.com/manual/) and [REST API reference](https://dregs.com/manual/api/)
- [Dregs MCP server](https://github.com/dregs-sdk/dregs-mcp), for connecting AI agents to your data
- [Security policy](SECURITY.md)

## License

MIT. See [LICENSE](LICENSE).
