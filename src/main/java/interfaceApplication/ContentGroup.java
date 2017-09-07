package interfaceApplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import apps.appsProxy;
import database.db;
import json.JSONHelper;
import model.ContentGroupModel;
import nlogger.nlogger;
import rpc.execRequest;
import session.session;
import string.StringHelper;

public class ContentGroup {
	private ContentGroupModel group = new ContentGroupModel();
	private HashMap<String, Object> defcol = new HashMap<>();
	private session session = new session();
	private JSONObject userInfo = null;

	public ContentGroup() {
		String sid = (String) execRequest.getChannelValue("sid");
		if (sid != null) {
			userInfo = new JSONObject();
			userInfo = session.getSession(sid);
		}
		String ownid = (userInfo != null && userInfo.size() != 0) ? ((JSONObject) userInfo.get("_id")).getString("$oid")
				: "";
		defcol.put("ownid", ownid); // 栏目管理员，默认为当前登录用户
		defcol.put("fatherid", "0");
		defcol.put("sort", 0);
		defcol.put("isvisble", 0);
		defcol.put("slevel", 0);
		defcol.put("type", 0);
		defcol.put("tempContent", "0");
		defcol.put("tempList", "0");
		defcol.put("contentType", "0"); // 该栏目下文章的类型
		defcol.put("u", 2000);
		defcol.put("r", 1000);
		defcol.put("d", 3000);
		defcol.put("wbid", (userInfo != null && userInfo.size() != 0) ? userInfo.get("currentWeb") : "");
		defcol.put("editor", ownid); // 栏目下文章编辑员,默认为当前登录用户
		defcol.put("timediff", 86400000); // 更新时间周期
		defcol.put("clickCount", 0); // 栏目更新周期内需更新的文章总数
		defcol.put("editCount", 1); // 栏目更新周期内需更新的文章总数
		defcol.put("connColumn", "0"); // 关联的栏目id，默认为0，不关联任何栏目
		defcol.put("isreview", 0); // 该栏目下文章是否需要审核，0：不需要审核，1：需要审核
	}

	// 新增
	public String GroupInsert(String GroupInfo) {
		JSONObject ginfos = group.AddMap(defcol, JSONHelper.string2json(GroupInfo));
		return group.AddGroup(ginfos);
	}

	// 编辑
	public String GroupEdit(String ogid, String groupInfo) {
		return group.resultMessage(group.UpdateGroup(ogid, JSONHelper.string2json(groupInfo)), "更新内容组数据成功");
	}

	// 删除
	public String GroupDelete(String ogid) {
		// 获取该栏目下的所有子栏目
		long code = -1;
		int codes = -1;
		List<String> list = new ArrayList<>();
		JSONArray arrays = group.getColumn(ogid);
		if (arrays != null) {
			try {
				while (arrays.size() > 0) {
					List<String> temp = new ArrayList<>();
					temp = group.getList(arrays);
					arrays = group.getColumn(StringHelper.join(list));
					list.addAll(temp);
				}
				list.add(ogid);
				String tips = appsProxy
						.proxyCall("/GrapeContent/Content/SetGroupBatch/s:" + StringHelper.join(list), null, "")
						.toString();
				if (tips != null && !tips.equals("")) {
					code = (long) JSONHelper.string2json(tips).get("errorcode");
					codes = Integer.parseInt(String.valueOf(code));
					if (codes == 0) {
						if (list.size() > 1) {
							codes = group.delete(StringHelper.join(list).split(","));
						} else {
							codes = group.DeleteGroup(StringHelper.join(list));
						}
					}
				}
			} catch (Exception e) {
				codes = -1;
				nlogger.logout(e);
			}
		}
		return codes == -1 ? "" : group.resultMessage(codes, "栏目删除成功");
	}

	public String FindByType(String type, int no) {
		String wbid = (userInfo != null && userInfo.size() != 0) ? userInfo.getString("currentWeb") : "";
		return group.findByType(wbid, type, no);
	}

	public String FindByTypes(String type, int no) {
		String wbid = (userInfo != null && userInfo.size() != 0) ? userInfo.getString("currentWeb") : "";
		return group.findByTypes(wbid, type, no);
	}

	/**
	 * 查询支持超链接文章的栏目，即contentType为5
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file ContentGroup.java
	 * 
	 * @param type
	 * @param no
	 * @return
	 *
	 */
	public String findByContentType(int idx, int pageSize) {
		db db = group.getdb();
		JSONArray array = db.eq("contentType", "5").field("_id,name").page(idx, pageSize);
		return array.toString();
	}

