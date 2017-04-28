package interfaceApplication;

import java.util.HashMap;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import esayhelper.JSONHelper;
import model.ContentGroupModel;
import model.ContentModel;

@SuppressWarnings("unchecked")
public class ContentGroup {
	private ContentGroupModel group = new ContentGroupModel();
	private HashMap<String, Object> defcol = new HashMap<>();
	private JSONObject _obj = new JSONObject();
	

	public ContentGroup() {
		defcol.put("ogid", ContentModel.getID());
		defcol.put("ownid", 0);
		defcol.put("fatherid", 0);
		defcol.put("sort", 0);
		defcol.put("isvisble", 0);
		defcol.put("slevel", 0);
		defcol.put("tempid", 0);
	}

	// 新增
	public String GroupInsert(String GroupInfo) {
		JSONObject ginfos = group.AddMap(defcol, JSONHelper.string2json(GroupInfo));
		_obj.put("records", JSONHelper.string2json(group.AddGroup(ginfos)));
		return group.resultMessage(0, _obj.toString());
	}

	// 编辑
	public String GroupEdit(String ogid, String groupInfo) {
		return group.resultMessage(group.UpdateGroup(ogid, JSONHelper.string2json(groupInfo)), "更新内容组数据成功");
	}

	// 删除
	public String GroupDelete(String ogid) {
		int code = 0;
		ContentModel model = new ContentModel();
		JSONArray array = model.findByGroupID(ogid);
		if (array.size()!=0) {
			// 根据内容组显示文章
				code = model.setGroup(array, ogid);
		}
		if (code == 0) {
			code = group.DeleteGroup(ogid);
		}
		return group.resultMessage(code, "删除内容组成功");
	}

	// public String GroupSelect() {
	// _obj.put("records", group.select());
	// return StringEscapeUtils.unescapeJava(group.resultMessage(0,
	// _obj.toString()));
	// }
	// 搜索
	public String GroupFind(String groupinfo) {
		_obj.put("records", group.select(groupinfo));
		return StringEscapeUtils.unescapeJava(group.resultMessage(0, _obj.toString()));
	}

	// 设置排序值
	public String GroupSort(String ogid, int num) {
		return group.resultMessage(group.setsort(ogid, num), "顺序调整成功");
	}

	// 分页
	public String GroupPage(int idx, int pageSize) {
		_obj.put("records", group.page(idx, pageSize));
		return group.resultMessage(0, _obj.toString());
	}

	// 条件分页
	public String GroupPageBy(int idx, int pageSize, String GroupInfo) {
		_obj.put("records", group.page(idx, pageSize, JSONHelper.string2json(GroupInfo)));
		return group.resultMessage(0, _obj.toString());
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
		return group.resultMessage(group.setfatherid(ogid, fatherid), "成功设置上级内容组");
	}

	// 批量删除
	public String GroupBatchDelete(String ogid) {
		return group.resultMessage(group.delete(ogid.split(",")), "删除成功");
	}

	// 获取下级栏目名称
	public String getColumnByFid(String ogid) {
		_obj.put("records", group.getColumnByFid(ogid));
		return group.resultMessage(0, _obj.toString());
	}
}
