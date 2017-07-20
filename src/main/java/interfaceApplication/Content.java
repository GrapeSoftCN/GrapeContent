package interfaceApplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import apps.appsProxy;
import json.JSONHelper;
import model.ContentModel;
import nlogger.nlogger;
import rpc.execRequest;
import security.codec;
import session.session;
import string.StringHelper;
import thirdsdk.kuweiCheck;
import time.TimeHelper;

@SuppressWarnings("unchecked")
public class Content {
	private ContentModel content = new ContentModel();
	private HashMap<String, Object> defmap = new HashMap<>();
	private static session session;
	private JSONObject UserInfo = new JSONObject();
	private String sid = null;

	static {
		session = new session();
	}

	public Content() {
		sid = (String) execRequest.getChannelValue("sid");
		if (sid != null) {
			UserInfo = session.getSession(sid);
		}
		defmap.put("subName", null);
		defmap.put("image", "");
		defmap.put("ownid", appsProxy.appid());
		defmap.put("manageid", "");
		defmap.put("content", "");
		defmap.put("fatherid", "0");
		defmap.put("author", 0);
		defmap.put("attribute", 0);
		defmap.put("ogid", 0);
		defmap.put("attrid", 0);
		defmap.put("sort", 0);
		defmap.put("isdelete", 0);
		defmap.put("isvisble", 0);
		defmap.put("souce", null);
		defmap.put("state", 0);
		defmap.put("substate", 0);
		defmap.put("slevel", 0);
		defmap.put("readCount", 0);
		defmap.put("thirdsdkid", "");
		defmap.put("tempid", 0);
		defmap.put("wbid", (UserInfo != null && UserInfo.size() != 0) ? UserInfo.get("currentWeb").toString() : "");
		defmap.put("u", 2000);
		defmap.put("r", 1000);
		defmap.put("d", 3000);
		defmap.put("time", TimeHelper.nowMillis());
		defmap.put("reason", ""); // 文章未通过审核，填写理由，【未通过审核，该项为必填项】
		defmap.put("reasonRead", TimeHelper.nowMillis()); // 未通过审核
															// 理由是否已读，0：未读；1：已读
	}

	private String getImageUri(String imageURL) {
		if (imageURL.contains("http://")) {
			int i = imageURL.toLowerCase().indexOf("/file/upload");
			imageURL = imageURL.substring(i);
		}
		return imageURL;
	}

	/**
	 * 批量添加文章
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file Content.java
	 * 
	 * @param ArticleInfo
	 *            参数格式为[{文章数据1},{文章数据2},...]
	 * @return
	 *
	 */
	public String AddAllArticle(String ArticleInfo) {
		String tip = content.resultMessage(99);
		JSONObject object;
		String info;
		List<String> list = new ArrayList<>();
		long time = 0;
		int code = 99;
		JSONArray array = JSONHelper.string2array(ArticleInfo);
		if (array != null && array.size() != 0) {
			for (Object obj : array) {
				object = (JSONObject) obj;
				if (object.containsKey("time")) {
					time = Long.parseLong(object.getString("time"));
					object.put("time", time);
				}
				info = content.AddAll(content.AddMap(defmap, object));
				if (info == null) {
					if (list.size() != 0) {
						BatchDelete(StringHelper.join(list));
					}
					code = 99;
					break;
				}
				code = 0;
				list.add(info);
			}
			if (code == 0) {
				tip = content.resultMessage(content.batch(list));
			} else {
				tip = content.resultMessage(99);
			}
		}
		return tip;
	}

	/**
	 * 错误字识别
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file Content.java
	 * 
	 * @param contents
	 * @return
	 *
	 */
	public String Typos(String contents) {
		// String kuweiUserId = appsProxy
		// .proxyCall(content.getHost(0), appsProxy.appid() +
		// "/30/Wechat/getPUser/10", null, null).toString();
		// JSONObject obj = JSONHelper.string2json(kuweiUserId);
		// if (obj != null) {
		// obj = JSONHelper.string2json(obj.getString("message"));
		// }
		contents = codec.DecodeHtmlTag(contents);
		// kuweiCheck check = new kuweiCheck(obj.getString("userid"));
		kuweiCheck check = new kuweiCheck("377c9dc160bff6cfa3cc0cbc749bb11a");
		contents = codec.decodebase64(contents);
		contents = check.checkContent(contents);
		return content.resultMessage(JSONHelper.string2json(contents));
	}

