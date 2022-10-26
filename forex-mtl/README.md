# Overview

The Forex proxy provides currency exchange rates via OneFrame Rates API, bypassing its strict API call limits.

# Running Locally

A Docker Compose manifest is provided to easily bring up both Forex proxy and local OneFrame service.

1. Ensure that `docker` client is available.
2. Start the service:
   ```shell
   docker compose up --build
   ```
3. The proxy is now available at `localhost:8080`:
   ```shell
   curl http://localhost:8080/rates?from=USD&to=JPY
   ```

# Considerations

Given the following requirements:
* No more than 1000 requests per day to OneFrame API per token;
* Forex proxy should be able to serve 10'000 requests per day;
* Returned currency exchange rates should not be older than 5 minutes;

And following assumptions:
* OneFrame Rates API does not limit the number of requested rates, only capped by all possible ISO 4217 currency pair combinations;
* There are only 9 supported currencies (AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, USD) by Get Rates API.

Then the requirements can be fulfilled by querying and caching all currency combination pairs' rates every 5 minutes.

## Feasibility

In this case, there will be `(24 * 60 / 5) = 288` requests made per day by each Forex proxy instance to OneFrame API.
Any Get Rates API request will be served this cached data, so an arbitrary number of requests can be served every day.

There are `9*(9-1) = 72` currency pair combinations.
Each requested pair is passed to OneFrame as a query parameter in `[?&]pair=XXXYYY` format, taking 12 octets each,
and `72 * 12 = 864` in total, which is far less than URL length limit of `8000` octets recommended by HTTP specification in RFC 7230.

## Implementation

### Cache

`scalacache` library was used for caching, with `Caffeine` implementation, an in-memory cache.
Such cache is easier to set up, manage and reason about, compared to an external one (e.g. Redis).

However, this means that exchange rates cache is not shared between proxy instances.
If scalability is required, then external distributed cache must be used --
currently only 3 proxy instances can be run in parallel, consuming at least `864` OneFrame API requests per day.

Additionally, if too many restarts are triggered, then API call limit might also be depleted.

Refresh period is set to a bit lower duration (4:50) than desired TTL to compensate for possible network delays.

All cached rates have TTL expiry of 5 minutes, in order not to serve stale data and to fulfill the requirements.

### Error Handling

In current implementation, Forex proxy Rates requests have only one observable failure mode (rate cache miss, or "no exchange rate"),
as all requests to OneFrame API are done in a separate, independent context.

Observability could be improved if Rates API returned last encountered error,
though it is up to debate whether downstream clients should be aware of OneFrame quotas and its existence in general.
For example, signalling that some cached currency rate has expired is not very useful.

After all, it is probably best to leave mitigations to these problems to proxy itself, as it was created specifically to circumvent them. 
And if Forex proxy is deemed a standalone currency exchange service, then OneFrame is merely an opaque upstream provider.

### Response Timezone

Currently, rate response reuses timezone provided by OneFrame service.
It is not much of a concern, since timezone is still provided in timestamps.
But it can be explicitly defined (e.g. UTC or JST), as otherwise it might be suddenly changed by upstream,
which might become a surprise to downstream clients.
