package nl.edegier.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class LocationVerticle extends AbstractVerticle {

	MongoClient mongo;

	@Override
	public void start() throws Exception {
		this.mongo = MongoClient.createShared(vertx, new JsonObject());
		vertx.eventBus().consumer("location", this::findZipcode);

		vertx.eventBus().publish("api-gateway",
				new JsonObject().put("type", "SUBSCRIBE").put("path", "/location/:lat/:lon"));
	}

	@Override
	public void stop() throws Exception {
		vertx.eventBus().publish("api-gateway",
				new JsonObject().put("type", "UNSUBSCRIBE").put("path", "/location/:lat/:lon"));
	}

	private void findZipcode(Message<JsonObject> message) {
		JsonObject params = message.body().getJsonObject("params");

		JsonObject query = new JsonObject();
		JsonObject loc = new JsonObject();
		query.put("loc", loc);
		JsonArray coord = new JsonArray();
		Double lat = Double.valueOf(params.getString("lat"));
		Double lon = Double.valueOf(params.getString("lon"));
		coord.add(lat);
		coord.add(lon);
		loc.put("$near", coord);
		loc.put("$maxDistance", 1);

		this.mongo.find("location", query, result -> {
			if (result.succeeded() && !result.result().isEmpty()) {
				JsonObject nearest = result.result().get(0);
				message.reply(
						new JsonObject().put("zipcode", nearest.getString("zipcode")).put("lat", lat).put("lon", lon));
			} else {
				message.reply(new JsonObject());
			}
		});
	}

}
