package model;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.json.simple.JSONObject;

import apps.appsProxy;
import database.db;
import nlogger.nlogger;

public class WsCount {
	private ContentModel content = new ContentModel();

	@SuppressWarnings("unchecked")
	public JSONObject getAllCount(JSONObject robj, String rootID, String rootName, String fatherID) {
		// appsProxy.
		String[] trees = null;
		JSONObject nObj = new JSONObject();
		String[] Allweb = getCid(rootID);
		long allCnt = getCount(Allweb);
		long argCnt = getAgreeCount(Allweb);
		long disArg = getDisagreeCount(Allweb);
		nObj.put("id", rootID);
		nObj.put("fatherid", fatherID);
		nObj.put("name", rootName);
		nObj.put("count", allCnt);
		nObj.put("checked", argCnt);
		nObj.put("uncheck", disArg);
		nObj.put("checking", allCnt - argCnt - disArg);
		String tree = (String) appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getChildrenweb/s:" + rootID, null, null);
		if (!tree.equals("")) {
			trees = tree.split(",");
		}
		JSONObject newJSON = new JSONObject();
		if (trees != null) {
			JSONObject webInfos = JSONObject
					.toJSON((String) appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebInfo/s:" + tree, null, null));
			int l = trees.length;
			for (int i = 0; i < l; i++) {
				// newJSON.put(trees[i], webInfos.getString(trees[i]) );//填充网站姓名
				getAllCount(newJSON, trees[i], webInfos.getString(trees[i]), rootID);
			}
		}
		nObj.put("children", newJSON);
		robj.put(rootID, nObj);
		return robj;
	}

	private long getCount(String[] wid) {
		long count = 0;
		if (wid != null) {
			db db = content.bind();
			db.or();
			for (String string : wid) {
				db.eq("wbid", string);
			}
			count = db.count();
		}
		return count;
	}

	private long getAgreeCount(String[] wid) {
		long count = 0;
		if (wid != null) {
			db db = content.bind();

			db.or();
			for (String string : wid) {
				db.eq("wbid", string);
			}
			db.and();
			count = db.eq("state", 2).count();
		}
		return count;
	}

	private long getDisagreeCount(String[] wid) {
		long count = 0;
		if (wid != null) {
			db db = content.bind();
			db.or();
			for (String string : wid) {
				db.eq("wbid", string);
			}
			db.and();
			count = db.eq("state", 1).count();
		}
		return count;
	}

	// 获取网站id，包含自身网站id
	private String[] getCid(String wid) {
		String[] trees = null;
		if (wid != null && !wid.equals("")) {
			// 获取子站点
			wid = (String) appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebTree/s:" + wid, null, null);
			if (!wid.equals("")) {
				trees = wid.split(",");
			}
		}
		return trees;
	}
}
