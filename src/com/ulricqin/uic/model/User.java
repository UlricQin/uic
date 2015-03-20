package com.ulricqin.uic.model;

import java.util.LinkedList;
import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.ulricqin.frame.kit.StringKit;
import com.ulricqin.uic.plugin.MemcachePlugin;

public class User extends Model<User> {

	private static final long serialVersionUID = -6863257894302811903L;
	public static final User dao = new User();

	public boolean updateProfile(String cnname, String email, String phone,
			String im, String qq) {
		this.set("cnname", cnname);
		this.set("email", email);
		this.set("phone", phone);
		this.set("im", im);
		this.set("qq", qq);
		boolean success = this.update();
		if (success) {
			// cache: object
			MemcachePlugin.client.delete("u:" + this.getLong("id"));
		}
		return success;
	}

	public boolean updatePasswd(String newPassword) {
		this.set("passwd", newPassword);
		return this.update();
	}
	
	@Override
	public boolean delete() {
		boolean ret = super.delete();
		// cache: object
		MemcachePlugin.client.delete("u:" + this.getLong("id"));
		// cache name -> id
		MemcachePlugin.client.delete("u:id:" + this.getStr("name"));
		return ret;
	}

	public Page<User> paginate(int pageNo, int pageSize, String query,
			boolean iAmCreator, User creator) {

		StringBuilder where = new StringBuilder("from user where 1=1");
		List<Object> params = new LinkedList<Object>();

		if (iAmCreator) {
			where.append(" and creator = ?");
			params.add(creator.getLong("id"));
		}

		if (StringKit.isNotBlank(query)) {
			String tmp = "%" + query + "%";
			where.append(" and (name like ? or cnname like ?)");
			params.add(tmp);
			params.add(tmp);
		}

		where.append(" order by name");

		return paginate(pageNo, pageSize, "select *", where.toString(),
				params.toArray());
	}

	public List<User> query(String query, int limit) {
		if (StringKit.isBlank(query)) {
			return find("select id,name,cnname,email,phone,im,qq from user limit ?", limit);
		}

		return find(
				"select id,name,cnname,email,phone,im,qq from user where name like ? limit ?",
				"%" + query + "%", limit);
	}

	public boolean canWrite(User todoUser) {
		if (todoUser == null) {
			throw new IllegalArgumentException("todoUser == null");
		}

		// root可以删除或修改任何用户
		if (this.getStr("name").equals("root")) {
			return true;
		}

		// 谁都不能删除或修改root
		if (todoUser.getStr("name").equals("root")) {
			return false;
		}

		// 走到这说明，当前用户不是root，todoUser如果是admin，也不能由别的人操作
		// admin也不能操作admin
		if (todoUser.getInt("role") > 0) {
			return false;
		}

		if (!(this.getInt("role") > 0 || todoUser.getLong("creator").longValue() == this
				.getLong("id").longValue())) {
			return false;
		}

		return true;
	}

	public boolean canWrite(Team obj) {
		if (obj == null) {
			throw new IllegalArgumentException("target team is null");
		}

		// root可以删除或修改任何team
		if (this.getStr("name").equals("root")) {
			return true;
		}
		
		// 我创建的组、或者我在这个组里，则可以删除或修改这个组
		if (this.getLong("id").longValue() == obj.getLong("creator").longValue()) {
			return true;
		}
		
		List<Record> rs = Db.find("select id from rel_team_user where tid = ? and uid = ?", obj.getLong("id"), this.getLong("id"));
		if (rs != null && rs.size() > 0) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean update() {
		boolean success = super.update();
		deleteUserCache(getLong("id"));
		return success;
	}
	
	public static void deleteUserCache(Object id) {
		MemcachePlugin.client.delete("u:"+id);
	}

}
