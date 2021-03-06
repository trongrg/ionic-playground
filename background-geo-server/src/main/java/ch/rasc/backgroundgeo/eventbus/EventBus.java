package ch.rasc.backgroundgeo.eventbus;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.rasc.backgroundgeo.Application;

@Component
public class EventBus {

	private final Map<String, EventBusClient> clients;

	private final ObjectMapper objectMapper;

	private final ApplicationEventPublisher publisher;

	@Autowired
	public EventBus(ObjectMapper objectMapper, ApplicationEventPublisher publisher) {
		this.clients = new ConcurrentHashMap<>();
		this.objectMapper = objectMapper;
		this.publisher = publisher;
	}

	public void subscribe(EventBusClient client) {
		this.clients.put(client.id(), client);

		this.publisher.publishEvent(SubscribeEvent.of(client.id()));
	}

	public void unsubscribe(String clientId) {
		this.clients.remove(clientId);
	}

	@EventListener
	public void handleEvent(EventBusEvent event) {
		if (this.clients.isEmpty()) {
			return;
		}

		String data = null;
		try {
			data = this.objectMapper.writeValueAsString(event.data());
		}
		catch (JsonProcessingException e) {
			Application.logger.error("event bus publish", e);
			return;
		}

		final String json = data;

		if (event.clientId() != null) {
			EventBusClient client = this.clients.get(event.clientId());
			if (client != null) {
				try {
					client.emitter()
							.send(SseEmitter.event().name(event.name()).data(json));
				}
				catch (Exception e) {
					client.emitter().completeWithError(e);
					this.clients.remove(event.clientId());
				}
			}
		}
		else {
			Set<String> failedClients = new HashSet<>();

			this.clients.forEach((clientId, client) -> {
				if (client != null) {
					try {
						client.emitter()
								.send(SseEmitter.event().name(event.name()).data(json));
					}
					catch (Exception e) {
						client.emitter().completeWithError(e);
						failedClients.add(client.id());
					}
				}
				else {
					failedClients.add(clientId);
				}
			});

			failedClients.forEach(this.clients::remove);
		}
	}

}
