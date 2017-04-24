package test;

import interfaceApplication.Content;

public class test {
//	private Content content = new Content();
	public static void main(String[] args) {
		Content content = new Content();
		long start = System.currentTimeMillis();
		for (int i = 0; i < 500; i++) {
			System.out.println(content.ShowByGroupId("58f62d741a4769cbf53907b6"));
		}
//		System.out.println(content.ShowByGroupId("58f62d741a4769cbf53907b6"));
		long end = System.currentTimeMillis();
		System.out.println(end-start);
	}
}
