package nl.edegier.verticle;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;


@RunWith(VertxUnitRunner.class)
public class MongoVerticleTest {

	private static Vertx vertx;

	@BeforeClass
	public static void setUp(TestContext context) {
		Async async = context.async();
		vertx = Vertx.vertx();
		vertx.deployVerticle(LocationVerticle.class.getName(), ar -> {
			if (ar.succeeded()) {
				//setup test data
				vertx.eventBus().send("message.insert", "testmessage", result -> {
					if(result.succeeded()){
						async.complete();
					} else {
						context.fail();
					}
				});
			} else {
				context.fail("Could not deploy verticle");
			}
		});
	}

	@Test
	public void testInsert(TestContext context) {
		Async async = context.async();
		vertx.eventBus().send("message.insert", "hello", result -> {
			if(result.succeeded()){
				async.complete();
			} else {
				context.fail();
			}
		});
	}

	@Test
	public void testFind(TestContext context) {
		Async async = context.async();
		vertx.eventBus().send("message.find", "testmessage", result -> {
			if(result.succeeded()){
				JsonArray messages = (JsonArray)result.result().body();
				System.out.println(messages.toString());
				if(messages.size() == 0){
					context.fail("result must not be 0");
				}
				
				async.complete();
			} else {
				context.fail();
			}
		});
	}
	
	@Test
	public void testFindNoResult(TestContext context) {
		Async async = context.async();
		vertx.eventBus().send("message.find", "not existing", result -> {
			if(result.succeeded()){
				JsonArray messages = (JsonArray)result.result().body();
				System.out.println(messages.toString());
				if(messages.size() == 0){
					async.complete();
				} else{
					context.fail("result must be 0");
				}
			} else {
				context.fail();
			}
		});
	}
	
	@Test
	public void testRemove(TestContext context) {
		Async async = context.async();
		vertx.eventBus().send("message.remove", "hello", result -> {
			if(result.succeeded()){
				async.complete();
			} else {
				context.fail();
			}
		});
	}
	
	@AfterClass
	public static void tearDown(TestContext context) {
		Async async = context.async();
		vertx.close(ar -> {
			async.complete();
		});
	}
}
