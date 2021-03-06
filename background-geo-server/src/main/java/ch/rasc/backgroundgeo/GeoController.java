package ch.rasc.backgroundgeo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ch.rasc.backgroundgeo.eventbus.EventBusEvent;
import ch.rasc.backgroundgeo.eventbus.SubscribeEvent;

@RestController
public class GeoController {

	private final ApplicationEventPublisher publisher;
	private final List<Position> positions;
	private final List<Stationary> stationaries;

	public GeoController(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
		this.positions = new ArrayList<>();
		this.stationaries = new ArrayList<>();
	}

	@EventListener
	public void onSubscribe(SubscribeEvent event) {
		if (!this.positions.isEmpty()) {
			this.publisher.publishEvent(
					EventBusEvent.of(event.clientId(), "pos", this.positions));
		}

		if (!this.stationaries.isEmpty()) {
			this.publisher.publishEvent(
					EventBusEvent.of(event.clientId(), "stationary", this.stationaries));
		}
	}

	@DeleteMapping(path = "/clear")
	public void clear() {
		this.stationaries.clear();
		this.positions.clear();
		this.publisher.publishEvent(EventBusEvent.of("clear"));
	}

	@PostMapping(path = "/pos")
	public void handleLocation(@RequestBody Position position) {
		this.publisher
				.publishEvent(EventBusEvent.of("pos", Collections.singleton(position)));

		this.positions.add(position);
		if (this.positions.size() > 100) {
			this.positions.remove(0);
		}
	}

	@PostMapping(path = "/stationary")
	public void handleStationary(@RequestBody Stationary stationary) {
		this.publisher.publishEvent(
				EventBusEvent.of("stationary", Collections.singleton(stationary)));

		this.stationaries.add(stationary);
		if (this.stationaries.size() > 10) {
			this.stationaries.remove(0);
		}
	}

	@PostMapping(path = "/clienterror")
	public void handleError(String errorMessage) {
		Application.logger.error(errorMessage);
	}

}