	public String GroupFind(String groupinfo) {
		return group.resultMessage(group.select(groupinfo));
	}

	// 设置排序值
	public String GroupSort(String ogid, int num) {
		return group.resultMessage(group.setsort(ogid, num), "顺序调整成功");
	}

	/*-------------------前台---------------------*/
	// 分页
	public String GroupPage(String wbid, int idx, int pageSize) {
		if (userInfo != null && userInfo.size() != 0) {
			wbid = userInfo.get("currentWeb").toString();
		}
		return group.pages(wbid, idx, pageSize, null);
	}

	// 条件分页
	public String GroupPageBy(String wbid, int idx, int pageSize, String GroupInfo) {
		if (userInfo != null && userInfo.size() != 0) {
			wbid = userInfo.get("currentWeb").toString();
		}
		return group.pages(wbid, idx, pageSize, GroupInfo);
	}

	/*-------------------后台------*/
	// 分页
	public String GroupPageBack(int idx, int pageSize) {
		String result = group.resultMessage(new JSONArray());
		if (userInfo != null && userInfo.size() != 0) {
			String wbid = userInfo.get("currentWeb").toString();
			result = group.pages(wbid, idx, pageSize, null);
		}
		return result;
	}

	// 条件分页
	public String GroupPageByBack(int idx, int pageSize, String GroupInfo) {
		String result = group.resultMessage(new JSONArray());
		if (userInfo != null && userInfo.size() != 0) {
			String wbid = userInfo.get("currentWeb").toString();
			result = group.pages(wbid, idx, pageSize, GroupInfo);
		}
		return result;
	}

	// 设置密级
	public String GroupSlevel(String ogid, int slevel) {
		return group.resultMessage(group.setslevel(ogid, slevel), "密级更新成功");
	}

	// 设置模版
	public String GroupSetTemp(String ogid, String tempid) {
		return group.resultMessage(group.setTempId(ogid, tempid), "更新模版成功");
	}

	// 设置上级栏目
	public String GroupSetFatherid(String ogid, int fatherid) {
		return group.resultMessage(group.setfatherid(ogid, fatherid), "成功设置上级栏目");
	}

	// 批量删除
	public String GroupBatchDelete(String ogid) {
		return group.resultMessage(group.delete(ogid.split(",")), "删除成功");
	}

	// 获取下级栏目名称
	public String getColumnByFid(String ogid) {
		return group.getColumnByFid(ogid);
	}

	// 获取当前文章所在栏目位置
	public String getPrevCol(String ogid) {
		JSONArray array = group.getPrev(ogid);
		return group.resultMessage(array);
	}

	// 设置栏目管理员 userid为用户表 _id
	public String setManage(String ogid, String userid) {
		return group.setColumManage(ogid, userid);
	}

	// 获取某网站下的栏目
	public String getPrevColumn(String wbid) {
		JSONArray array = group.getColumnByWbid(wbid);
		return array != null ? array.toString() : "";
	}

	public String getGroupById(String ogid) {
		JSONObject object = group.find(ogid);
		return group.resultMessage(object);
	}

	public String getGroupByIds(String ogid) {
		JSONArray array = group.finds(ogid);
		return array != null ? array.toString() : "";
	}

	// 获取该栏目下，栏目管理员
	public String getManagerByOgid(String ogid) {
		JSONObject object = group.findOwnid(ogid);
		return object != null ? object.toString() : "";
	}

	/**
	 * 获取栏目
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file ContentGroup.java
	 * 
	 * @param condString
	 * @return
	 *
	 */
	public String getColumns(int idx, int pageSize, String wbid) {
		long totalSize = 0;
		JSONArray array = null;
		db db = group.getdb();
		if (wbid != null && !wbid.equals("")) {
			// 查询条件含有wbid，判断wbid是否为本站点或者本站点的下级站点
			if (IsWeb(wbid)) {
				db.eq("wbid", wbid);
				array = db.dirty().page(idx, pageSize);
				totalSize = db.pageMax(pageSize);
				db.clear();
			}
		}
		return group.pageShow(array, idx, pageSize, totalSize);
	}

	/**
	 * 查询条件若含有wbid，判断wbid是否为本站点或者本站点的下级站点
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file ContentGroup.java
	 * 
	 * @param array
	 * @return
	 *
	 */
	private boolean IsWeb(String wbid) {
		String currentWeb = (userInfo != null && userInfo.size() != 0) ? userInfo.getString("currentWeb") : "";
		String web = "";
		if (!currentWeb.equals("")) {
			// 获取当前站点及当前站点的下级站点
			web = appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebTree/" + currentWeb, null, null).toString();
			// web = appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebTree/" +
			// currentWeb).toString();
		}
		return web.contains(wbid);
	}

