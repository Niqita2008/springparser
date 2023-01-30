package com.vprave.niqitadev.parser.storage;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.text.SimpleDateFormat;

@Getter
@ConfigurationProperties("storage")
public class StorageProperties {
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd--HH:mm:ss.SSS");
	private Path location = Path.of("upload");

	public void setLocation(String location) {
		this.location = Path.of(location);
	}
}
