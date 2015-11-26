package nl.edegier.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class RestVerticle extends AbstractVerticle{

	
	@Override
	public void start() throws Exception {
		vertx.createHttpServer().requestHandler(getRouter()::accept).listen(9080);
		
	}
	
	private Router getRouter(){
		Router router = Router.router(vertx);
		router.get("/api/hello-world").handler(context -> context.response().end("{\"content\" : \"Hello world!\" }"));
		router.get("/location/:lat/:lon").handler(rc -> {
			
			String lat = rc.request().getParam("lat");
			String lon = rc.request().getParam("lon");
			vertx.eventBus().send("location", new JsonObject().put("lat",lat).put("lon",lon), result -> {
				if(result.succeeded()){
					rc.response().end(result.result().body().toString());
				} 
			});
//			rc.response().end(new JsonObject().put("lat",lat).put("lon",lon).toString());
		});
		
		return router;
	}
}
