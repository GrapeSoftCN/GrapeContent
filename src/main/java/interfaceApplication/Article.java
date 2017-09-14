package interfaceApplication;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import apps.appsProxy;
import model.ContentModel;
import string.StringHelper;

public class Article {
	/**
	 * 封装新闻数据
	 * @project	GrapeContent
	 * @package interfaceApplication
	 * @file Test.java
	 * 
	 * @param articleInfo   新闻信息
	 * @param currentId  当前网站id
	 * 
	 * @return
	 *
	 */
	public String SetArticleInfo(String articleInfo,String currentId) {
		String wbids, wbid = "", ogid = "";
		JSONArray array = null;
		JSONObject Info = JSONObject.toJSON(articleInfo);
		if (Info != null && Info.size() != 0) {
			if (Info.containsKey("wbid")) {
				wbid = Info.getString("wbid");
				// 获取所有下级站点
				if (wbid.equals(currentId)) {
					wbids = appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebTree/" + wbid, null, null).toString();
					if (!wbids.equals("")) {
						wbid = wbids;
					}
				}
			}
			if (Info.containsKey("ogid")) {
				ogid = Info.getString("ogid");
			}
			array = setInfo(ogid, wbid, Info);
		}
		return (array != null && array.size() != 0) ? array.toJSONString() : "";
	}

	/**
	 * 封装内容数据[{"wbid":"","ogid":"","mainName":""}]
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file Test.java
	 * 
	 * @param ogid
	 * @param wbids
	 * @param ArticleInfo
	 * @return jsonarray
	 *
	 */
	@SuppressWarnings("unchecked")
	private JSONArray setInfo(String ogid, String wbids, JSONObject ArticleInfo) {
		JSONObject webObj = getColumn(wbids, ogid);
		String[] value = null;
		JSONObject Articles = null;
		JSONArray array = new JSONArray();
		String temp = "";
		if (ArticleInfo != null && ArticleInfo.size() != 0) {
			value = wbids.split(",");
			for (String wbid : value) {
				if (webObj != null && webObj.size() != 0) {
					Articles = new JSONObject();
					Articles = ArticleInfo;
					Articles.put("wbid", wbid);
					Articles.put("ogid", webObj.getString(wbid));
					temp +=Articles.toJSONString()+";";
				}
			}
		}
		
		return String2Array(temp);
	}

	@SuppressWarnings("unchecked")
	private JSONArray String2Array(String temp){
		JSONObject tempobj = null;
		String[] value = null;
		JSONArray array = new JSONArray();
		if (temp!=null && !temp.equals("")) {
			temp = StringHelper.fixString(temp, ';');
			if (temp!=null && !temp.equals("")) {
				value = temp.split(";");
				for (String string : value) {
					tempobj = JSONObject.toJSON(string);
					if (tempobj!=null) {
						array.add(tempobj);
					}
				}
			}
		}
		return array;
	}
	/***
	 * 获取栏目id，封装成{wbid:ogid,wbid:ogid...}
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file Test.java
	 * 
	 * @param wbid
	 * @param ogid
	 * @return
	 *
	 */
	private JSONObject getColumn(String wbid, String ogid) {
		String name = "", temp;
		JSONObject rObject = null;
		if (ogid != null && !ogid.equals("") && wbid != null && !wbid.equals("")) {
			String column = appsProxy.proxyCall("/GrapeContent/ContentGroup/getGroupById/" + ogid, null, null)
					.toString();
			JSONObject columnInfo = JSONObject.toJSON(column);
			column = JSONObject.toJSON(columnInfo.getString("message")).getString("records");
			columnInfo = JSONObject.toJSON(column);
			if (columnInfo != null && columnInfo.size() != 0) {
				name = columnInfo.getString("name");
			}
			temp = appsProxy.proxyCall("/GrapeContent/ContentGroup/getColumnInfo/" + wbid + "/" + name, null, null)
					.toString();
			rObject = JSONObject.toJSON(temp);
		}
		return (rObject != null && rObject.size() != 0) ? rObject : null;
	}
}
