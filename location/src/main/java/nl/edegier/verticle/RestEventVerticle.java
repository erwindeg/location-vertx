package nl.edegier.verticle;

import java.util.Map.Entry;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class RestEventVerticle extends AbstractVerticle {

	Router router;

	@Override
	public void start() throws Exception {
		router = Router.router(vertx);
		vertx.createHttpServer().requestHandler(router::accept).listen(8080);
		vertx.eventBus().consumer("api-gateway", this::changeServiceRegistration);		
	}

	private void changeServiceRegistration(Message<JsonObject> message) {
		JsonObject change = message.body();
		String type = change.getString("type");
		if ("SUBSCRIBE".equals(type)) {
			String path = change.getString("path");
			router.route(path).handler(this::mapRequest);
		} else if ("UNSUBSCRIBE".equals(type)) {
			String path = change.getString("path");
			router.getRoutes().forEach(route -> {
				if(route.getPath().equals(path)){
					route.remove();
				}
			});
		}
	}

	private void mapRequest(RoutingContext context) {
		context.request().bodyHandler(buffer -> {
			JsonObject event = new JsonObject();
			event.put("path", context.request().path());
			String data = buffer.getString(0, buffer.length());
			event.put("data", data);
			event.put("method", context.request().method().toString());
			addParams(event, context.request().params());
			String address = context.request().path().split("/")[1];
			vertx.eventBus().send(address, event, message -> {
				if (message.succeeded()) {
					context.response().end(message.result().body().toString());
				} else {
					System.out.println(message.cause());
					context.response().setStatusCode(503).end();
				}
			});
		});

	}

	private void addParams(JsonObject event, MultiMap params) {
		JsonObject parameters = new JsonObject();
		event.put("params", parameters);
		for (Entry<String, String> entry : params.entries()) {
			System.out.println(entry.toString());
			parameters.put(entry.getKey(), entry.getValue());
		}
	}
}
