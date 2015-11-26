package nl.edegier.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.ext.mongo.MongoClient;

/**
 * db.location.ensureIndex({loc: "2d"},{min: -500, max: 500, w:1});
 * 
 * @author giererwi
 *
 */
public class LocationImporterVerticle extends AbstractVerticle {

	MongoClient mongo;
	final int max = 400;
	int requests = 0;
	AsyncFile file;
	private static final String FILE_NAME = "postcode_NH.csv";
	boolean isReading;

	@Override
	public void start() throws Exception {
		this.mongo = MongoClient.createShared(vertx, new JsonObject());
		this.mongo.dropCollection("location", result -> System.out.println(result.succeeded()));
		this.readFile();
	}

	private void readFile() {
		this.isReading = true;
		final RecordParser parser = RecordParser.newDelimited("\n", location -> {
			this.saveLocation(location.getString(0, location.length()));
		});
		FileSystem fs = vertx.fileSystem();
		fs.open(FILE_NAME, new OpenOptions(), contents -> {
			if (contents.succeeded()) {
				this.file = contents.result();
				this.file.handler(buffer -> {
					parser.handle(buffer.copy());
				});

				this.file.endHandler(result -> {
					System.out.println("Done");
					this.isReading = false;
				});

			} else {
				System.out.println(contents.cause());
			}
		});
	}

	private void saveLocation(String location) {

		this.requests++;
		if (this.requests >= this.max) {
			if (this.isReading) {
				this.file.pause();
			}
		}
		String[] fields = location.split(";");
		String zipcode = fields[1];
		String lat = fields[15];
		String lon = fields[16];
		JsonObject jsonLocation = new JsonObject();
		jsonLocation.put("zipcode", zipcode);
		JsonArray coord = new JsonArray();
		coord.add(Double.valueOf(lat));
		coord.add(Double.valueOf(lon));
		jsonLocation.put("loc", coord);
		this.mongo.insert("location", jsonLocation, result -> {
			boolean closed = false;
			this.requests--;
			if (this.requests < this.max) {
				if (this.isReading) {
					this.file.resume();
				} else {
					closed = true;
					if (!closed) {
						this.file.close();
					}

				}

			}
			if (result.failed()) {
				System.out.println(result.cause());
			}
		});
	}
}
