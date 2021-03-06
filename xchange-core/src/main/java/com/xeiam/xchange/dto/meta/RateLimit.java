package com.xeiam.xchange.dto.meta;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Describe a call rate limit as a number of calls per some time span.
 */
public class RateLimit {

  @JsonProperty
  public int calls = 1;

  @JsonProperty
  @JsonDeserialize(using = TimeUnitDeserializer.class)
  public TimeUnit timeUnit = TimeUnit.SECONDS;

  @JsonProperty
  public int timeSpan = 1;

  /**
   * Constructor
   */
  public RateLimit() {
  }

  public RateLimit(int calls, int timeSpan, TimeUnit timeUnit) {
    this.calls = calls;
    this.timeUnit = timeUnit;
    this.timeSpan = timeSpan;
  }

  /**
   * @return this rate limit as a number of milliseconds required between any two remote calls, assuming the client makes consecutive calls without
   *         any bursts or breaks for an infinite period of time.
   */
  @JsonIgnore
  public long getPollDelayMillis() {
    return timeUnit.toMillis(timeSpan) / calls;
  }

  public static class TimeUnitDeserializer extends JsonDeserializer<TimeUnit> {
    @Override
    public TimeUnit deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
      return TimeUnit.valueOf(jp.getValueAsString().toUpperCase());
    }
  }
}
