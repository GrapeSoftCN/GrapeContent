package test;


import httpServer.booter;
import interfaceApplication.ContentGroup;

public class test {
	public static void main(String[] args) {
		booter booter = new booter();
		try {
			System.out.println("GrapeContent!");
			System.setProperty("AppName", "GrapeContent");
			booter.start(1003);
		} catch (Exception e) {
		}
//		System.out.println(new ContentGroup().);
	}
}
