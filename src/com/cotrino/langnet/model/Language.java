package com.cotrino.langnet.model;

public class Language {

	long ID;
	String name;
	Dictionary dict;
	
	 public Language(Dictionary dict, long ID, String name) {
		 this.ID = ID;
		 this.name = name;
		 this.dict = dict;
	 }
	 
	 public long getId() {
		 return ID;
	 }
	 
	 public String toString() {
		 return name;
	 }
}