	/**
	 * 修改文章
	 * 
	 * @param jsonstring
	 * @return
	 */
	public String EditArticle(String oid, String contents) {
		int code = 99;
		JSONObject infos = JSONHelper.string2json(contents);
		if (UserInfo != null && UserInfo.size() != 0) {
			String wbid = UserInfo.get("currentWeb").toString();
			if (infos != null) {
				try {
					String value = infos.get("content").toString();
					value = codec.DecodeHtmlTag(value);
					value = codec.decodebase64(value);
					infos.escapeHtmlPut("content", value);
					if (infos.containsKey("image")) {
						String image = infos.get("image").toString();
						image = getImageUri(codec.DecodeHtmlTag(image));
						if (image != null) {
							infos.put("image", image);
						}
					}
					if (infos.containsKey("time")) {
						infos.put("time", Long.parseLong(infos.getString("time")));
					}
				} catch (Exception e) {
					nlogger.logout(e);
				}
			}
			code = content.UpdateArticle(wbid,oid, infos);
		}
		return content.resultMessage(code, "文章更新成功");
	}

	/**
	 * 删除文章
	 * 
	 * @param oid
	 *            唯一识别符
	 * @return
	 */
	public String DeleteArticle(String oid) {
		int code = 99;
		if (UserInfo!=null|| UserInfo.size()!=0) {
			String wbid = UserInfo.get("currentWeb").toString();
			code = content.DeleteArticle(wbid,oid);
		}
		return content.resultMessage(code, "文章删除成功");
	}

	/**
	 * 获取最新的一篇文章信息 排序条件(时间优先，_id次之)
	 * 
	 * @return
	 */
	public String FindNewArc() {
		return content.findnew();
	}

	/**
	 * 获取某个栏目下最新的一篇文章信息 排序条件(时间优先，_id次之)
	 * 
	 * @return
	 */
	public String FindNewByOgid(String ogid) {
		return content.findnew(ogid);
	}

	/**
	 * 获取最新公开的指定数量的信息
	 * @project	GrapeContent
	 * @package interfaceApplication
	 * @file Content.java
	 * 
	 * @param size
	 * @return
	 *
	 */
	public String FindOpen(int size) {
		return content.findnew(size);
	}

	/**
	 * 根据oid显示文章 同时显示上一篇文章id，名称 下一篇文章id，名称,显示文章所有数据信息
	 * 
	 * @return
	 */
	/*public String findArticle(String oid) {
		JSONObject object = content.select(oid);
		return content.resultMessage(object);
	}*/
	public String findArticle(String oid) {
		JSONObject object = content.find(oid);
		return content.resultMessage(object);
	}

	/**
	 * 设置内容组
	 * 
	 * @param oid
	 * @param ogid
	 * @return
	 */
	public String SetGroup(String oid, String ogid) {
		return content.resultMessage(content.setGroup(oid, ogid), "设置内容组成功");
	}

	// 批量设置内容组
	public String SetGroupBatch(String ogid) {
		return content.resultMessage(content.setGroup(ogid), "设置内容组成功");
	}

	/**
	 * 根据用户查找文章
	 * 
	 * @param uid
	 * @return
	 */
	public String findbyUser(String uid) {
		return content.resultMessage(content.searchByUid(uid));
	}

	/*----------------前台搜索-----------------*/
	/**
	 * 文章模糊查询
	 * 
	 * @param jsonstring
	 * @return
	 */
	public String SearchArticle(String wbid,int ids,int pageSize,String condString) {
		JSONObject object = content.search(wbid,ids,pageSize,condString);
		return content.resultMessage(object);
	}

	/*public String SearchArticles(String wbid,String condString, int no) {
		JSONArray array = content.search(wbid,JSONHelper.string2json(condString), no);
		return content.resultMessage(array);
	}*/

