package test;


import interfaceApplication.Content;

public class test {
	public static void main(String[] args) {
//		booter booter = new booter();
//		try {
//			System.out.println("GrapeContent!");
//			System.setProperty("AppName", "GrapeContent");
//			booter.start(1003);
//		} catch (Exception e) {
//		}
		System.out.println(new Content().getImgs("58f75b0c1a4769cbf53bc310", 7));
	}
}
