package com.example.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventPublisher {
  private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
  private final KafkaTemplate<String, String> kafka;
  public EventPublisher(KafkaTemplate<String, String> kafka) { this.kafka = kafka; }
  public void publish(String topic, String key, String json) {
    try { kafka.send(topic, key, json); }
    catch (Exception e) { log.warn("Kafka publish skipped/fail topic={} key={} error={}", topic, key, e.getMessage()); }
  }
}
