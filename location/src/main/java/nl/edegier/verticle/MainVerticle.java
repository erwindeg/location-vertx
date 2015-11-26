package nl.edegier.verticle;

import io.vertx.core.AbstractVerticle;

public class MainVerticle extends AbstractVerticle {
	@Override
	public void start() throws Exception {
		vertx.deployVerticle(new LocationVerticle());
		vertx.deployVerticle(new RestEventVerticle());
	}
}
