package dev.eyadsharkawy.agency_os_api.shared.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketBroadcastService {

  private final SimpMessagingTemplate messagingTemplate;

  @Async
  public void broadcast(String destination, Object payload) {
    log.debug("Asynchronously broadcasting message to destination: [{}]", destination);
    messagingTemplate.convertAndSend(destination, payload);
  }
}
