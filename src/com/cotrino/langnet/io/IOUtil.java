package com.cotrino.langnet.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class IOUtil {

	public static InputStream getInputStream(String path) throws IOException {
		File initialFile = new File(path);
		return FileUtils.openInputStream(initialFile);
	}

	public static List<String> getLines(String path) throws IOException {
		return Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
	}
	
	public static String read(String path) throws IOException {
		String content = "";
		List<String> lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
		for(String line : lines) {
			content += line + "\n";
		}
		return content;
	}
	
	public static void write(String filename, String content) {
		try {
			FileWriter fstream = new FileWriter(filename);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(content);
			out.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static boolean fileExists(String filename) {
		File file = new File(filename);
		return file.exists();
	}
	
	public static String getUserDirectory() {
		return System.getProperty("user.home");
	}
	
	public static String getWorkingDirectory() {
		return System.getProperty("user.dir");
	}
}