	/*----------------后台搜索-----------------*/
	/**
	 * 文章模糊查询
	 * 
	 * @param jsonstring
	 * @return
	 */
	public String SearchArticleBack(int ids,int pageSize,String condString) {
		JSONObject object = null;
		if (UserInfo!=null|| UserInfo.size()!=0) {
			String wbid = UserInfo.get("currentWeb").toString();
			object = content.searchBack(wbid,ids,pageSize,condString);
		}
		return content.resultMessage(object);
	}

	/**
	 * 分页[前台]
	 * 
	 * @param idx
	 *            当前页
	 * @param pageSize
	 *            每页显示量
	 * @return
	 */
	public String Page(String wbid,int idx, int pageSize) {
		if (UserInfo != null && UserInfo.size() != 0) {
			wbid = UserInfo.get("currentWeb").toString();
		}
		JSONObject object = content.page(wbid,idx, pageSize);
		return content.resultMessage(object);
	}

	public String PageBySid(int idx, int pageSize, String contents) {
		JSONObject object = content.page2(idx, pageSize, JSONHelper.string2json(contents));
		return content.resultMessage(object);
	}

	public String PageBy(String wbid,int idx, int pageSize, String contents) {
		if (UserInfo != null && UserInfo.size() != 0) {
			wbid = UserInfo.get("currentWeb").toString();
		}
		JSONObject object = content.page(wbid,idx, pageSize, JSONHelper.string2json(contents));
		return content.resultMessage(object);
	}

	public String PageBack(int idx, int pageSize) {
		return content.resultMessage(content.pageBack(idx, pageSize));
	}

	public String PageByBack(int idx, int pageSize, String contents) {
		JSONObject object = content.pageBack(idx, pageSize, JSONHelper.string2json(contents));
		return content.resultMessage(object);
	}

	/**
	 * 根据内容组id显示文章
	 * 
	 * @param ogid
	 * @return
	 */
	public String ShowByGroupId(String wbid,String ogid) {
		return content.resultMessage(content.findByGroupID(wbid,ogid));
	}

	/*----------前台页面幻灯片显示-------------*/
	/**
	 * 根据内容组id显示文章
	 * 
	 * @param ogid
	 * @return
	 */
	public String ShowPicByGroupId(String wbid,String ogid) {
		return content.resultMessage(content.findPicByGroupID(wbid,ogid));
	}

	/**
	 * 修改排序值
	 * 
	 * @param oid
	 *            文章id
	 * @param sortNo
	 *            排序值
	 * @return 显示修改之前的数据
	 */
	public String sort(String oid, int sortNo) {
		return content.resultMessage(content.updatesort(oid, sortNo), "排序值修改成功");
	}

	/**
	 * 发布文章
	 * 
	 * @param ArticleInfo
	 * @return
	 */
	public String PublishArticle(String ArticleInfo) {
		JSONObject object = JSONHelper.string2json(ArticleInfo);
		if (object.containsKey("time")) {
			object.put("time", Long.parseLong(object.getString("time")));
		}
		object = content.AddMap(defmap, object);
		return content.insert(object);
	}

	public JSONObject getwbid(String wbid, JSONObject object) {
		if (object != null) {
			List<String> list = new ArrayList<>();
			if (object.containsKey("wbid")) {
				String wbsid = object.get("wbid").toString();
				String[] value = wbsid.split(",");
				for (int i = 0; i < value.length; i++) {
					list.add(value[i]);
				}
				list.add(wbid);
				wbid = StringHelper.join(list);
			}
			object.put("wbid", wbid);
		}
		return object;
	}

	/**
	 * 删除指定栏目下的文章
	 * 
	 * @param oid
	 *            文章id
	 * @param ogid
	 *            栏目id
	 * @return
	 */
	public String DeleteByOgid(String oid, String ogid) {
		int code = 99;
		if (UserInfo!=null|| UserInfo.size()!=0) {
			String wbid = UserInfo.get("currentWeb").toString();
			code = content.deleteByOgID(wbid,oid,ogid);
		}
		return content.resultMessage(code, "该栏目下文章删除成功");
	}

