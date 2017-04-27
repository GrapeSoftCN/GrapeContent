package test;

import httpServer.booter;

public class test {
//	private Content content = new Content();
	public static void main(String[] args) {
		booter booter = new booter();
		try {
			System.out.println("GrapeContent!");
			System.setProperty("AppName", "GrapeContent");
			booter.start(1003);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
