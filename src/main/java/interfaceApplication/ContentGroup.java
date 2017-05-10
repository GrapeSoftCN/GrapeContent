package interfaceApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import apps.appsProxy;
import esayhelper.JSONHelper;
import esayhelper.StringHelper;
import model.ContentGroupModel;

public class ContentGroup {
	private ContentGroupModel group = new ContentGroupModel();
	private HashMap<String, Object> defcol = new HashMap<>();
	// private String userId;

	public ContentGroup() {
		// userId = execRequest.getChannelValue("Userid").toString();

		defcol.put("ownid", 0);
		defcol.put("fatherid", 0);
		defcol.put("sort", 0);
		defcol.put("isvisble", 0);
		defcol.put("slevel", 0);
		defcol.put("tempContent", 0);
		defcol.put("tempList", 0);
		defcol.put("uplv", 2000);
		defcol.put("rplv", 1000);
		defcol.put("dplv", 3000);
	}

	// 新增
	public String GroupInsert(String GroupInfo) {
		// 该用户是否拥有新增权限
		// String tip = execRequest
		// ._run("GrapeAuth/Auth/InsertPLV/s:" + userId, null).toString();
		// if (!"0".equals(tip)) {
		// return group.resultMessage(4, "没有新增权限");
		// }
		JSONObject ginfos = group.AddMap(defcol,
				JSONHelper.string2json(GroupInfo));
		return group.AddGroup(ginfos);
	}

	// 编辑
	public String GroupEdit(String ogid, String groupInfo) {
		// String uPLV = group.find(ogid).get("uplv").toString();
		// String tip = execRequest._run("GrapeAuth/Auth/UpdatePLV/s:" + uPLV
		// + "/s:" + userId, null).toString();
		// if (!"0".equals(tip)) {
		// return group.resultMessage(5, "没有编辑权限");
		// }
		return group.resultMessage(
				group.UpdateGroup(ogid, JSONHelper.string2json(groupInfo)),
				"更新内容组数据成功");
	}

	// 删除
	public String GroupDelete(String ogid) {
		// // 获取该条数据的删除，修改，查询权限
		// String dPLV = group.find(ogid).get("dplv").toString();
		// String tip = execRequest._run("GrapeAuth/Auth/DeletePLV/s:" + dPLV
		// + "/s:" + userId, null).toString();
		// if (!"0".equals(tip)) {
		// return group.resultMessage(6, "没有删除权限");
		// }
		// 获取该栏目下的所有子栏目
		List<String> list = new ArrayList<>();
		JSONArray arrays = group.getColumn(ogid);
		while (arrays.size() != 0) {
			List<String> temp = new ArrayList<>();
			temp = group.getList(arrays);
			arrays = group.getColumn(StringHelper.join(list));
			list.addAll(temp);
		}
		list.add(ogid);
		// 设置该栏目及所有子栏目下的文章的栏目设为默认值
		// String tips =
		// execRequest._run("GrapeContent/content/SetGroupBatch/s:"
		// + StringHelper.join(list), null).toString();
		String tips = appsProxy.proxyCall("123.57.214.226:801",
				"15/content/SetGroupBatch/s:" + StringHelper.join(list), null,
				"").toString();
		long code = (long) JSONHelper.string2json(tips).get("errorcode");
		int codes = Integer.parseInt(String.valueOf(code));
		if (codes == 0) {
			if (list.size() > 1) {
				codes = group.delete(StringHelper.join(list).split(","));
			} else {
				codes = group.DeleteGroup(StringHelper.join(list));
			}
		}
		return group.resultMessage(codes, "栏目删除成功");
	}

	public String FindByType(String type, int no) {
		return group.findByType(type, no);
	}

	public String GroupFind(String groupinfo) {
		return group.resultMessage(0, group.select(groupinfo).toString());
	}

	// 设置排序值
	public String GroupSort(String ogid, int num) {
		// String uPLV = group.find(ogid).get("uplv").toString();
		// String tip = execRequest._run("GrapeAuth/Auth/UpdatePLV/s:" + uPLV
		// + "/s:" + userId, null).toString();
		// if (!"0".equals(tip)) {
		// return group.resultMessage(5, "没有编辑权限");
		// }
		return group.resultMessage(group.setsort(ogid, num), "顺序调整成功");
	}

	// 分页
	public String GroupPage(int idx, int pageSize) {
		return group.page(idx, pageSize);
	}

	// 条件分页
	public String GroupPageBy(int idx, int pageSize, String GroupInfo) {
		return group.page(idx, pageSize, JSONHelper.string2json(GroupInfo));
	}

	// 设置密级
	public String GroupSlevel(String ogid, int slevel) {
		// String uPLV = group.find(ogid).get("uplv").toString();
		// String tip = execRequest._run("GrapeAuth/Auth/UpdatePLV/s:" + uPLV
		// + "/s:" + userId, null).toString();
		// if (!"0".equals(tip)) {
		// return group.resultMessage(5, "没有编辑权限");
		// }
		return group.resultMessage(group.setslevel(ogid, slevel), "密级更新成功");
	}

	// 设置模版
	public String GroupSetTemp(String ogid, String tempid) {
		// String uPLV = group.find(ogid).get("uplv").toString();
		// String tip = execRequest._run("GrapeAuth/Auth/UpdatePLV/s:" + uPLV
		// + "/s:" + userId, null).toString();
		// if (!"0".equals(tip)) {
		// return group.resultMessage(5, "没有编辑权限");
		// }
		return group.resultMessage(group.setTempId(ogid, tempid), "更新模版成功");
	}

	// 设置上级栏目
	public String GroupSetFatherid(String ogid, int fatherid) {
		// String uPLV = group.find(ogid).get("uplv").toString();
		// String tip = execRequest._run("GrapeAuth/Auth/UpdatePLV/s:" + uPLV
		// + "/s:" + userId, null).toString();
		// if (!"0".equals(tip)) {
		// return group.resultMessage(5, "没有编辑权限");
		// }
		return group.resultMessage(group.setfatherid(ogid, fatherid),
				"成功设置上级栏目");
	}

	// 批量删除
	public String GroupBatchDelete(String ogid) {
		return group.resultMessage(group.delete(ogid.split(",")), "删除成功");
	}

	// 获取下级栏目名称
	public String getColumnByFid(String ogid) {
		return group.getColumnByFid(ogid);
	}

	// 获得上级栏目id，name，fatherid
	public String getPrevCol(String ogid) {
		return group.find(ogid).toString();
	}

	// 获取当前文章所在栏目位置
	@SuppressWarnings("unchecked")
	public String getPreColumn(String ogid) {
		JSONObject object = group.find(ogid);
		JSONObject obj = new JSONObject();
		List<JSONObject> list = new ArrayList<>();
		String fatherid = object.get("fatherid").toString();
		JSONObject objID = (JSONObject) object.get("_id");
		obj.put("_id", objID.get("$oid").toString());
		obj.put("name", object.get("name").toString());
		list.add(obj);
		while (!"0".equals(fatherid)) {
			String prevCol = getPrevCol(fatherid);
			fatherid = JSONHelper.string2json(prevCol).get("fatherid")
					.toString();
			list = group.getName(list, JSONHelper.string2json(prevCol));

		}
		Collections.reverse(list); // list倒序排列
		return group.resultMessage(0,
				JSONHelper.string2array(list.toString()).toString());
	}
}