	/**
	 * 删除指定站点下的文章
	 * 
	 * @param oid
	 *            文章id
	 * @param wbid
	 *            站点id
	 * @return
	 */
	public String DeleteByWbID(String oid, String wbid) {
		return content.resultMessage(content.deleteByWbID(oid, wbid), "该站点下文章删除成功");
	}

	public String SetTempId(String oid, String tempid) {
		int code = 99;
		if (UserInfo!=null|| UserInfo.size()!=0) {
			String wbid = UserInfo.get("currentWeb").toString();
			code = content.setTempId(wbid,oid,tempid);
		}
		return content.resultMessage(code, "设置模版成功");
	}

	public String Setfatherid(String oid, String fatherid) {
		int code = 99;
		if (UserInfo!=null|| UserInfo.size()!=0) {
			String wbid = UserInfo.get("currentWeb").toString();
			code = content.setfatherid(wbid,oid,fatherid);
		}
		return content.resultMessage(code, "设置模版成功");
	}

	public String Setslevel(String oid, String slevel) {
		int code = 99;
		if (UserInfo!=null|| UserInfo.size()!=0) {
			String wbid = UserInfo.get("currentWeb").toString();
			code = content.setslevel(wbid,oid,slevel);
		}
		return content.resultMessage(code, "设置密级成功");
	}

	// 审核
	public String Review(String oid, String managerid, String state) {
		int code = 99;
		if (UserInfo!=null|| UserInfo.size()!=0) {
			String wbid = UserInfo.get("currentWeb").toString();
			code = content.review(wbid,oid, managerid, state);
		}
		return content.resultMessage(code, "审核文章操作成功");
	}

	public String BatchDelete(String oid) {
		return content.resultMessage(content.delete(oid.split(",")), "批量删除成功");
	}

	// 获取图片及简介
	public String getImgs(String ogid, int no) {
		return content.resultMessage(content.find(ogid, no));
	}

	// 获取当前文章所在栏目位置
	public String getPosition(String oid) {
		JSONObject object;
		String prevCol = "";
		if (oid.equals("") || oid.equals("0")) {
			nlogger.logout("oid = 0");
			return "";
		}
		try {
			// 获取文章所属的栏目id
			object = getOgid(oid);
			if (object != null) {
				String ogid = object.get("ogid").toString();
				prevCol = content.getPrev(ogid);
			}
		} catch (Exception e) {
			nlogger.logout(e);
			prevCol = "";
		}
		return prevCol;
	}

	// 获取下级栏目的文章
	public String getContent(String ogid, int no) {
		String ids = null;
		try {
			String tips = appsProxy
					.proxyCall(content.getPrev(ogid),
							String.valueOf(appsProxy.appid()) + "/15/ContentGroup/getColumnByFid/s:" + ogid, null, "")
					.toString();
			String message = JSONHelper.string2json(tips).get("message").toString();
			String records = JSONHelper.string2json(message).get("records").toString();
			JSONArray array = JSONHelper.string2array(records);
			ids = getID(array);
		} catch (Exception e) {
			nlogger.logout(e);
			ids = null;
		}
		// 批量查询
		return ids == null ? "" : content.resultMessage(content.find(ids.split(","), no));
	}

	// 获取文章id
	public String getID(JSONArray array) {
		List<String> list = null;
		if (array != null) {
			list = new ArrayList<>();
			JSONObject object;
			JSONObject objID;
			for (int i = 0, len = array.size(); i < len; i++) {
				object = (JSONObject) array.get(i);
				objID = (JSONObject) object.get("_id");
				list.add(objID.get("$oid").toString());
			}
		}
		return list == null ? "" : StringHelper.join(list);
	}

	// 根据条件进行统计
	public String getCount(String info) {
		return content.getCount(JSONHelper.string2json(info));
	}

	// 批量查询本周内已更新的文章数量
	public String getArticleCount(String info, String ogid) {
		return content.getArticleCount(info, ogid).toJSONString();
	}

	// 获取文章所属的栏目id
	private JSONObject getOgid(String oid) {
		return content.bind().eq("_id", new ObjectId(oid)).field("ogid").find();
	}
}
