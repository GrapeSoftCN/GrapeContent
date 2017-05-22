package test;

import httpServer.booter;
import interfaceApplication.Content;
import interfaceApplication.ContentGroup;
import model.ContentGroupModel;
import model.ContentModel;

public class test {
	public static void main(String[] args) {
		 booter booter = new booter();
		 try {
		 System.out.println("GrapeContent!");
		 System.setProperty("AppName", "GrapeContent");
		 booter.start(1003);
		} catch (Exception e) {
		}
		//// System.out.println(new
		// Content().getImgs("58f75b0c1a4769cbf53bc310", 7));
		// System.out.println(new
		// ContentGroup().GroupDelete("58f884671a4769cbf53e6aa5"));
//		System.out.println(new Content().EditArticle("58f8890cc6c2040bbccd692b",
//				contents));
		
//		System.out.println(new Content().ShowByGroupId("58f62de01a4769cbf53908ae"));
		
//		String content = "{\"content\":\"安全为本创新为先打造高效创新为先创新为先打造高效创新为先创新为先123\",\"subName\":null,\"mainName\":\"安全为本创新为先打造高效创新为先创新为先打造高效创新为先创新为先\",\"time\":\"1492568040000\",\"souce\":null,\"desp\":\"这是test\",\"attribute\":\"1\",\"image\":\"http://123.57.214.226:8080\\File\\upload\\2017-05-05\\tp03.jpg\"}";
//		System.out.println(new Content().EditArticle("590043231a4769cbf54dd211", content));
	}
}