	/**
	 * 根据栏目id获取该栏目的点击次数
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file ContentGroup.java
	 * 
	 * @param ogid
	 * @return
	 *
	 */
	@SuppressWarnings("unchecked")
	public String getClickCount(String ogid) {
		String[] value = ogid.split(",");
		db db = group.getdb().or();
		for (String tempid : value) {
			if (!tempid.equals("")) {
				db.eq("_id", new ObjectId(tempid));
			}
		}
		JSONArray array = db.field("_id,clickcount").select();
		JSONObject object;
		JSONObject objId;
		JSONObject rObject = new JSONObject();
		String clickCount;
		String id;
		if (array != null && array.size() != 0) {
			for (Object object2 : array) {
				object = (JSONObject) object2;
				objId = (JSONObject) object.get("_id");
				id = objId.getString("$oid");
				clickCount = String.valueOf(object.get("clickcount"));
				if (clickCount.contains("$numberLong")) {
					objId = JSONObject.toJSON(clickCount);
					clickCount = (objId != null && objId.size() != 0) ? objId.getString("$numberLong") : "0";
				}
				rObject.put(id, Long.parseLong(clickCount));
			}
		}
		return rObject.toJSONString();
	}

	/**
	 * 批量修改，不同的id，修改不同的数据
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file ContentGroup.java
	 * 
	 * @param ogid
	 * @param info
	 * @return
	 *
	 */
	public String GroupEdits(String ogid, String info) {
		int code = 0;
		String message = group.resultMessage(99);
		JSONObject tempObj = JSONObject.toJSON(info);
		String dataInfo;
		String[] value = ogid.split(",");
		String id;
		int l = value.length;
		db db = group.getdb();
		if (tempObj != null && tempObj.size() != 0) {
			for (int i = 0; i < l; i++) {
				id = value[i];
				dataInfo = String.valueOf(tempObj.get(id));
				dataInfo = "{\"clickcount\":" + Long.parseLong(dataInfo) + "}";
				if (code == 0) {
					code = db.eq("_id", new ObjectId(id)).data(dataInfo).update() != null ? 0 : 99;
				}
			}
			message = group.resultMessage(code, "修改成功");
		}
		return message;
	}

	/**
	 * 获取关联栏目id
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file ContentGroup.java
	 * 
	 * @param ogid
	 *            当前栏目id
	 * @return 当前栏目id和关联栏目id，格式为ogid,ogid,ogid
	 *
	 */
	public String getConnColumns(String ogid) {
		String column = "", columnId = "", id;
		JSONArray array = null;
		JSONObject object = group.find(ogid);
		if (object != null && object.size() != 0) {
			if (object.containsKey("connColumn")) {
				column = object.getString("connColumn");
				if (!column.equals("0")) {
					array = JSONArray.toJSONArray(column);
					for (Object obj : array) {
						object = (JSONObject) obj;
						id = (object != null && object.size() != 0) ? object.getString("id") : "";
						columnId += (!id.equals("") ? id : "") + ",";
					}
				}
			}
		}
		if (!columnId.equals("")) {
			columnId = StringHelper.fixString(columnId, ',');
			ogid = ogid + "," + columnId;
		}
		return ogid;
	}

	public String getColumnInfo(String wbid, String name) {
		JSONArray array = null;
		String[] value = null;
		db db = group.getdb();
		if (wbid != null && !wbid.equals("")) {
			value = wbid.split(",");
			db.or();
			for (String string : value) {
				db.eq("wbid", string);
			}
			db.and();
			db.eq("name", name);
			array = db.select();
		}
		JSONObject rsObject = JoinObj(value, array);
		return (rsObject != null && rsObject.size() != 0) ? rsObject.toString() : null ;
	}

	@SuppressWarnings("unchecked")
	private JSONObject JoinObj(String[] value, JSONArray array) {
		JSONObject rsObject = null, object;
		String id, wbid;
		if (array != null && array.size() != 0) {
			rsObject = new JSONObject();
			int l = array.size();
			for (int i = 0; i < l; i++) {
				object = (JSONObject) array.get(i);
				id = ((JSONObject) object.get("_id")).getString("$oid");
				wbid = object.getString("wbid");
				for (String string : value) {
					if (wbid.equals(string)) {
						rsObject.put(string, id);
					}
				}
			}
		}
		return rsObject;

	}
}
